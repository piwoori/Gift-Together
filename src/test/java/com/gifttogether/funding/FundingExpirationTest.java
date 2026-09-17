package com.gifttogether.funding;

import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.service.ContributionService;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.funding.service.FundingService;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wallet.domain.Wallet;
import com.gifttogether.wallet.domain.WalletTransactionType;
import com.gifttogether.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.gifttogether.wallet.repository.WalletTransactionRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expiration-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class FundingExpirationTest {

    @Autowired
    FundingService fundingService;

    @Autowired
    ContributionService contributionService;

    @Autowired
    FundingRepository fundingRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    WalletTransactionRepository walletTransactionRepository;

    @Test
    void 목표금액_미달로_마감되면_모금액이_수령자_지갑에_지급된다() {

        // given
        User receiver = userRepository.save(
                new User("receiver", "receiver@test.com")
        );

        User contributor = userRepository.save(
                new User("contributor", "contributor@test.com")
        );

        Product product = productRepository.save(
                new Product(
                        "테스트 상품",
                        150_000L,
                        "https://example.com/test.jpg"
                )
        );

        Funding funding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusMinutes(10),
                        "만료 테스트"
                )
        );

        contributionService.contribute(
                funding.getId(),
                contributor.getId(),
                new ContributionCreateRequest(
                        80_000L,
                        false,
                        false
                )
        );

        // 테스트를 위해 마감 처리 기준 시간을
        // 실제 expiredAt보다 미래로 전달
        fundingService.expireFunding(
                funding.getId(),
                LocalDateTime.now().plusMinutes(20)
        );

        // when
        Funding expiredFunding =
                fundingRepository.findById(funding.getId()).orElseThrow();

        Wallet wallet =
                walletRepository.findByUserId(receiver.getId()).orElseThrow();

        // then
        assertThat(expiredFunding.getStatus())
                .isEqualTo(FundingStatus.EXPIRED);

        assertThat(expiredFunding.getCurrentAmount())
                .isEqualTo(80_000L);

        assertThat(wallet.getBalance())
                .isEqualTo(80_000L);

        assertThat(
                walletTransactionRepository.existsByFundingIdAndType(
                        funding.getId(),
                        WalletTransactionType.FUNDING_EXPIRED_REWARD
                )
        ).isTrue();
    }

    @Test
    void 같은_펀딩을_두번_만료처리해도_지갑에는_한번만_지급된다() {

        // given
        User receiver = userRepository.save(
                new User("receiver2", "receiver2@test.com")
        );

        User contributor = userRepository.save(
                new User("contributor2", "contributor2@test.com")
        );

        Product product = productRepository.save(
                new Product(
                        "테스트 상품2",
                        150_000L,
                        "https://example.com/test2.jpg"
                )
        );

        Funding funding = fundingRepository.save(
                new Funding(
                        receiver,
                        product,
                        LocalDateTime.now().plusMinutes(10),
                        "중복 만료 테스트"
                )
        );

        contributionService.contribute(
                funding.getId(),
                contributor.getId(),
                new ContributionCreateRequest(
                        80_000L,
                        false,
                        false
                )
        );

        LocalDateTime expirationTime =
                LocalDateTime.now().plusMinutes(20);

        // when
        fundingService.expireFunding(
                funding.getId(),
                expirationTime
        );

        fundingService.expireFunding(
                funding.getId(),
                expirationTime
        );

        // then
        Wallet wallet = walletRepository
                .findByUserId(receiver.getId())
                .orElseThrow();

        assertThat(wallet.getBalance())
                .isEqualTo(80_000L);

        assertThat(
                walletTransactionRepository.existsByFundingIdAndType(
                        funding.getId(),
                        WalletTransactionType.FUNDING_EXPIRED_REWARD
                )
        ).isTrue();
    }
}