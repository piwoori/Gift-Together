package com.gifttogether.contribution;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;
import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.contribution.service.ContributionService;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.domain.PaymentStatus;
import com.gifttogether.payment.repository.PaymentRepository;
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
        "spring.datasource.url=jdbc:h2:mem:payment-failure-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
class ContributionPaymentFailureTest {

    @Autowired
    ContributionService contributionService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    FundingRepository fundingRepository;

    @Autowired
    ContributionRepository contributionRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Test
    void 결제에_실패하면_참여도_실패하고_펀딩_금액은_증가하지_않는다() {

        User receiver = userRepository.save(
                new User(
                        "receiver",
                        "receiver@test.com"
                )
        );

        User contributor = userRepository.save(
                new User(
                        "contributor",
                        "contributor@test.com"
                )
        );

        Product product = productRepository.save(
                new Product(
                        "테스트 상품",
                        100_000L,
                        "https://example.com/product.jpg"
                )
        );

        Funding funding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "테스트 펀딩"
                )
        );

        ContributionCreateRequest request =
                new ContributionCreateRequest(
                        10_000L,
                        false,
                        true
                );

        ContributionCreateResponse response =
                contributionService.contribute(
                        funding.getId(),
                        contributor.getId(),
                        request
                );

        Contribution contribution = contributionRepository
                .findById(response.contributionId())
                .orElseThrow();

        Payment payment = paymentRepository
                .findByContributionId(contribution.getId())
                .orElseThrow();

        Funding updatedFunding = fundingRepository
                .findById(funding.getId())
                .orElseThrow();

        // API 응답
        assertThat(response.contributionStatus())
                .isEqualTo(ContributionStatus.FAILED);

        assertThat(response.currentAmount())
                .isZero();

        assertThat(response.fundingStatus())
                .isEqualTo(FundingStatus.OPEN);

        // 실제 DB 상태
        assertThat(contribution.getStatus())
                .isEqualTo(ContributionStatus.FAILED);

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        assertThat(updatedFunding.getCurrentAmount())
                .isZero();

        assertThat(updatedFunding.getStatus())
                .isEqualTo(FundingStatus.OPEN);
    }
}