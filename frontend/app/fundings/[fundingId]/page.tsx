import Link from "next/link";
import ContributionHistory from "./ContributionHistory";

type Funding = {
    fundingId: number;
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

    if (!response.ok) {
        return null;
    }

    return response.json();
}

export default async function FundingDetailPage({
                                                    params,
                                                }: {
    params: Promise<{ fundingId: string }>;
}) {
    const { fundingId } = await params;
    const funding = await getFunding(fundingId);

    if (!funding) {
        return (
            <main className="flex min-h-screen items-center justify-center">
                펀딩 정보를 찾을 수 없습니다.
            </main>
        );
    }

    const progress = Math.min(
        100,
        (funding.currentAmount / funding.targetAmount) * 100
    );

    const formatMoney = (amount: number) =>
        amount.toLocaleString("ko-KR") + "원";

    const statusLabel = {
        OPEN: "진행 중",
        COMPLETED: "선물 완성",
        EXPIRED: "모금 마감",
        CANCELLED: "펀딩 취소",
    }[funding.status];

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link href="/wishlist" className="text-xl text-gray-600">
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        공동 선물 펀딩
                    </h1>
                </header>

                <section className="space-y-7 px-6 py-8">
                    <div className="flex h-52 items-center justify-center rounded-2xl bg-[#e9f7f4] text-6xl">
                        🎧
                    </div>

                    <div>
            <span className="rounded-full bg-[#e9f7f4] px-3 py-1 text-sm font-semibold text-[#328c82]">
              {statusLabel}
            </span>

                        <h2 className="mt-4 text-2xl font-bold text-gray-900">
                            {funding.productName}
                        </h2>

                        <p className="mt-2 text-sm text-gray-500">
                            {funding.receiverNickname}님의 위시리스트 선물
                        </p>
                    </div>

                    <div className="rounded-2xl border border-gray-100 p-5">
                        <div className="flex items-end justify-between">
                            <p className="text-2xl font-bold text-[#328c82]">
                                {formatMoney(funding.currentAmount)}
                            </p>

                            <p className="text-sm text-gray-500">
                                / {formatMoney(funding.targetAmount)}
                            </p>
                        </div>

                        <div className="mt-4 h-3 overflow-hidden rounded-full bg-gray-100">
                            <div
                                className="h-full rounded-full bg-[#5db6a8]"
                                style={{ width: `${progress}%` }}
                            />
                        </div>

                        <div className="mt-3 flex justify-between text-sm text-gray-500">
                            <span>{progress.toFixed(0)}% 달성</span>
                            <span>{formatMoney(funding.remainingAmount)} 남음</span>
                        </div>
                    </div>

                    <div className="space-y-3 rounded-2xl bg-gray-50 p-5 text-sm">
                        <div className="flex justify-between">
                            <span className="text-gray-500">참여 건수</span>
                            <span className="font-semibold">
                {funding.participantCount}건
              </span>
                        </div>

                        <div className="flex justify-between">
                            <span className="text-gray-500">마감일</span>
                            <span className="font-semibold">
                {funding.expiredAt.replace("T", " ")}
              </span>
                        </div>
                    </div>

                    {funding.message && (
                        <div className="rounded-2xl bg-[#e9f7f4] p-5">
                            <p className="mb-2 text-sm font-semibold text-[#328c82]">
                                💌 친구들에게 전하는 메시지
                            </p>
                            <p className="whitespace-pre-line text-sm leading-7 text-gray-700">
                                {funding.message}
                            </p>
                        </div>
                    )}

                    <div className="rounded-xl bg-gray-50 p-4 text-xs leading-6 text-gray-500">
                        목표 금액을 달성하면 상품 선물이 완료됩니다.
                        목표 금액을 달성하지 못하면 모인 금액은
                        선물 받는 사람의 카카오머니로 전달됩니다.
                    </div>

                    {funding.status === "OPEN" && (
                        <Link
                            href={`/fundings/${funding.fundingId}/contribute`}
                            className="block w-full rounded-xl bg-[#328c82] py-4 text-center font-semibold text-white"
                        >
                            이 선물에 참여하기 🎁
                        </Link>
                    )}

                    <ContributionHistory
                        fundingId={funding.fundingId}
                        fundingStatus={funding.status}
                    />
                    
                </section>
            </div>
        </main>
    );
}