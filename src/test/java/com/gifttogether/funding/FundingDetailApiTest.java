package com.gifttogether.funding;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.repository.FundingRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:funding-detail-api-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class FundingDetailApiTest {

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

    @Test
    void 펀딩_상세_조회시_완료된_참여만_참여자_수에_포함한다()
            throws Exception {

        User receiver = userRepository.save(
                new User(
                        "receiver",
                        "receiver@test.com"
                )
        );

        User contributor1 = userRepository.save(
                new User(
                        "contributor1",
                        "contributor1@test.com"
                )
        );

        User contributor2 = userRepository.save(
                new User(
                        "contributor2",
                        "contributor2@test.com"
                )
        );

        User contributor3 = userRepository.save(
                new User(
                        "contributor3",
                        "contributor3@test.com"
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
                        "같이 선물해주세요!"
                )
        );

        Contribution completed1 = new Contribution(
                funding,
                contributor1,
                10_000L,
                false
        );
        completed1.complete();

        Contribution completed2 = new Contribution(
                funding,
                contributor2,
                20_000L,
                false
        );
        completed2.complete();

        Contribution cancelled = new Contribution(
                funding,
                contributor3,
                10_000L,
                false
        );

        /*
         * Contribution.cancel()은 COMPLETED 상태에서만 가능하므로
         * 먼저 완료시킨 뒤 취소한다.
         */
        cancelled.complete();
        cancelled.cancel();

        contributionRepository.save(completed1);
        contributionRepository.save(completed2);
        contributionRepository.save(cancelled);

        mockMvc.perform(
                        get("/api/fundings/{fundingId}", funding.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingId")
                        .value(funding.getId()))
                .andExpect(jsonPath("$.participantCount")
                        .value(2));
    }
}