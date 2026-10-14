
"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

type FundingStatus = "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";

type FundingInfo = {
    fundingId: number;
    status: FundingStatus;
    expiredAt: string;
    remainingAmount: number;
};

function getDeadlineTime(expiredAt: string): number {
    const hasTimezone = /(?:Z|[+-]\d{2}:\d{2})$/i.test(expiredAt);

    const dateString = hasTimezone
        ? expiredAt
        : `${expiredAt}+09:00`;

    return new Date(dateString).getTime();
}

export default function ContributePage() {
    const params = useParams<{ fundingId: string }>();
    const router = useRouter();
    const fundingId = params.fundingId;

    const [amount, setAmount] = useState("");
    const [anonymous, setAnonymous] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [funding, setFunding] = useState<FundingInfo | null>(null);
    const [fundingLoading, setFundingLoading] = useState(true);
    const [deadlinePassed, setDeadlinePassed] = useState(false);

    // 1. 펀딩 정보 조회
    useEffect(() => {
        let active = true;

        async function loadFunding() {
            setFundingLoading(true);
            setFunding(null);
            setError("");

            try {
                const response = await fetch(`/api/fundings/${fundingId}`, {
                    cache: "no-store",
                });

                if (!response.ok) {
                    throw new Error("펀딩 정보를 불러오지 못했습니다.");
                }

                const data: FundingInfo = await response.json();

                if (active) {
                    setFunding(data);
                }
            } catch (err) {
                if (active) {
                    setError(
                        err instanceof Error
                            ? err.message
                            : "펀딩 정보를 불러오지 못했습니다."
                    );
                }
            } finally {
                if (active) {
                    setFundingLoading(false);
                }
            }
        }

        loadFunding();

        return () => {
            active = false;
        };
    }, [fundingId]);

    // 2. 마감 시간 실시간 감지
    useEffect(() => {
        if (!funding || funding.status !== "OPEN") return;

        const deadline = getDeadlineTime(funding.expiredAt);

        function checkDeadline() {
            setDeadlinePassed(
                !Number.isFinite(deadline) || Date.now() >= deadline
            );
        }

        checkDeadline();

        const interval = window.setInterval(checkDeadline, 1000);

        return () => window.clearInterval(interval);
    }, [funding]);

    const canContribute =
        !fundingLoading &&
        funding?.status === "OPEN" &&
        !deadlinePassed &&
        Number.isFinite(getDeadlineTime(funding.expiredAt)) &&
        Date.now() < getDeadlineTime(funding.expiredAt);

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        if (!canContribute || !funding) {
            setError("현재 참여할 수 없는 펀딩입니다.");
            return;
        }

        const contributionAmount = Number(amount);

        if (
            !Number.isSafeInteger(contributionAmount) ||
            contributionAmount < 1000
        ) {
            setError("최소 1,000원 이상 입력해 주세요.");
            return;
        }

        if (contributionAmount > funding.remainingAmount) {
            setError(
                `남은 금액 ${funding.remainingAmount.toLocaleString()}원을 초과할 수 없습니다.`
            );
            return;
        }

        setLoading(true);
        setError("");

        try {
            const response = await fetch(
                `/api/fundings/${fundingId}/contributions`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "X-USER-ID": "1",
                    },
                    body: JSON.stringify({
                        amount: contributionAmount,
                        anonymous,
                        simulatePaymentFailure: false,
                    }),
                }
            );

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);

                throw new Error(
                    errorData?.message ||
                    "펀딩 참여에 실패했습니다. 마감 여부와 남은 금액을 확인해 주세요."
                );
            }

            router.push(`/fundings/${fundingId}`);
            router.refresh();
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "참여 처리 중 문제가 발생했습니다."
            );
        } finally {
            setLoading(false);
        }
    }

    const unavailableMessage = funding
        ? funding.status === "COMPLETED"
            ? "🎉 이미 목표 금액을 달성한 펀딩이에요."
            : funding.status === "EXPIRED"
                ? "🎁 마감된 펀딩이에요."
                : funding.status === "CANCELLED"
                    ? "이 펀딩은 취소되었어요."
                    : deadlinePassed
                        ? "⏳ 참여 가능 시간이 종료되었어요."
                        : ""
        : "";

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link
                        href={`/fundings/${fundingId}`}
                        className="text-xl text-gray-600"
                    >
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        공동 선물 참여하기
                    </h1>
                </header>

                <form
                    onSubmit={handleSubmit}
                    className="space-y-7 px-6 py-8"
                >
                    <div className="rounded-2xl bg-[#e9f7f4] p-5">
                        <p className="text-sm text-[#328c82]">
                            함께 만드는 특별한 선물 🎁
                        </p>
                        <h2 className="mt-2 text-xl font-bold text-gray-900">
                            마음을 담아 선물해 주세요
                        </h2>
                        <p className="mt-2 text-sm text-gray-600">
                            원하는 금액만큼 자유롭게 참여할 수 있어요.
                        </p>
                    </div>

                    {fundingLoading && (
                        <p className="text-sm text-gray-500">
                            펀딩 정보를 확인하는 중...
                        </p>
                    )}

                    {unavailableMessage && (
                        <div
                            role="status"
                            className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-gray-700"
                        >
                            {unavailableMessage}
                        </div>
                    )}

                    <div>
                        <label
                            htmlFor="amount"
                            className="mb-3 block text-sm font-semibold text-gray-700"
                        >
                            참여 금액
                        </label>

                        <div className="relative">
                            <input
                                id="amount"
                                type="number"
                                min="1000"
                                step="1"
                                required
                                disabled={!canContribute || loading}
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                                placeholder="금액을 입력해 주세요"
                                className="w-full rounded-xl border border-gray-200 p-4 pr-12 text-gray-900 disabled:bg-gray-50"
                            />
                            <span className="absolute right-4 top-4 text-gray-500">
                원
              </span>
                        </div>

                        <div className="mt-3 flex gap-2">
                            {[10000, 30000, 50000].map((value) => (
                                <button
                                    key={value}
                                    type="button"
                                    disabled={!canContribute || loading}
                                    onClick={() =>
                                        setAmount((prev) =>
                                            String((Number(prev) || 0) + value)
                                        )
                                    }
                                    className="flex-1 rounded-xl border border-gray-200 py-3 text-sm font-medium text-gray-700 disabled:opacity-50"
                                >
                                    +{value / 10000}만원
                                </button>
                            ))}
                        </div>

                        <p className="mt-3 text-xs text-gray-500">
                            최소 참여 금액은 1,000원입니다.
                            {funding && funding.status === "OPEN" && (
                                <>
                                    {" "}남은 금액은{" "}
                                    {funding.remainingAmount.toLocaleString()}원입니다.
                                </>
                            )}
                        </p>
                    </div>

                    <label className="flex items-center gap-3 rounded-xl border border-gray-100 p-4">
                        <input
                            type="checkbox"
                            checked={anonymous}
                            disabled={!canContribute || loading}
                            onChange={(e) => setAnonymous(e.target.checked)}
                            className="h-4 w-4 accent-[#328c82]"
                        />
                        <span className="text-sm text-gray-700">
              익명으로 참여하기
            </span>
                    </label>

                    <div className="rounded-xl bg-gray-50 p-4 text-xs leading-6 text-gray-600">
                        현재는 실제 결제가 아닌 테스트용 모의 결제입니다.
                        목표 금액을 달성하면 모의 선물 구매가 완료되며,
                        달성하지 못하고 마감되면 모인 금액은
                        선물 받는 사람의 모의 지갑으로 전달됩니다.
                    </div>

                    {error && (
                        <p role="alert" className="text-sm text-red-600">
                            {error}
                        </p>
                    )}

                    <button
                        type="submit"
                        disabled={loading || !canContribute}
                        className="w-full rounded-xl bg-[#328c82] py-4 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading
                            ? "참여 처리 중..."
                            : fundingLoading
                                ? "펀딩 확인 중..."
                                : "선물에 참여하기 🎁"}
                    </button>

                    {!canContribute && !fundingLoading && (
                        <Link
                            href={`/fundings/${fundingId}`}
                            className="block text-center text-sm font-medium text-[#328c82]"
                        >
                            펀딩 상세 페이지로 돌아가기 →
                        </Link>
                    )}
                </form>
            </div>
        </main>
    );
}
