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
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link href="/" className="text-xl text-gray-600">
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        내 공동 선물 펀딩
                    </h1>
                </header>

                <div className="px-6 py-7">
                    <div className="mb-6 rounded-2xl bg-[#e9f7f4] p-5">
                        <p className="text-sm text-[#328c82]">Gift Together 🎁</p>
                        <h2 className="mt-2 text-xl font-bold text-gray-900">
                            내가 만든 공동 선물
                        </h2>
                        <p className="mt-2 text-sm text-gray-600">
                            진행 상황과 참여 내역을 한눈에 확인해 보세요.
                        </p>
                    </div>

                    <div className="mb-6 flex gap-2 overflow-x-auto pb-2">
                        {filters.map((item) => (
                            <button
                                key={item.value}
                                type="button"
                                onClick={() => setFilter(item.value)}
                                className={`shrink-0 rounded-full px-4 py-2 text-sm font-medium ${
                                    filter === item.value
                                        ? "bg-[#328c82] text-white"
                                        : "bg-gray-100 text-gray-600"
                                }`}
                            >
                                {item.label}
                            </button>
                        ))}
                    </div>

                    {loading && (
                        <p className="py-10 text-center text-sm text-gray-500">
                            펀딩 목록을 불러오는 중...
                        </p>
                    )}

                    {error && (
                        <p role="alert" className="py-10 text-center text-sm text-red-600">
                            {error}
                        </p>
                    )}

                    {!loading && !error && (
                        <>
                            <div className="mb-4 flex items-center justify-between">
                                <h3 className="font-bold text-gray-900">펀딩 목록</h3>
                                <span className="text-sm text-gray-500">
                                  {filteredFundings.length}건
                                </span>
                            </div>

                            {filteredFundings.length === 0 ? (
                                <div className="rounded-2xl border border-gray-100 p-8 text-center">
                                    <p className="text-3xl">🎁</p>
                                    <p className="mt-3 text-sm text-gray-500">
                                        해당하는 펀딩이 없어요.
                                    </p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {filteredFundings.map((funding) => {
                                        const percentage =
                                            funding.targetAmount > 0
                                                ? Math.min(
                                                    100,
                                                    Math.round(
                                                        (funding.currentAmount /
                                                            funding.targetAmount) *
                                                        100
                                                    )
                                                )
                                                : 0;

                                        return (
                                            <Link
                                                key={funding.fundingId}
                                                href={`/fundings/${funding.fundingId}`}
                                                className="block rounded-2xl border border-gray-100 p-5 transition hover:border-[#9bd5cc]"
                                            >
                                                <div className="flex items-center gap-4">
                                                    <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl bg-[#e9f7f4] text-3xl">
                                                        🎧
                                                    </div>

                                                    <div className="min-w-0 flex-1">
                                                        <div className="flex items-center justify-between gap-2">
                                                            <h4 className="truncate font-bold text-gray-900">
                                                                {funding.productName}
                                                            </h4>
                                                            <span className="shrink-0 rounded-full bg-[#e9f7f4] px-2 py-1 text-xs text-[#328c82]">
                                {statusLabels[funding.status]}
                              </span>
                                                        </div>

                                                        <p className="mt-2 text-sm text-gray-600">
                                                            {funding.currentAmount.toLocaleString()}원
                                                            {" / "}
                                                            {funding.targetAmount.toLocaleString()}원
                                                        </p>

                                                        <div className="mt-3 h-2 overflow-hidden rounded-full bg-gray-100">
                                                            <div
                                                                className="h-full rounded-full bg-[#5db5a5]"
                                                                style={{ width: `${percentage}%` }}
                                                            />
                                                        </div>

                                                        <p className="mt-2 text-right text-xs text-gray-500">
                                                            {percentage}% 달성
                                                        </p>
                                                    </div>
                                                </div>
                                            </Link>
                                        );
                                    })}
                                </div>
                            )}
                        </>
                    )}

                    <Link
                        href="/wishlist"
                        className="mt-8 block rounded-xl bg-[#328c82] py-4 text-center font-semibold text-white"
                    >
                        새로운 공동 선물 만들기 🎁
                    </Link>
                </div>
            </div>
        </main>
    );
}