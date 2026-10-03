package com.gifttogether.contribution;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.repository.PaymentRepository;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contribution-cancel-api-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class ContributionCancelApiTest {

    @Autowired
    MockMvc mockMvc;

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
    void 다른_펀딩의_참여를_취소하려고_하면_404를_반환한다()
            throws Exception {

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

        // URL에 사용할 펀딩
        Funding funding1 = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "펀딩 1"
                )
        );

        // 실제 contribution이 속한 펀딩
        Funding funding2 = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "펀딩 2"
                )
        );

        Contribution contribution = new Contribution(
                funding2,
                contributor,
                10_000L,
                false
        );

        contribution.complete();
        contributionRepository.save(contribution);

        Payment payment = new Payment(
                contribution,
                10_000L
        );

        payment.success();
        paymentRepository.save(payment);

        mockMvc.perform(
                        post(
                                "/api/fundings/{fundingId}/contributions/{contributionId}/cancel",
                                funding1.getId(),
                                contribution.getId()
                        )
                                .header(
                                        "X-USER-ID",
                                        contributor.getId()
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CONTRIBUTION_NOT_FOUND"));
    }
}