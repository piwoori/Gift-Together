package com.gifttogether.funding;

import com.gifttogether.auth.jwt.JwtTokenProvider;
import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.funding.scheduler.FundingExpirationScheduler;
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.domain.PaymentStatus;
import com.gifttogether.payment.repository.PaymentRepository;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wishlist.domain.WishlistItem;
import com.gifttogether.wishlist.repository.WishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:funding-deadline-api",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
class FundingDeadlineApiTest {

    private static final Instant DEADLINE_UTC = Instant.parse("2026-12-01T09:00:00Z");
    private static final LocalDateTime DEADLINE_SEOUL = LocalDateTime.parse("2026-12-01T18:00:00");

    @MockitoBean
    Clock clock;

    // Keep the fixture OPEN even after its deadline, as it would be before the next scheduler run.
    @MockitoBean
    FundingExpirationScheduler scheduler;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtTokenProvider jwtTokenProvider;
    @Autowired
    UserRepository users;
    @Autowired
    ProductRepository products;
    @Autowired
    WishlistItemRepository wishlist;
    @Autowired
    FundingRepository fundings;
    @Autowired
    ContributionRepository contributions;
    @Autowired
    PaymentRepository payments;

    private User receiver;
    private User contributor;
    private Product product;
    private Funding funding;
    private Contribution contribution;

    @BeforeEach
    void setUp() {
        receiver = users.save(new User("receiver", UUID.randomUUID() + "@example.test"));
        contributor = users.save(new User("contributor", UUID.randomUUID() + "@example.test"));
        product = products.save(new Product("테스트 상품", 100_000L, null));
        funding = new Funding(receiver, product, DEADLINE_SEOUL, "마감 테스트");
        funding.contribute(1_000L, DEADLINE_SEOUL.minusSeconds(1));
        funding = fundings.save(funding);
        contribution = new Contribution(funding, contributor, 1_000L, false);
        contribution.complete();
        contribution = contributions.save(contribution);
        Payment payment = new Payment(contribution, 1_000L);
        payment.success();
        payments.save(payment);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void validatesCreationUsingSeoulTime(long secondsFromDeadline) throws Exception {
        setCurrentTime(secondsFromDeadline);
        wishlist.save(new WishlistItem(receiver, product));
        ResultActions result = perform(post("/api/fundings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productId":%d,"expiredAt":"2026-12-01T18:00:00"}
                        """.formatted(product.getId())), receiver);

        if (secondsFromDeadline < 0) {
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.expiredAt").value("2026-12-01T18:00:00"));
        } else {
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void validatesParticipationBeforeSchedulerRuns(long secondsFromDeadline) throws Exception {
        setCurrentTime(secondsFromDeadline);
        ResultActions result = perform(post("/api/fundings/{id}/contributions", funding.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":2000}"), contributor);

        if (secondsFromDeadline < 0) {
            result.andExpect(status().isCreated());
        } else {
            expectDeadlineConflict(result);
        }

        assertThat(loadFunding().getCurrentAmount())
                .isEqualTo(secondsFromDeadline < 0 ? 3_000L : 1_000L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    void rejectedMockFailureDoesNotRecordParticipation(long secondsFromDeadline) throws Exception {
        setCurrentTime(secondsFromDeadline);
        expectDeadlineConflict(perform(post("/api/fundings/{id}/contributions", funding.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":2000,\"simulatePaymentFailure\":true}"), contributor));
        assertThat(contributions.findAllByFundingId(funding.getId())).hasSize(1);
        assertThat(loadFunding().getCurrentAmount()).isEqualTo(1_000L);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void validatesOwnCancellationBeforeSchedulerRuns(long secondsFromDeadline) throws Exception {
        setCurrentTime(secondsFromDeadline);
        ResultActions result = perform(post("/api/fundings/{id}/contributions/{contributionId}/cancel",
                funding.getId(), contribution.getId()), contributor);
        assertCancellation(result, secondsFromDeadline, ContributionStatus.CANCELLED, FundingStatus.OPEN);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void validatesOwnerCancellationBeforeSchedulerRuns(long secondsFromDeadline) throws Exception {
        setCurrentTime(secondsFromDeadline);
        ResultActions result = perform(post("/api/fundings/{id}/cancel", funding.getId()), receiver);
        assertCancellation(result, secondsFromDeadline, ContributionStatus.REFUNDED, FundingStatus.CANCELLED);
    }

    private void assertCancellation(ResultActions result, long secondsFromDeadline,
                                    ContributionStatus cancelledContribution, FundingStatus cancelledFunding)
            throws Exception {
        boolean beforeDeadline = secondsFromDeadline < 0;
        if (beforeDeadline) {
            result.andExpect(status().isNoContent());
        } else {
            expectDeadlineConflict(result);
        }

        Funding stored = loadFunding();
        assertThat(stored.getStatus()).isEqualTo(beforeDeadline ? cancelledFunding : FundingStatus.OPEN);
        assertThat(stored.getCurrentAmount()).isEqualTo(beforeDeadline ? 0L : 1_000L);
        assertThat(contributions.findById(contribution.getId()).orElseThrow().getStatus())
                .isEqualTo(beforeDeadline ? cancelledContribution : ContributionStatus.COMPLETED);
        assertThat(payments.findByContributionId(contribution.getId()).orElseThrow().getStatus())
                .isEqualTo(beforeDeadline ? PaymentStatus.CANCELLED : PaymentStatus.SUCCESS);
    }

    private void setCurrentTime(long secondsFromDeadline) {
        when(clock.instant()).thenReturn(DEADLINE_UTC.plusSeconds(secondsFromDeadline));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, User user) throws Exception {
        return mockMvc.perform(request.header("Authorization",
                "Bearer " + jwtTokenProvider.createAccessToken(user.getId())));
    }

    private void expectDeadlineConflict(ResultActions result) throws Exception {
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("마감된 펀딩입니다."));
    }

    private Funding loadFunding() {
        return fundings.findById(funding.getId()).orElseThrow();
    }
}
