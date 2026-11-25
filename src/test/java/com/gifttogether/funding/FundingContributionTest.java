package com.gifttogether.funding;

import com.gifttogether.common.exception.BadRequestException;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.product.domain.Product;
import com.gifttogether.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundingContributionTest {

    private Funding createFunding() {
        User receiver = new User(
                "receiver",
                "receiver@test.com"
        );

        Product product = new Product(
                "테스트 상품",
                100_000L,
                "https://example.com/product.jpg"
        );

        return new Funding(
                receiver,
                product,
                LocalDateTime.now().plusDays(1),
                "테스트 펀딩"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "5000, 999, false", "5000, 1000, true", "1001, 1000, true",
            "1000, 999, false", "1000, 1000, true", "999, 999, true",
            "999, 998, false", "999, 1000, false", "1, 1, true",
            "0, 1, false", "500, 0, false", "500, -1, false"
    })
    void validatesContributionAgainstTheCurrentRemainder(long remaining, long amount, boolean accepted) {
        Funding funding = createFunding();
        long startingAmount = 100_000L - remaining;
        funding.contribute(startingAmount);

        if (!accepted) {
            assertThatThrownBy(() -> funding.contribute(amount))
                    .isInstanceOf(remaining == 0
                            ? com.gifttogether.common.exception.ConflictException.class
                            : BadRequestException.class);
            assertThat(funding.getCurrentAmount()).isEqualTo(startingAmount);
            assertThat(funding.getStatus()).isEqualTo(remaining == 0 ? FundingStatus.COMPLETED : FundingStatus.OPEN);
            return;
        }

        funding.contribute(amount);
        assertThat(funding.getCurrentAmount()).isEqualTo(startingAmount + amount);
        assertThat(funding.getStatus()).isEqualTo(amount == remaining ? FundingStatus.COMPLETED : FundingStatus.OPEN);
    }

    @Test
    void 천원_미만은_참여할_수_없다() {
        Funding funding = createFunding();

        assertThatThrownBy(() ->
                funding.contribute(999L)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("최소 참여 금액은 1,000원입니다.");

        assertThat(funding.getCurrentAmount())
                .isZero();
    }

    @Test
    void 남은_금액을_초과해서_참여할_수_없다() {
        Funding funding = createFunding();

        funding.contribute(90_000L);

        assertThatThrownBy(() ->
                funding.contribute(20_000L)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("남은 금액을 초과할 수 없습니다.");

        assertThat(funding.getCurrentAmount())
                .isEqualTo(90_000L);

        assertThat(funding.getStatus())
                .isEqualTo(FundingStatus.OPEN);
    }

    @Test
    void 목표_금액을_정확히_채우면_펀딩이_완료된다() {
        Funding funding = createFunding();

        funding.contribute(30_000L);

        assertThat(funding.getStatus())
                .isEqualTo(FundingStatus.OPEN);

        funding.contribute(70_000L);

        assertThat(funding.getCurrentAmount())
                .isEqualTo(100_000L);

        assertThat(funding.getStatus())
                .isEqualTo(FundingStatus.COMPLETED);
    }

    @Test
    void 완료된_펀딩에는_추가로_참여할_수_없다() {
        Funding funding = createFunding();

        funding.contribute(100_000L);

        assertThatThrownBy(() ->
                funding.contribute(1_000L)
        )
                .isInstanceOf(
                        com.gifttogether.common.exception.ConflictException.class
                )
                .hasMessage("진행 중인 펀딩이 아닙니다.");

        assertThat(funding.getCurrentAmount())
                .isEqualTo(100_000L);

        assertThat(funding.getStatus())
                .isEqualTo(FundingStatus.COMPLETED);
    }
}