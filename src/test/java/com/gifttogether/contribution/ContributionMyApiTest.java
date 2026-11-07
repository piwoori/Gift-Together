
package com.gifttogether.contribution;

import com.gifttogether.auth.jwt.JwtTokenProvider;
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
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contribution-my-api-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class ContributionMyApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired FundingRepository fundingRepository;
    @Autowired ContributionRepository contributionRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 인증하지_않은_사용자는_내_참여_내역을_조회할_수_없다()
            throws Exception {

        mockMvc.perform(
                get("/api/fundings/1/contributions/my")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void 인증된_사용자는_본인의_익명_참여만_조회한다()
            throws Exception {

        User receiver = userRepository.save(
                new User("receiver-my", "receiver-my@test.com")
        );

        User userA = userRepository.save(
                new User("userA-my", "userA-my@test.com")
        );

        User userB = userRepository.save(
                new User("userB-my", "userB-my@test.com")
        );

        Product product = productRepository.save(
                new Product(
                        "내 참여 조회 테스트 상품",
                        100_000L,
                        "https://example.com/product.jpg"
                )
        );

        Funding funding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "내 참여 조회 API 테스트"
                )
        );

        Contribution myContribution = contributionRepository.save(
                new Contribution(
                        funding,
                        userA,
                        10_000L,
                        true
                )
        );

        contributionRepository.save(
                new Contribution(
                        funding,
                        userB,
                        20_000L,
                        false
                )
        );

        mockMvc.perform(
                        get(
                                "/api/fundings/{fundingId}/contributions/my",
                                funding.getId()
                        ).header(
                                "Authorization",
                                "Bearer " + jwtTokenProvider.createAccessToken(userA.getId())
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contributionId")
                        .value(myContribution.getId()))
                .andExpect(jsonPath("$[0].contributorNickname")
                        .value("익명"))
                .andExpect(jsonPath("$[0].amount")
                        .value(10000))
                .andExpect(jsonPath("$[0].anonymous")
                        .value(true));
    }
}
