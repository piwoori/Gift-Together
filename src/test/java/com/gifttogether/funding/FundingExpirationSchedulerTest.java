package com.gifttogether.funding;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.funding.scheduler.FundingExpirationScheduler;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scheduler-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class FundingExpirationSchedulerTest {

    @Autowired
    FundingExpirationScheduler scheduler;

    @Autowired
    FundingRepository fundingRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void 마감시간이_지난_OPEN_펀딩만_자동으로_만료한다() {

        // given
        User receiver = userRepository.save(
                new User("scheduler-user", "scheduler@test.com")
        );

        Product product = productRepository.save(
                new Product(
                        "스케줄러 테스트 상품",
                        150_000L,
                        "https://example.com/scheduler.jpg"
                )
        );

        Funding expiredFunding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().minusMinutes(10),
                        "이미 마감된 펀딩"
                )
        );

        Funding activeFunding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "아직 진행 중인 펀딩"
                )
        );

        // when
        scheduler.expireFundings();

        // then
        Funding expiredResult = fundingRepository
                .findById(expiredFunding.getId())
                .orElseThrow();

        Funding activeResult = fundingRepository
                .findById(activeFunding.getId())
                .orElseThrow();

        assertThat(expiredResult.getStatus())
                .isEqualTo(FundingStatus.EXPIRED);

        assertThat(activeResult.getStatus())
                .isEqualTo(FundingStatus.OPEN);
    }
}