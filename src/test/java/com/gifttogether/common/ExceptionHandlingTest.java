package com.gifttogether.common;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:exception-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class ExceptionHandlingTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    FundingRepository fundingRepository;

    @Test
    void 존재하지_않는_펀딩은_404를_반환한다() throws Exception {

        mockMvc.perform(
                        get("/api/fundings/999999")
                                .header("X-USER-ID", 1L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("FUNDING_NOT_FOUND"));
    }

    @Test
    void 최소금액보다_적게_참여하면_400을_반환한다() throws Exception {

        User receiver = userRepository.save(
                new User("receiver400", "receiver400@test.com")
        );

        User contributor = userRepository.save(
                new User("contributor400", "contributor400@test.com")
        );

        Product product = productRepository.save(
                new Product(
                        "400 테스트 상품",
                        150_000L,
                        "https://example.com/400.jpg"
                )
        );

        Funding funding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "400 테스트"
                )
        );

        mockMvc.perform(
                        post("/api/fundings/" + funding.getId() + "/contributions")
                                .header("X-USER-ID", contributor.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "amount": 999,
                                          "anonymous": false,
                                          "simulatePaymentFailure": false
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 다른_사용자가_펀딩을_취소하면_403을_반환한다() throws Exception {

        User receiver = userRepository.save(
                new User("receiver403", "receiver403@test.com")
        );

        User otherUser = userRepository.save(
                new User("other403", "other403@test.com")
        );

        Product product = productRepository.save(
                new Product(
                        "403 테스트 상품",
                        150_000L,
                        "https://example.com/403.jpg"
                )
        );

        Funding funding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusDays(1),
                        "403 테스트"
                )
        );

        mockMvc.perform(
                        post("/api/fundings/" + funding.getId() + "/cancel")
                                .header("X-USER-ID", otherUser.getId())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("FORBIDDEN"));
    }
}