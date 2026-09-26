package com.gifttogether.contribution;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.dto.ContributionResponse;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.contribution.service.ContributionService;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contribution-query-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
class ContributionQueryTest {

    @Autowired
    ContributionService contributionService;

    @Autowired
    ContributionRepository contributionRepository;

    @Autowired
    FundingRepository fundingRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void 익명_참여자는_조회할_때_사용자_정보를_노출하지_않는다() {

        User receiver = userRepository.save(
                new User(
                        "receiver",
                        "receiver@test.com"
                )
        );

        User contributor = userRepository.save(
                new User(
                        "secret-user",
                        "secret@test.com"
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
                        "익명 테스트"
                )
        );

        contributionRepository.save(
                new Contribution(
                        funding,
                        contributor,
                        10_000L,
                        true
                )
        );

        List<ContributionResponse> responses =
                contributionService.getContributions(
                        funding.getId()
                );

        assertThat(responses).hasSize(1);

        ContributionResponse response = responses.get(0);

        assertThat(response.contributorId()).isNull();
        assertThat(response.contributorNickname()).isEqualTo("익명");
        assertThat(response.amount()).isEqualTo(10_000L);
        assertThat(response.anonymous()).isTrue();
    }
}