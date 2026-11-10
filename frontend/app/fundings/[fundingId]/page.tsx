
import Link from "next/link";

import ContributionHistory from "./ContributionHistory";
import FundingCancelActions from "./FundingCancelActions";
import FundingCompleted from "./FundingCompleted";
import FundingDeadlineActions from "./FundingDeadlineActions";
import ShareFunding from "./ShareFunding";
import WalletLink from "./WalletLink";

type Funding = {
    fundingId: number;
    receiverId: number;
    receiverNickname: string;
    productName: string;
    targetAmount: number;
    currentAmount: number;
    remainingAmount: number;
    status: "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";
    message: string;
    expiredAt: string;
    participantCount: number;
};

async function getFunding(fundingId: string): Promise<Funding | null> {
    const response = await fetch(
        `http://localhost:8080/api/fundings/${fundingId}`,
        { cache: "no-store" }
    );

    if (response.status === 404) return null;

    if (!response.ok) {
        throw new Error(`펀딩 조회에 실패했습니다. (${response.status})`);
    }

    return response.json();
}

const statusLabels: Record<Funding["status"], string> = {
    OPEN: "진행 중",
    COMPLETED: "선물 완성",
    EXPIRED: "모금 마감",
    CANCELLED: "펀딩 취소",
};

const statusStyles: Record<Funding["status"], string> = {
    OPEN: "bg-gray-100 text-gray-600",
    COMPLETED: "bg-emerald-50 text-emerald-700",
    EXPIRED: "bg-gray-100 text-gray-500",
    CANCELLED: "bg-red-50 text-red-600",
};

const formatMoney = (amount: number) =>
    `${amount.toLocaleString("ko-KR")}원`;

export default async function FundingDetailPage({
                                                    params,
                                                }: {
    params: Promise<{ fundingId: string }>;
}) {
    const { fundingId } = await params;
    const funding = await getFunding(fundingId);

    if (!funding) {
        return (
            <main className="flex min-h-screen items-center justify-center bg-[#f8f8f8]">
                <div className="text-center">
                    <p className="text-lg font-semibold text-gray-900">
                        펀딩 정보를 찾을 수 없어요.
                    </p>
                    <Link
                        href="/fundings"
                        className="mt-5 inline-block text-sm text-primary"
                    >
                        내 펀딩으로 돌아가기 →
                    </Link>
                </div>
            </main>
        );
    }

    const progress =
        funding.targetAmount > 0
            ? Math.min(
                100,
                Math.max(
                    0,
                    Math.round(
                        (funding.currentAmount / funding.targetAmount) * 100
                    )
                )
            )
            : 0;

    return (
        <main className="min-h-screen bg-[#f8f8f8]">
            <div className="mx-auto min-h-screen max-w-md bg-white shadow-sm">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-6">
                    <Link
                        href="/fundings"
                        aria-label="내 펀딩으로 돌아가기"
                        className="text-2xl text-gray-600"
                    >
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        공동 선물 상세
                    </h1>
                </header>

                <section className="space-y-8 px-6 py-10">
                    {/* 상품 정보 */}
                    <div>
                        <p className="text-xs font-bold tracking-wider text-gray-500">
                            GIFT TOGETHER
                        </p>
                        <h2 className="mt-3 text-2xl font-bold text-gray-900">
                            함께 만드는 특별한 선물
                        </h2>
                        <p className="mt-2 text-sm text-gray-500">
                            친구들의 마음을 모아 선물을 완성해 보세요.
                        </p>
                    </div>

                    <div className="rounded-2xl border border-gray-100 p-5 shadow-sm">
                        <div className="flex items-center gap-4">
                            <div className="flex h-28 w-28 shrink-0 items-center justify-center rounded-2xl bg-gray-50 text-5xl">
                                🎁
                            </div>

                            <div className="min-w-0 flex-1">
                <span
                    className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${statusStyles[funding.status]}`}
                >
                  {statusLabels[funding.status]}
                </span>
                                <h3 className="mt-3 text-lg font-bold text-gray-900">
                                    {funding.productName}
                                </h3>
                                <p className="mt-2 text-sm text-gray-500">
                                    {funding.receiverNickname}님의 선물
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* 모금 현황 */}
                    <div className="rounded-2xl border border-gray-100 p-6 shadow-sm">
                        <p className="text-sm text-gray-500">현재 모인 금액</p>
                        <p className="mt-2 text-3xl font-bold text-primary">
                            {formatMoney(funding.currentAmount)}
                        </p>

                        <div className="mt-5 flex items-center justify-between text-sm">
                            <span className="text-gray-500">목표 금액</span>
                            <span className="font-semibold text-gray-700">
                {formatMoney(funding.targetAmount)}
              </span>
                        </div>

                        <div
                            className="mt-4 h-3 overflow-hidden rounded-full bg-gray-100"
                            role="progressbar"
                            aria-label="펀딩 달성률"
                            aria-valuemin={0}
                            aria-valuemax={100}
                            aria-valuenow={progress}
                        >
                            <div
                                className="h-full rounded-full bg-primary transition-all"
                                style={{ width: `${progress}%` }}
                            />
                        </div>

                        <div className="mt-3 flex items-center justify-between text-sm">
              <span className="font-semibold text-primary">
                {progress}% 달성
              </span>
                            <span className="text-gray-500">
                {formatMoney(funding.remainingAmount)} 남았어요
              </span>
                        </div>
                    </div>

                    {/* 기본 정보 */}
                    <div className="rounded-2xl bg-gray-50 p-5">
                        <h3 className="font-bold text-gray-900">펀딩 정보</h3>

                        <div className="mt-5 space-y-4 text-sm">
                            <div className="flex items-center justify-between gap-4">
                                <span className="text-gray-500">참여 건수</span>
                                <span className="font-semibold text-gray-800">
                  {funding.participantCount}건
                </span>
                            </div>

                            <div className="flex items-center justify-between gap-4">
                                <span className="shrink-0 text-gray-500">마감일</span>
                                <span className="text-right font-semibold text-gray-800">
                  {funding.expiredAt.replace("T", " ")}
                </span>
                            </div>
                        </div>
                    </div>

                    {/* 친구들에게 전하는 메시지 */}
                    {funding.message && (
                        <div className="rounded-2xl border border-gray-100 p-5">
                            <h3 className="font-bold text-gray-900">
                                💌 친구들에게 전하는 메시지
                            </h3>
                            <p className="mt-4 whitespace-pre-line text-sm leading-7 text-gray-600">
                                {funding.message}
                            </p>
                        </div>
                    )}

                    {/* 선물 완료 */}
                    {funding.status === "COMPLETED" && (
                        <FundingCompleted
                            productName={funding.productName}
                            targetAmount={funding.targetAmount}
                            participantCount={funding.participantCount}
                        />
                    )}

                    {/* 모금 마감 */}
                    {funding.status === "EXPIRED" && (
                        <div className="rounded-2xl border border-gray-200 bg-gray-50 p-5">
                            <h3 className="text-lg font-bold text-gray-900">
                                🎁 펀딩이 마감되었어요
                            </h3>

                            <p className="mt-3 text-sm leading-6 text-gray-600">
                                {funding.currentAmount > 0
                                    ? "목표 금액을 달성하지 못해 모인 금액이 선물 받는 사람의 모의 지갑에 적립되었어요."
                                    : "목표 금액을 달성하지 못한 채 펀딩이 마감되었어요. 참여 금액이 없어 지갑에 적립된 금액은 없어요."}
                            </p>

                            <div className="mt-5 rounded-xl bg-white p-4">
                                <p className="text-xs text-gray-500">
                                    지갑 적립 금액
                                </p>
                                <p className="mt-2 text-2xl font-bold text-primary">
                                    {formatMoney(funding.currentAmount)}
                                </p>
                            </div>

                            {funding.currentAmount > 0 && (
                                <WalletLink receiverId={funding.receiverId} />
                            )}
                        </div>
                    )}

                    {/* 마감 처리 */}
                    <FundingDeadlineActions
                        fundingId={funding.fundingId}
                        expiredAt={funding.expiredAt}
                        status={funding.status}
                    />

                    {/* 공유하기 */}
                    <ShareFunding
                        fundingId={funding.fundingId}
                        productName={funding.productName}
                    />

                    {/* 참여 내역 */}
                    <ContributionHistory
                        fundingId={funding.fundingId}
                        fundingStatus={funding.status}
                        expiredAt={funding.expiredAt}
                    />

                    {/* 펀딩 취소 */}
                    <FundingCancelActions
                        fundingId={funding.fundingId}
                        receiverId={funding.receiverId}
                        expiredAt={funding.expiredAt}
                        status={funding.status}
                    />

                    {/* 서비스 안내 */}
                    <div className="rounded-xl bg-gray-50 p-4 text-xs leading-6 text-gray-500">
                        목표 금액을 달성하면 모의 선물 구매가 완료됩니다.
                        목표 금액을 달성하지 못하면 모인 금액은 선물 받는 사람의
                        모의 지갑에 적립됩니다.
                        실제 결제 및 카카오머니 연동은 지원하지 않습니다.
                    </div>
                </section>
            </div>
        </main>
    );
}
