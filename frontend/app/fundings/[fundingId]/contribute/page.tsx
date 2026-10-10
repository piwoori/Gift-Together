"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";

export default function ContributePage() {
    const params = useParams<{ fundingId: string }>();
    const router = useRouter();
    const fundingId = params.fundingId;

    const [amount, setAmount] = useState("");
    const [anonymous, setAnonymous] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        const contributionAmount = Number(amount);

        if (
            !Number.isSafeInteger(contributionAmount) ||
            contributionAmount < 1000
        ) {
            setError("최소 1,000원 이상 입력해 주세요.");
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
                    errorData?.message ?? "펀딩 참여에 실패했습니다."
                );
            }

            router.push(`/fundings/${fundingId}`);
            router.refresh();
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "잠시 후 다시 시도해 주세요."
            );
        } finally {
            setLoading(false);
        }
    }

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
                                step="1000"
                                required
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                                placeholder="금액을 입력해 주세요"
                                className="w-full rounded-xl border border-gray-200 p-4 pr-12 text-gray-900"
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
                                    onClick={() => setAmount(String(value))}
                                    className="flex-1 rounded-xl border border-gray-200 py-3 text-sm font-medium text-gray-700"
                                >
                                    +{(value / 10000).toLocaleString()}만원
                                </button>
                            ))}
                        </div>

                        <p className="mt-3 text-xs text-gray-500">
                            최소 참여 금액은 1,000원입니다.
                        </p>
                    </div>

                    <label className="flex items-center gap-3 rounded-xl border border-gray-100 p-4">
                        <input
                            type="checkbox"
                            checked={anonymous}
                            onChange={(e) => setAnonymous(e.target.checked)}
                            className="h-4 w-4 accent-[#328c82]"
                        />
                        <span className="text-sm text-gray-700">
              익명으로 참여하기
            </span>
                    </label>

                    <div className="rounded-xl bg-gray-50 p-4 text-xs leading-6 text-gray-600">
                        현재는 실제 결제가 아닌 테스트용 모의 결제입니다.
                        목표 금액을 달성하면 상품 선물이 완료되며,
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
                        disabled={loading}
                        className="w-full rounded-xl bg-[#328c82] py-4 font-semibold text-white disabled:opacity-50"
                    >
                        {loading ? "참여 처리 중..." : "선물에 참여하기 🎁"}
                    </button>
                </form>
            </div>
        </main>
    );
}