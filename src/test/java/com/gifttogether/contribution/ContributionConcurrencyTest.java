package com.gifttogether.contribution;

import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.service.ContributionService;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
class ContributionConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("gift_together_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create"
        );
    }

    @Autowired
    ContributionService contributionService;

    @Autowired
    FundingRepository fundingRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    private Long fundingId;

    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {

        userIds.clear();

        User receiver = userRepository.save(
                new User(
                        "receiver",
                        "receiver@test.com"
                )
        );

        Product product = productRepository.save(
                new Product(
                        "테스트 상품",
                        100_000L,
                        "https://example.com/test.jpg"
                )
        );

        Funding funding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "동시성 테스트"
                )
        );

        fundingId = funding.getId();

        for (int i = 0; i < 100; i++) {

            User contributor = userRepository.save(
                    new User(
                            "user" + i,
                            "user" + i + "@test.com"
                    )
            );

            userIds.add(contributor.getId());
        }
    }

    @Test
    void 동시에_100명이_만원씩_참여해도_정확히_10명만_성공한다()
            throws InterruptedException {

        int threadCount = 100;

        ExecutorService executorService =
                Executors.newFixedThreadPool(32);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch doneLatch =
                new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {

            final Long userId = userIds.get(i);

            executorService.submit(() -> {
                try {
                    startLatch.await();

                    contributionService.contribute(
                            fundingId,
                            userId,
                            new ContributionCreateRequest(
                                    10_000L,
                                    false,
                                    false
                            )
                    );

                    successCount.incrementAndGet();

                } catch (Exception e) {

                    failureCount.incrementAndGet();

                    System.out.println(
                            "참여 실패: "
                                    + e.getClass().getSimpleName()
                                    + " / "
                                    + e.getMessage()
                    );

                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        Funding funding =
                fundingRepository
                        .findById(fundingId)
                        .orElseThrow();

        System.out.println(
                "성공 횟수 = " + successCount.get()
        );

        System.out.println(
                "실패 횟수 = " + failureCount.get()
        );

        System.out.println(
                "최종 모금 금액 = " + funding.getCurrentAmount()
        );

        System.out.println(
                "최종 펀딩 상태 = " + funding.getStatus()
        );

        assertThat(successCount.get())
                .isEqualTo(10);

        assertThat(failureCount.get())
                .isEqualTo(90);

        assertThat(funding.getCurrentAmount())
                .isEqualTo(100_000L);

        assertThat(funding.getStatus())
                .isEqualTo(FundingStatus.COMPLETED);
    }
}