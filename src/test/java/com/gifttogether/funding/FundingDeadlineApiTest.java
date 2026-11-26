package com.gifttogether.funding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gifttogether.auth.jwt.JwtTokenProvider;
import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.dto.ContributionCreateRequest;
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
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    ObjectMapper json;
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
                    .andExpect(jsonPath("$.expiredAt").value("2026-12-01T18:00:00"))
                    .andExpect(resultValue -> {
                        long id = json.readTree(resultValue.getResponse().getContentAsString())
                                .get("fundingId").longValue();
                        Funding created = fundings.findById(id).orElseThrow();
                        assertThat(created.getCreatedAt()).isEqualTo(DEADLINE_SEOUL.minusSeconds(1));
                        assertThat(created.getUpdatedAt()).isEqualTo(created.getCreatedAt());
                    });
        } else {
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Z", "+09:00", "-05:00", "+00:00", "[Asia/Seoul]"})
    void rejectsDeadlinesWithTimezoneInformation(String suffix) throws Exception {
        setCurrentTime(-1);
        wishlist.save(new WishlistItem(receiver, product));
        long fundingCount = fundings.count();
        perform(post("/api/fundings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productId":%d,"expiredAt":"2026-12-01T18:00:00%s"}
                        """.formatted(product.getId(), suffix)), receiver)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        "마감일(expiredAt)은 시간대 오프셋이 없는 ISO 8601 한국 시간이어야 합니다."));
        assertThat(fundings.count()).isEqualTo(fundingCount);
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
            Contribution added = contributions.findAllByFundingId(funding.getId()).stream()
                    .filter(item -> !item.getId().equals(contribution.getId()))
                    .findFirst().orElseThrow();
            LocalDateTime recordedAt = DEADLINE_SEOUL.minusSeconds(1);
            assertThat(loadFunding().getUpdatedAt()).isEqualTo(recordedAt);
            assertThat(added.getCreatedAt()).isEqualTo(recordedAt);
            assertThat(added.getUpdatedAt()).isEqualTo(recordedAt);
            assertThat(payments.findByContributionId(added.getId()).orElseThrow().getPaidAt())
                    .isEqualTo(recordedAt);
        } else {
            expectDeadlineConflict(result, "FUNDING_DEADLINE_PASSED");
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
                .content("{\"amount\":2000,\"simulatePaymentFailure\":true}"), contributor), "FUNDING_DEADLINE_PASSED");
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
            expectDeadlineConflict(result, "CONFLICT");
        }

        Funding stored = loadFunding();
        assertThat(stored.getStatus()).isEqualTo(beforeDeadline ? cancelledFunding : FundingStatus.OPEN);
        assertThat(stored.getCurrentAmount()).isEqualTo(beforeDeadline ? 0L : 1_000L);
        Contribution storedContribution = contributions.findById(contribution.getId()).orElseThrow();
        Payment storedPayment = payments.findByContributionId(contribution.getId()).orElseThrow();
        assertThat(storedContribution.getStatus())
                .isEqualTo(beforeDeadline ? cancelledContribution : ContributionStatus.COMPLETED);
        assertThat(storedPayment.getStatus())
                .isEqualTo(beforeDeadline ? PaymentStatus.CANCELLED : PaymentStatus.SUCCESS);
        if (beforeDeadline) {
            LocalDateTime recordedAt = DEADLINE_SEOUL.minusSeconds(1);
            assertThat(stored.getUpdatedAt()).isEqualTo(recordedAt);
            assertThat(storedContribution.getUpdatedAt()).isEqualTo(recordedAt);
            assertThat(storedPayment.getCancelledAt()).isEqualTo(recordedAt);
        } else {
            assertThat(storedPayment.getCancelledAt()).isNull();
        }
    }

    @Test
    void acceptsOptionalIdempotencyHeaderAndReplaysTheOriginalHttpResponse() throws Exception {
        setCurrentTime(-1);
        String first = perform(participationRequest("http-retry", 1_000), contributor)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        setCurrentTime(0);
        String replay = perform(participationRequest("http-retry", 1_000), contributor)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertThat(replay).isEqualTo(first);
        perform(participationRequest("http-retry", 999), contributor)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
        assertThat(contributions.findAllByFundingId(funding.getId())).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid key", "invalid,key"})
    void rejectsMalformedIdempotencyHeaders(String key) throws Exception {
        setCurrentTime(-1);
        perform(participationRequest(key, 1_000), contributor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        assertThat(contributions.findAllByFundingId(funding.getId())).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"999.5", "1000.0"})
    void rejectsNonIntegerJsonAmountWithoutRounding(String amount) throws Exception {
        setCurrentTime(-1);
        perform(post("/api/fundings/{id}/contributions", funding.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"amount\":%s}", amount)), contributor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        assertThat(contributions.findAllByFundingId(funding.getId())).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readsOriginalParticipationResultAfterDeadline(boolean failure) throws Exception {
        setCurrentTime(-1);
        String original = perform(participationRequest("lookup", 99_000)
                .content(json.writeValueAsString(new ContributionCreateRequest(
                        99_000L, true, failure))), contributor)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long paymentCount = payments.count();
        setCurrentTime(0);

        String response = perform(attemptRequest(funding.getId(), "lookup"), contributor)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.request.amount").value(99_000))
                .andExpect(jsonPath("$.request.anonymous").value(true))
                .andExpect(jsonPath("$.request.simulatePaymentFailure").value(failure))
                .andExpect(jsonPath("$.result.contributionStatus").value(failure ? "FAILED" : "COMPLETED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(response).get("result")).isEqualTo(json.readTree(original));
        assertThat(contributions.findAllByFundingId(funding.getId())).hasSize(2);
        assertThat(payments.count()).isEqualTo(paymentCount);
        assertThat(loadFunding().getCurrentAmount()).isEqualTo(failure ? 1_000L : 100_000L);
    }

    @Test
    void lookupIsPrivateAndScopesMissingResultsIdentically() throws Exception {
        setCurrentTime(-1);
        perform(participationRequest("private", 1_000), contributor).andExpect(status().isCreated());
        mockMvc.perform(attemptRequest(funding.getId(), "private")).andExpect(status().isUnauthorized());

        String missing = expectMissingAttempt(funding.getId(), "missing", contributor);
        assertThat(expectMissingAttempt(funding.getId(), "private", receiver)).isEqualTo(missing);
        Funding other = fundings.save(new Funding(receiver, product, DEADLINE_SEOUL, "다른 펀딩"));
        assertThat(expectMissingAttempt(other.getId(), "private", contributor)).isEqualTo(missing);

        perform(attemptRequest(funding.getId(), "invalid key"), contributor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private String expectMissingAttempt(Long fundingId, String key, User user) throws Exception {
        return perform(attemptRequest(fundingId, key), user)
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("CONTRIBUTION_ATTEMPT_NOT_FOUND"))
                .andReturn().getResponse().getContentAsString();
    }

    private MockHttpServletRequestBuilder attemptRequest(Long fundingId, String key) {
        return get("/api/fundings/{id}/contributions/idempotency", fundingId)
                .header("Idempotency-Key", key);
    }

    private MockHttpServletRequestBuilder participationRequest(String key, int amount) {
        return post("/api/fundings/{id}/contributions", funding.getId())
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + "}");
    }

    private void setCurrentTime(long secondsFromDeadline) {
        when(clock.instant()).thenReturn(DEADLINE_UTC.plusSeconds(secondsFromDeadline));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, User user) throws Exception {
        return mockMvc.perform(request.header("Authorization",
                "Bearer " + jwtTokenProvider.createAccessToken(user.getId())));
    }

    private void expectDeadlineConflict(ResultActions result, String code) throws Exception {
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value("마감된 펀딩입니다."));
    }

    private Funding loadFunding() {
        return fundings.findById(funding.getId()).orElseThrow();
    }
}
