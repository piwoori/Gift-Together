"use client";

import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { Suspense, useState } from "react";

function FundingCreateForm() {
    const router = useRouter();
    const searchParams = useSearchParams();

    const productId = Number(searchParams.get("productId"));

    const [expiredAt, setExpiredAt] = useState("");
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        if (!Number.isSafeInteger(productId) || productId <= 0) {
            setError("상품 정보가 올바르지 않습니다.");
            return;
        }

        const deadline = new Date(expiredAt).getTime();

        if (!expiredAt || !Number.isFinite(deadline) || deadline <= Date.now()) {
            setError("현재보다 미래의 마감일을 선택해 주세요.");
            return;
        }

        setLoading(true);
        setError("");

        try {
            const response = await fetch("/api/fundings", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-USER-ID": "1",
                },
                body: JSON.stringify({
                    productId,
                    expiredAt: `${expiredAt}:00`,
                    message,
                }),
            });

            if (!response.ok) {
                throw new Error("펀딩 생성에 실패했습니다.");
            }

            const data = await response.json();
            router.push(`/fundings/${data.fundingId}`);
        } catch {
            setError("펀딩을 생성하지 못했습니다. 다시 시도해 주세요.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link href="/wishlist" className="text-xl text-gray-600">
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        공동 선물 펀딩 만들기
                    </h1>
                </header>

                <form onSubmit={handleSubmit} className="space-y-7 px-6 py-8">
                    <div>
                        <label
                            htmlFor="expiredAt"
                            className="mb-2 block text-sm font-semibold text-gray-700"
                        >
                            펀딩 마감일
                        </label>
                        <input
                            id="expiredAt"
                            type="datetime-local"
                            required
                            value={expiredAt}
                            onChange={(e) => setExpiredAt(e.target.value)}
                            className="w-full rounded-xl border border-gray-200 p-4 text-gray-900"
                        />
                    </div>

                    <div>
                        <label
                            htmlFor="message"
                            className="mb-2 block text-sm font-semibold text-gray-700"
                        >
                            친구들에게 전할 메시지
                        </label>
                        <textarea
                            id="message"
                            rows={4}
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            placeholder="생일 선물로 갖고 싶어요 🎂"
                            className="w-full resize-none rounded-xl border border-gray-200 p-4 text-gray-900"
                        />
                    </div>

                    <div className="rounded-xl bg-[#e9f7f4] p-4 text-sm leading-6 text-gray-700">
                        목표 금액을 달성하면 상품 선물이 완료됩니다.
                        <br />
                        목표 금액을 달성하지 못하면 모인 금액은
                        선물 받는 사람의 카카오머니로 전달됩니다.
                    </div>

                    {error && (
                        <p className="text-sm text-red-600">{error}</p>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full rounded-xl bg-[#328c82] py-4 font-semibold text-white disabled:opacity-50"
                    >
                        {loading ? "펀딩 생성 중..." : "펀딩 생성하기"}
                    </button>
                </form>
            </div>
        </main>
    );
}

export default function FundingCreatePage() {
    return (
        <Suspense fallback={<p className="p-6">화면을 불러오는 중...</p>}>
            <FundingCreateForm />
        </Suspense>
    );
}