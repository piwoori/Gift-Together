
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { getMyFundings, type Funding, type FundingStatus } from "@/lib/funding";

type FundingFilter = "ALL" | FundingStatus;

const filters: { label: string; value: FundingFilter }[] = [
    { label: "전체", value: "ALL" },
    { label: "진행 중", value: "OPEN" },
    { label: "선물 완성", value: "COMPLETED" },
    { label: "모금 마감", value: "EXPIRED" },
    { label: "펀딩 취소", value: "CANCELLED" },
];

const statusLabels: Record<FundingStatus, string> = {
    OPEN: "진행 중",
    COMPLETED: "선물 완성",
    EXPIRED: "모금 마감",
    CANCELLED: "펀딩 취소",
};

const statusStyles: Record<FundingStatus, string> = {
    OPEN: "bg-blue-50 text-blue-700",
    COMPLETED: "bg-emerald-50 text-emerald-700",
    EXPIRED: "bg-gray-100 text-gray-600",
    CANCELLED: "bg-red-50 text-red-600",
};

export default function MyFundingsPage() {
    const [fundings, setFundings] = useState<Funding[]>([]);
    const [filter, setFilter] = useState<FundingFilter>("ALL");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        async function loadFundings() {
            try {
                const data = await getMyFundings(controller.signal);

                if (controller.signal.aborted) return;

                setFundings(data);
            } catch (err) {
                if (controller.signal.aborted) return;

                setError(
                    err instanceof Error
                        ? err.message
                        : "펀딩 목록을 불러오지 못했습니다."
                );
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            }
        }

        loadFundings();

        return () => controller.abort();
    }, []);

    const filteredFundings =
        filter === "ALL"
            ? fundings
            : fundings.filter((funding) => funding.status === filter);

    return (
        <main className="min-h-screen bg-[#f8f9fa]">
            <div className="mx-auto min-h-screen max-w-md bg-white shadow-sm">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link
                        href="/"
                        aria-label="홈으로 돌아가기"
                        className="text-xl text-gray-600"
                    >
                        ←
                    </Link>

                    <h1 className="text-lg font-bold text-gray-900">
                        내 공동 선물 펀딩
                    </h1>
                </header>

                <section className="px-6 pb-12 pt-10">
                    <p className="text-xs font-bold tracking-wider text-primary">
                        MY GIFT FUNDINGS
                    </p>

                    <h2 className="mt-3 text-2xl font-bold text-gray-900">
                        내가 만든 공동 선물
                    </h2>

                    <p className="mt-3 text-sm leading-7 text-gray-500">
                        친구들과 함께 만드는 특별한 선물.
                        <br />
                        펀딩 진행 상황을 한눈에 확인해 보세요.
                    </p>

                    <div className="mt-9 flex gap-2 overflow-x-auto pb-3">
                        {filters.map((item) => {
                            const selected = filter === item.value;

                            return (
                                <button
                                    key={item.value}
                                    type="button"
                                    onClick={() => setFilter(item.value)}
                                    aria-pressed={selected}
                                    className={`shrink-0 rounded-full px-4 py-2.5 text-sm font-medium transition ${
                                        selected
                                            ? "bg-primary text-white"
                                            : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                                    }`}
                                >
                                    {item.label}
                                </button>
                            );
                        })}
                    </div>

                    {loading && (
                        <p className="py-12 text-center text-sm text-gray-500">
                            펀딩 목록을 불러오는 중...
                        </p>
                    )}

                    {!loading && error && (
                        <p
                            role="alert"
                            className="mt-6 rounded-xl bg-red-50 p-4 text-sm text-red-600"
                        >
                            {error}
                        </p>
                    )}

                    {!loading && !error && (
                        <div className="mt-7">
                            <div className="mb-5 flex items-center justify-between">
                                <h3 className="text-base font-bold text-gray-900">
                                    펀딩 목록
                                </h3>

                                <span className="text-sm text-gray-400">
                  {filteredFundings.length}건
                </span>
                            </div>

                            {filteredFundings.length === 0 ? (
                                <div className="rounded-2xl border border-gray-100 px-6 py-12 text-center">
                                    <div className="text-4xl">🎁</div>

                                    <p className="mt-4 font-semibold text-gray-800">
                                        해당하는 펀딩이 없어요.
                                    </p>

                                    <p className="mt-2 text-sm text-gray-400">
                                        새로운 공동 선물을 만들어 보세요.
                                    </p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {filteredFundings.map((funding) => {
                                        const percentage =
                                            funding.targetAmount > 0
                                                ? Math.max(
                                                    0,
                                                    Math.min(
                                                        100,
                                                        Math.round(
                                                            (funding.currentAmount /
                                                                funding.targetAmount) *
                                                            100
                                                        )
                                                    )
                                                )
                                                : 0;

                                        return (
                                            <Link
                                                key={funding.fundingId}
                                                href={`/fundings/${funding.fundingId}`}
                                                className="block rounded-2xl border border-gray-100 bg-white p-5 shadow-sm transition hover:border-gray-300 hover:shadow-md"
                                            >
                                                <div className="flex items-start gap-4">
                                                    <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl bg-gray-50 text-4xl">
                                                        🎁
                                                    </div>

                                                    <div className="min-w-0 flex-1">
                            <span
                                className={`inline-block rounded-full px-2.5 py-1 text-xs font-medium ${
                                    statusStyles[funding.status]
                                }`}
                            >
                              {statusLabels[funding.status]}
                            </span>

                                                        <h4 className="mt-2 truncate text-base font-bold text-gray-900">
                                                            {funding.productName}
                                                        </h4>

                                                        <p className="mt-1 text-sm text-gray-400">
                                                            목표 {funding.targetAmount.toLocaleString("ko-KR")}원
                                                        </p>
                                                    </div>
                                                </div>

                                                <div className="mt-6 border-t border-gray-100 pt-5">
                                                    <div className="flex items-end justify-between gap-3">
                                                        <div>
                                                            <p className="text-xs text-gray-400">
                                                                현재 모인 금액
                                                            </p>

                                                            <p className="mt-1 text-xl font-bold text-primary">
                                                                {funding.currentAmount.toLocaleString("ko-KR")}원
                                                            </p>
                                                        </div>

                                                        <p className="text-sm font-semibold text-primary">
                                                            {percentage}% 달성
                                                        </p>
                                                    </div>

                                                    <div
                                                        className="mt-4 h-2 overflow-hidden rounded-full bg-gray-100"
                                                        role="progressbar"
                                                        aria-label={`${funding.productName} 펀딩 달성률`}
                                                        aria-valuemin={0}
                                                        aria-valuemax={100}
                                                        aria-valuenow={percentage}
                                                    >
                                                        <div
                                                            className="h-full rounded-full bg-primary transition-all"
                                                            style={{ width: `${percentage}%` }}
                                                        />
                                                    </div>

                                                    <p className="mt-4 text-right text-xs font-medium text-gray-400">
                                                        펀딩 자세히 보기 →
                                                    </p>
                                                </div>
                                            </Link>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}

                    <Link
                        href="/wishlist"
                        className="mt-9 block rounded-2xl bg-primary py-4 text-center font-semibold text-white transition hover:bg-primary-hover"
                    >
                        🎁 새로운 공동 선물 만들기
                    </Link>
                </section>
            </div>
        </main>
    );
}
