package com.gifttogether.funding;

import com.gifttogether.common.exception.ConflictException;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.product.domain.Product;
import com.gifttogether.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundingDeadlineTest {

    @Test
    void 마감시간_직전에는_참여할_수_있다() {
        User receiver = new User(
                "receiver",
                "receiver@test.com"
        );

        Product product = new Product(
                "테스트 상품",
                100_000L,
                "https://example.com/product.jpg"
        );

        LocalDateTime deadline =
                LocalDateTime.of(
                        2026, 10, 5,
                        18, 0, 0
                );

        Funding funding = new Funding(
                receiver,
                product,
                deadline,
                "테스트 펀딩"
        );

        funding.contribute(
                10_000L,
                deadline.minusNanos(1)
        );

        assertThat(funding.getCurrentAmount())
                .isEqualTo(10_000L);
    }

    @Test
    void 마감시간과_정확히_같으면_참여할_수_없다() {
        User receiver = new User(
                "receiver",
                "receiver@test.com"
        );

        Product product = new Product(
                "테스트 상품",
                100_000L,
                "https://example.com/product.jpg"
        );

        LocalDateTime deadline =
                LocalDateTime.of(
                        2026, 10, 5,
                        18, 0, 0
                );

        Funding funding = new Funding(
                receiver,
                product,
                deadline,
                "테스트 펀딩"
        );

        assertThatThrownBy(() ->
                funding.contribute(
                        10_000L,
                        deadline
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("마감된 펀딩입니다.");

        assertThat(funding.getCurrentAmount())
                .isZero();
    }

    @Test
    void 마감시간과_정확히_같으면_만료할_수_있다() {
        User receiver = new User(
                "receiver",
                "receiver@test.com"
        );

        Product product = new Product(
                "테스트 상품",
                100_000L,
                "https://example.com/product.jpg"
        );

        LocalDateTime deadline =
                LocalDateTime.of(
                        2026, 10, 5,
                        18, 0, 0
                );

        Funding funding = new Funding(
                receiver,
                product,
                deadline,
                "테스트 펀딩"
        );

        funding.expire(deadline);

        assertThat(funding.getStatus())
                .isEqualTo(
                        com.gifttogether.funding.domain.FundingStatus.EXPIRED
                );
    }
}