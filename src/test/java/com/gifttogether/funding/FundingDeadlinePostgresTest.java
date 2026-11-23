package com.gifttogether.funding;

import com.gifttogether.common.exception.BadRequestException;
import com.gifttogether.common.exception.ConflictException;
import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;
import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.contribution.service.ContributionService;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.funding.scheduler.FundingExpirationScheduler;
import com.gifttogether.funding.service.FundingService;
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.domain.PaymentStatus;
import com.gifttogether.payment.repository.PaymentRepository;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wallet.domain.Wallet;
import com.gifttogether.wallet.domain.WalletTransaction;
import com.gifttogether.wallet.domain.WalletTransactionType;
import com.gifttogether.wallet.dto.WalletResponse;
import com.gifttogether.wallet.repository.WalletRepository;
import com.gifttogether.wallet.repository.WalletTransactionRepository;
import com.gifttogether.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(properties = "spring.sql.init.mode=never")
class FundingDeadlinePostgresTest {

    private static final Instant DEADLINE_UTC = Instant.parse("2026-12-01T09:00:00Z");
    private static final LocalDateTime DEADLINE_SEOUL = LocalDateTime.parse("2026-12-01T18:00:00");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gift_deadline_test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    Clock clock;
    @MockitoBean
    FundingExpirationScheduler scheduler;

    @Autowired
    FundingService fundingService;
    @Autowired
    ContributionService contributionService;
    @Autowired
    FundingRepository fundings;
    @Autowired
    ContributionRepository contributions;
    @Autowired
    PaymentRepository payments;
    @Autowired
    UserRepository users;
    @Autowired
    ProductRepository products;
    @Autowired
    TransactionTemplate transaction;
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    WalletService walletService;
    @Autowired
    WalletRepository wallets;
    @Autowired
    WalletTransactionRepository walletTransactions;

    private final AtomicReference<Instant> currentTime = new AtomicReference<>();
    private User receiver;
    private User contributor;
    private Funding funding;

    @BeforeEach
    void setUp() {
        currentTime.set(DEADLINE_UTC.minusSeconds(1));
        when(clock.instant()).thenAnswer(invocation -> currentTime.get());

        receiver = users.save(new User("receiver", UUID.randomUUID() + "@example.test"));
        contributor = users.save(new User("contributor", UUID.randomUUID() + "@example.test"));
        Product product = products.save(new Product("테스트 상품", 100_000L, null));
        funding = fundings.save(new Funding(receiver, product, DEADLINE_SEOUL, "마감 테스트",
                DEADLINE_SEOUL.minusMinutes(1)));
    }

    enum FundingAction {
        CONTRIBUTE, CANCEL_CONTRIBUTION, CANCEL_FUNDING
    }

    @ParameterizedTest
    @EnumSource(FundingAction.class)
    void rejectsRequestsThatReachTheFundingLockAtTheDeadline(FundingAction action) {
        Contribution original = addContribution();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> request = transaction.execute(status -> {
                fundings.findByIdWithLock(funding.getId()).orElseThrow();
                Integer holderPid = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                Future<?> pending = executor.submit(() -> {
                    switch (action) {
                        case CONTRIBUTE -> addContribution();
                        case CANCEL_CONTRIBUTION -> contributionService.cancelContribution(
                                funding.getId(), original.getId(), contributor.getId());
                        case CANCEL_FUNDING -> fundingService.cancelFunding(funding.getId(), receiver.getId());
                    }
                });
                awaitBlockedBy(holderPid);
                currentTime.set(DEADLINE_UTC);
                return pending;
            });

            assertThatThrownBy(() -> request.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ConflictException.class)
                    .hasRootCauseMessage("마감된 펀딩입니다.");
            assertOpenWithSuccessfulPayments(List.of(original));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rollsBackAllCancellationChangesWhenPostgresRejectsTheSecondRefund() {
        Contribution first = addContribution();
        Contribution second = addContribution();
        currentTime.set(DEADLINE_UTC.minusMillis(500));

        // A deferred trigger runs after Hibernate has flushed the refund writes.
        jdbc.execute("""
                CREATE FUNCTION reject_test_refund() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF EXISTS (SELECT FROM payments
                               WHERE contribution_id = %d AND status = 'CANCELLED') THEN
                        RAISE EXCEPTION 'test refund rejected after both payments were cancelled'
                            USING ERRCODE = '23514';
                    END IF;
                    RAISE EXCEPTION 'test did not reach both payment updates';
                END;
                $$
                """.formatted(first.getId()));
        try {
            jdbc.execute("""
                    CREATE CONSTRAINT TRIGGER reject_test_refund
                    AFTER UPDATE ON payments DEFERRABLE INITIALLY DEFERRED
                    FOR EACH ROW WHEN (NEW.contribution_id = %d AND NEW.status = 'CANCELLED')
                    EXECUTE FUNCTION reject_test_refund()
                    """.formatted(second.getId()));

            assertThatThrownBy(() -> fundingService.cancelFunding(funding.getId(), receiver.getId()))
                    .hasStackTraceContaining("test refund rejected after both payments were cancelled");
            assertOpenWithSuccessfulPayments(List.of(first, second));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS reject_test_refund ON payments");
            jdbc.execute("DROP FUNCTION reject_test_refund()");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void creditsDifferentFundingsConcurrently(boolean existingWallet) throws Exception {
        long openingBalance = existingWallet ? 5_000L : 0L;
        if (existingWallet) {
            createWallet(openingBalance);
        }
        addContribution();
        Funding other = fundings.save(new Funding(receiver, products.save(
                new Product("다른 상품", 100_000L, null)), DEADLINE_SEOUL, "동시 만료 테스트",
                DEADLINE_SEOUL.minusMinutes(1)));
        contributionService.contribute(other.getId(), contributor.getId(),
                new ContributionCreateRequest(2_000L, false, false));

        runWhileFirstTransactionIsOpen(() -> expire(funding), () -> expire(other));

        Wallet wallet = assertWalletBalance(openingBalance + 3_000L);
        assertThat(walletTransactions.findAllByWalletIdOrderByCreatedAtDesc(wallet.getId()))
                .extracting(entry -> entry.getFunding().getId(), WalletTransaction::getAmount,
                        WalletTransaction::getType)
                .containsExactlyInAnyOrder(
                        tuple(funding.getId(), 1_000L, WalletTransactionType.FUNDING_EXPIRED_REWARD),
                        tuple(other.getId(), 2_000L, WalletTransactionType.FUNDING_EXPIRED_REWARD));
        assertExpired(funding);
        assertExpired(other);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void walletLookupAndFirstCreditShareTheSameWallet(boolean lookupFirst) throws Exception {
        addContribution();
        AtomicReference<WalletResponse> response = new AtomicReference<>();
        Runnable lookup = () -> response.set(walletService.getWallet(receiver.getId()));
        Runnable credit = () -> expire(funding);

        runWhileFirstTransactionIsOpen(lookupFirst ? lookup : credit, lookupFirst ? credit : lookup);

        Wallet wallet = assertSingleCredit(1_000L, 1_000L);
        assertThat(response.get().walletId()).isEqualTo(wallet.getId());
        assertThat(response.get().userId()).isEqualTo(receiver.getId());
        assertThat(response.get().balance()).isEqualTo(lookupFirst ? 0L : 1_000L);
    }

    @Test
    void creditsTheSameFundingOnlyOnceDuringConcurrentExpiration() throws Exception {
        addContribution();

        runWhileFirstTransactionIsOpen(() -> expire(funding), () -> expire(funding));

        assertSingleCredit(1_000L, 1_000L);
    }

    @Test
    void rollsBackFundingWalletAndLedgerAfterFlushedCreditFails() {
        createWallet(5_000L);
        addContribution();
        IllegalStateException failure = new IllegalStateException("test failure after credit writes");

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            expire(funding);
            fundings.flush();
            assertSingleCredit(6_000L, 1_000L);
            throw failure;
        })).isSameAs(failure);

        Wallet wallet = assertWalletBalance(5_000L);
        assertThat(walletTransactions.findAllByWalletIdOrderByCreatedAtDesc(wallet.getId())).isEmpty();
        assertThat(fundings.findById(funding.getId()).orElseThrow().getStatus())
                .isEqualTo(FundingStatus.OPEN);

        expire(funding);
        assertSingleCredit(6_000L, 1_000L);
    }

    @Test
    void replaysLostResponseAfterOtherParticipationAndDeadline() {
        var original = contribute("first", 1_000L, false, false);
        contribute("second", 2_000L, false, false);
        currentTime.set(DEADLINE_UTC);

        assertThat(contribute("first", 1_000L, false, false)).isEqualTo(original);
        assertThat(original.currentAmount()).isEqualTo(1_000L);
        assertContributionTotals(2, 2, 3_000L);
    }

    @Test
    void serializesConcurrentRequestsWithTheSameKey() throws Exception {
        AtomicReference<ContributionCreateResponse> first = new AtomicReference<>();
        AtomicReference<ContributionCreateResponse> second = new AtomicReference<>();
        runWhileFirstTransactionIsOpen(
                () -> first.set(contribute("shared", 1_000L, false, false)),
                () -> second.set(contribute("shared", 1_000L, false, false)));

        assertThat(second.get()).isEqualTo(first.get());
        assertContributionTotals(1, 1, 1_000L);
    }

    @Test
    void replaysCompletedFundingAndRejectsChangesToAnyRequestField() {
        var original = contribute("final", 100_000L, false, false);
        assertThat(original.fundingStatus()).isEqualTo(FundingStatus.COMPLETED);
        assertThat(contribute("final", 100_000L, false, false)).isEqualTo(original);
        assertThatThrownBy(() -> contribute("final", 99_000L, false, false))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> contribute("final", 100_000L, true, false))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> contribute("final", 100_000L, false, true))
                .isInstanceOf(ConflictException.class);
        assertContributionTotals(1, 1, 100_000L);
    }

    @Test
    void replaysMockFailureWithoutAddingToFunding() {
        var original = contribute("failed", 1_000L, true, true);

        assertThat(contribute("failed", 1_000L, true, true)).isEqualTo(original);
        assertThat(original.contributionStatus()).isEqualTo(ContributionStatus.FAILED);
        assertThat(payments.findByContributionId(original.contributionId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);
        assertContributionTotals(1, 1, 0L);
    }

    @Test
    void leavesKeysAvailableAfterValidationFailureAndRollback() {
        assertThatThrownBy(() -> contribute("invalid", 100_001L, false, false))
                .isInstanceOf(BadRequestException.class);
        IllegalStateException failure = new IllegalStateException("test rollback");
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            contribute("rolled-back", 1_000L, false, false);
            fundings.flush();
            throw failure;
        })).isSameAs(failure);
        assertContributionTotals(0, 0, 0L);
        assertThat(jdbc.queryForObject("select count(*) from contribution_idempotency where funding_id = ?",
                Long.class, funding.getId())).isZero();

        contribute("invalid", 2_000L, false, false);
        contribute("rolled-back", 3_000L, false, false);
        assertContributionTotals(2, 2, 5_000L);
    }

    @Test
    void acceptsIntentionalRepeatsWithDistinctKeysAndWithoutAKey() {
        var first = contribute("first", 1_000L, false, false);
        var second = contribute("second", 1_000L, false, false);
        var third = contribute(null, 1_000L, false, false);
        var fourth = contribute(null, 1_000L, false, false);

        assertThat(List.of(first.contributionId(), second.contributionId(),
                third.contributionId(), fourth.contributionId())).doesNotHaveDuplicates();
        assertContributionTotals(4, 4, 4_000L);
    }

    private ContributionCreateResponse contribute(String key, long amount, boolean anonymous, boolean failure) {
        return contributionService.contribute(funding.getId(), contributor.getId(),
                new ContributionCreateRequest(amount, anonymous, failure), key);
    }

    private void assertContributionTotals(int contributionCount, long paymentCount, long currentAmount) {
        assertThat(contributions.findAllByFundingId(funding.getId())).hasSize(contributionCount);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM payments p JOIN contributions c ON c.id = p.contribution_id
                WHERE c.funding_id = ?
                """, Long.class, funding.getId())).isEqualTo(paymentCount);
        assertThat(fundings.findById(funding.getId()).orElseThrow().getCurrentAmount()).isEqualTo(currentAmount);
    }

    private void runWhileFirstTransactionIsOpen(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> pending = transaction.execute(status -> {
                first.run();
                Integer holderPid = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                Future<?> request = executor.submit(second);
                awaitBlockedBy(holderPid);
                return request;
            });
            pending.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitBlockedBy(Integer holderPid) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> Boolean.TRUE.equals(
                jdbc.queryForObject("""
                        SELECT EXISTS (
                            SELECT FROM pg_stat_activity
                            WHERE ? = ANY (pg_blocking_pids(pid))
                        )
                        """, Boolean.class, holderPid)));
    }

    private void createWallet(long balance) {
        Wallet wallet = new Wallet(receiver);
        wallet.deposit(balance);
        wallets.save(wallet);
    }

    private void expire(Funding target) {
        fundingService.expireFunding(target.getId(), DEADLINE_SEOUL);
    }

    private Wallet assertWalletBalance(long balance) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wallets WHERE user_id = ?",
                Long.class, receiver.getId())).isEqualTo(1L);
        Wallet wallet = wallets.findByUserId(receiver.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(balance);
        return wallet;
    }

    private Wallet assertSingleCredit(long balance, long amount) {
        Wallet wallet = assertWalletBalance(balance);
        assertThat(walletTransactions.findAllByWalletIdOrderByCreatedAtDesc(wallet.getId()))
                .extracting(entry -> entry.getFunding().getId(), WalletTransaction::getAmount,
                        WalletTransaction::getType)
                .containsExactly(tuple(funding.getId(), amount, WalletTransactionType.FUNDING_EXPIRED_REWARD));
        assertExpired(funding);
        return wallet;
    }

    private void assertExpired(Funding target) {
        assertThat(fundings.findById(target.getId()).orElseThrow().getStatus())
                .isEqualTo(FundingStatus.EXPIRED);
    }

    private Contribution addContribution() {
        var response = contributionService.contribute(funding.getId(), contributor.getId(),
                new ContributionCreateRequest(1_000L, false, false));
        return contributions.findById(response.contributionId()).orElseThrow();
    }

    private void assertOpenWithSuccessfulPayments(List<Contribution> expected) {
        Funding stored = fundings.findById(funding.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(FundingStatus.OPEN);
        assertThat(stored.getCurrentAmount()).isEqualTo(1_000L * expected.size());
        assertThat(stored.getUpdatedAt()).isEqualTo(DEADLINE_SEOUL.minusSeconds(1));
        assertThat(contributions.findAllByFundingId(funding.getId()))
                .extracting(Contribution::getId)
                .containsExactlyInAnyOrderElementsOf(expected.stream().map(Contribution::getId).toList());

        for (Contribution original : expected) {
            Contribution storedContribution = contributions.findById(original.getId()).orElseThrow();
            Payment payment = payments.findByContributionId(original.getId()).orElseThrow();
            assertThat(storedContribution.getStatus()).isEqualTo(ContributionStatus.COMPLETED);
            assertThat(storedContribution.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getCancelledAt()).isNull();
            assertThat(payment.getPaidAt()).isEqualTo(DEADLINE_SEOUL.minusSeconds(1));
        }
    }
}
