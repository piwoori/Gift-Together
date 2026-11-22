# Gift Together 작업 가이드

## 프로젝트 개요

Gift Together는 여러 사용자가 위시리스트 상품의 구매 금액을 나누어 부담하는 공동 선물 펀딩 서비스다. 사용자는 본인의 위시리스트 상품으로 펀딩을 만들고, 서버는 생성 당시 상품 가격을 목표 금액으로 저장한다. 참여 금액은 최소 1,000원이며 남은 목표 금액 이하여야 한다. 익명 참여의 공개 응답에서는 참여자 식별 정보를 숨긴다.

펀딩은 `OPEN`에서 시작한다. 목표를 달성하면 `COMPLETED`, 생성자가 취소하면 `CANCELLED`, 목표 미달 상태로 마감되면 `EXPIRED`가 된다. 결제는 모의 결제이며, 결제에 실패한 참여 금액은 모금액에 반영하지 않는다. 진행 중인 펀딩에서는 본인 참여를 취소할 수 있고, 생성자가 펀딩을 취소하면 완료된 참여의 결제를 취소한다. 목표 미달로 만료된 펀딩의 모금액은 수신자 지갑에 적립한다. 회원가입과 로그인, 보호된 API 인증에는 Spring Security와 JWT Bearer 토큰을 사용한다.

## 기술 스택과 구조

| 위치 | 구성 |
| --- | --- |
| `src/main/java/com/gifttogether/` | Java 17, Spring Boot 3.5, Spring MVC, Spring Data JPA, Spring Security, Bean Validation, Springdoc OpenAPI |
| `src/main/resources/` | PostgreSQL 연결 설정, Flyway 마이그레이션 `db/migration/` |
| `src/test/java/com/gifttogether/` | JUnit 5, Spring Boot Test, H2, Testcontainers PostgreSQL |
| `frontend/` | Next.js 16 App Router, React 19, TypeScript, Tailwind CSS 4, ESLint |
| 저장소 루트 | Gradle Wrapper 및 백엔드 빌드 설정 |
| `docs/` | 기존 OPEN 펀딩 마감 시각 점검 안내와 읽기 전용 SQL |

백엔드는 `auth`, `user`, `product`, `wishlist`, `funding`, `contribution`, `payment`, `wallet` 등 기능별 패키지로 구성한다. 공통 예외는 `common/exception`, 애플리케이션 설정은 `config`에 있다. 프런트엔드 라우트는 `frontend/app/`, API 호출과 인증·마감 시각 관련 공통 로직은 `frontend/lib/`에 있다. 프런트엔드 작업 전에는 `frontend/AGENTS.md`와 그 파일이 안내하는 설치 버전의 Next.js 문서를 확인한다.

## 코드 작성 원칙

- 기존 기능별 패키지와 계층을 따른다. 컨트롤러는 인증 주체와 요청을 받아 DTO로 응답하고, 서비스는 유스케이스와 트랜잭션을 조율하며, 도메인은 상태 전이와 금액 규칙을 관리한다.
- 기존 검증, 도메인 메서드, 저장소 쿼리, `GlobalExceptionHandler`를 재사용한다. API 변경 시 프런트엔드 호출부, 인증·인가, 익명 응답, 기존 데이터의 호환성을 함께 확인한다.
- 참여, 참여 취소, 펀딩 취소, 만료 처리에서는 `FundingRepository.findByIdWithLock`의 `PESSIMISTIC_WRITE` 잠금을 유지하고 잠금 획득 후 상태와 금액을 검증한다. 관련 금액·상태·거래 기록은 같은 트랜잭션에서 갱신한다.
- 펀딩 마감 시각은 오프셋 없는 `LocalDateTime`이며 `Asia/Seoul`로 해석한다. 기존 `FundingTime`과 주입된 `Clock`을 활용하고, 같은 작업에서는 기준 시각을 공유한다. 마감 정각부터는 스케줄러 실행 여부와 관계없이 참여와 취소가 차단되는 규칙을 유지한다.
- 작업과 직접 관련된 변경만 포함한다. 자격 증명, JWT 비밀 키, 데이터베이스 덤프와 로컬 백업을 커밋하지 않는다.

## 테스트 명령어

백엔드는 저장소 루트에서 Gradle Wrapper로 실행한다.

```bash
./gradlew test
./gradlew test --tests '*ContributionConcurrencyTest' --tests '*FundingDeadlinePostgresTest'
TZ=UTC ./gradlew test --tests '*FundingDeadline*' --tests '*FundingExpirationSchedulerTest'
```

`ContributionConcurrencyTest`와 `FundingDeadlinePostgresTest`에는 Docker 호환 런타임이 필요하다. 통합 테스트의 연결 대상은 테스트가 설정한 H2 메모리 DB 또는 Testcontainers의 임시 PostgreSQL로 유지한다. PostgreSQL 잠금이나 마이그레이션 변경은 PostgreSQL 테스트로 확인한다.

프런트엔드는 의존성 설치 후 다음 명령을 실행한다.

```bash
cd frontend
npm run lint
TZ=UTC node --test tests/deadline.test.mjs
npm run build
```

## PostgreSQL 데이터 보존

- 스키마 변경이 필요한 경우 먼저 사용자 승인을 받고, 기존 데이터와의 호환성 및 롤백 방안을 검토한 뒤 별도의 테스트 데이터베이스에서 검증한다.
- 기본 애플리케이션은 로컬 PostgreSQL의 `gift_together`에 연결한다. 서버 기동은 Flyway 마이그레이션과 만료 스케줄러를 실행할 수 있으므로, 데이터베이스 접근이 금지된 작업에서는 서버를 기동하지 않는다.
- 기본 설정의 `spring.jpa.hibernate.ddl-auto=validate`, `spring.sql.init.mode=never`, `spring.flyway.enabled=true`, `spring.flyway.baseline-on-migrate=false`를 유지한다. `data.sql`은 기본 설정에서 자동 실행되지 않는다. `create`와 `create-drop`은 폐기 가능한 테스트 DB로 한정한다.
- 스키마 변경은 `src/main/resources/db/migration/`에 새 버전의 Flyway 마이그레이션으로 추가한다. 이미 적용된 마이그레이션을 수정하거나 기존 데이터를 초기화해 검증 오류를 해결하지 않는다.
- 기존 데이터베이스의 삭제·재생성, `TRUNCATE`, Flyway `clean`, 일괄 데이터 수정은 별도 승인 없이 실행하지 않는다. 승인된 마이그레이션이나 데이터 보정은 실행 전에 대상 DB, 백업과 복구 방법, 영향 범위를 확인한다.
- 기존 OPEN 펀딩의 마감 시각은 저장된 값만으로 원래 입력의 시간대를 판별할 수 없다. 권한이 있는 운영자가 읽기 전용으로 점검할 때는 [마감 시각 점검 안내](docs/open-funding-deadline-audit.md)를 따르고, 근거 없이 기존 시각을 일괄 변환하지 않는다.

## 지갑 동시성

- `FundingService.expireFunding`은 펀딩 행을 잠근 뒤 이미 만료된 펀딩을 건너뛴다. `wallet_transactions(funding_id, type)` 유일 제약은 같은 펀딩의 중복 적립 내역을 방지한다. 만료 상태 변경, 잔액 적립, 거래 기록은 같은 트랜잭션에서 처리한다.
- 같은 수신자의 서로 다른 펀딩은 각자 다른 펀딩 행을 잠근다. 현재 `WalletRepository.findByUserId`에는 지갑 잠금이 없고 `Wallet.deposit`은 읽어 온 잔액에 금액을 더한다. 서로 다른 펀딩을 동시에 만료시키면 한쪽 잔액 갱신이 덮어써질 수 있다.
- `WalletService.getWallet`과 만료 처리 모두 지갑이 없으면 생성한다. 지갑 조회와 첫 적립이 겹치거나 여러 첫 적립이 겹치면 `wallets(user_id)` 유일 제약으로 한 트랜잭션이 실패할 수 있다.
- 지갑 동시성을 변경할 때는 기존 지갑의 잔액 갱신과 최초 생성 경합을 모두 다룬다. 관련 경로에서 잠금 순서를 일관되게 유지하고, PostgreSQL에서 동일 펀딩 재처리, 동일 수신자의 서로 다른 펀딩 동시 만료, 지갑 조회와 최초 적립 경합을 검증한다. 최종 잔액과 거래 기록이 함께 일치하는지 확인한다.

## Git 작업 규칙

- 시작할 때 `git branch --show-current`와 `git status --short`로 브랜치와 작업 트리를 확인한다. 작업 브랜치가 지정되었다면 현재 브랜치와 대조하고 불일치를 알린다.
- 사용자 변경을 보존하고 요청 범위의 파일만 수정한다. 작업 후 `git status --short`와 변경 내용을 확인한다. 새 파일은 기본 `git diff`에 표시되지 않으므로 파일 내용도 직접 확인한다.
- 커밋, 푸시, PR 생성은 사용자가 요청한 경우에만 한다. 요청된 커밋에는 관련 파일만 포함하고, 검증 결과와 실행하지 못한 검증을 작업 보고에 적는다.
