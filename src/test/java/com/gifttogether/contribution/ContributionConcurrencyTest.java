package com.gifttogether.contribution;

import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.service.ContributionService;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrency-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class ContributionConcurrencyTest {

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
        User receiver = userRepository.save(
                new User("receiver", "receiver@test.com")
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
    void 동시에_100명이_만원씩_참여한다() throws InterruptedException {

        int threadCount = 100;

        ExecutorService executorService =
                Executors.newFixedThreadPool(32);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

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

                } catch (Exception e) {
                    System.out.println("참여 실패: " + e.getClass().getSimpleName()
                            + " / " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();

        Funding funding =
                fundingRepository.findById(fundingId).orElseThrow();

        System.out.println(
                "최종 모금 금액 = " + funding.getCurrentAmount()
        );

        assertThat(funding.getCurrentAmount())
                .isEqualTo(100_000L);
    }
}