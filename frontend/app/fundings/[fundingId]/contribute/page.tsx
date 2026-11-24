
"use client";

import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/app/components/AuthProvider";
import { getCurrentUserId } from "@/lib/auth";
import {
    contributionStorageKey,
    prepareContributionAttempt,
    readContributionAttempt,
    sendContributionAttempt,
    type ContributionAttempt,
} from "@/lib/contribution-attempt";
import { isFundingBeforeDeadline } from "@/lib/deadline";

type FundingStatus = "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";

type FundingInfo = {
    fundingId: number;
    status: FundingStatus;
    expiredAt: string;
    remainingAmount: number;
};

export default function ContributePage() {
    const { fundingId } = useParams<{ fundingId: string }>();
    const { userId, isReady, refreshAuth } = useAuth();

    useEffect(() => {
        window.addEventListener("storage", refreshAuth);
        return () => window.removeEventListener("storage", refreshAuth);
    }, [refreshAuth]);

    return <ContributionForm key={`${userId}:${fundingId}`}
                             fundingId={fundingId} userId={userId} authReady={isReady} />;
}

const unknownMessage = "참여 결과를 아직 확인할 수 없습니다. 같은 요청으로 다시 확인해 주세요.";

function ContributionForm({ fundingId, userId, authReady }: {
    fundingId: string;
    userId: number | null;
    authReady: boolean;
}) {
    const router = useRouter();
    const storageKey = userId === null ? null : contributionStorageKey(userId, fundingId);

    const [amount, setAmount] = useState("");
    const [anonymous, setAnonymous] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [attempt, setAttempt] = useState<ContributionAttempt | null>(null);
    const [storageReady, setStorageReady] = useState(false);
    const [completed, setCompleted] = useState(false);
    const [fundingRevision, setFundingRevision] = useState(0);
    const submitting = useRef(false);
    const completedRequest = useRef(false);
    const mounted = useRef(false);

    useEffect(() => {
        mounted.current = true;
        const timer = window.setTimeout(() => {
            try {
                const saved = storageKey ? readContributionAttempt(sessionStorage, storageKey) : null;
                if (saved) {
                    setAttempt(saved);
                    setAmount(String(saved.amount));
                    setAnonymous(saved.anonymous);
                    setError(unknownMessage);
                }
                setStorageReady(true);
            } catch {
                setError("저장된 참여 요청을 확인할 수 없습니다. 참여 요청을 보내지 않았습니다.");
            }
        }, 0);
        return () => {
            mounted.current = false;
            window.clearTimeout(timer);
        };
    }, [storageKey]);

    const [funding, setFunding] = useState<FundingInfo | null>(null);
    const [fundingLoading, setFundingLoading] = useState(true);
    const [deadlinePassed, setDeadlinePassed] = useState(true);

    // 1. 펀딩 정보 조회
    useEffect(() => {
        let active = true;

        async function loadFunding() {
            setFundingLoading(true);
            setFunding(null);

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
                    setError((previous) => previous || (
                        err instanceof Error ? err.message : "펀딩 정보를 불러오지 못했습니다."
                    ));
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
    }, [fundingId, fundingRevision]);

    // 2. 마감 시간 실시간 감지
    useEffect(() => {
        if (!funding || funding.status !== "OPEN") return;

        const expiredAt = funding.expiredAt;

        function checkDeadline() {
            setDeadlinePassed(
                !isFundingBeforeDeadline(expiredAt, Date.now())
            );
        }

        checkDeadline();

        const interval = window.setInterval(checkDeadline, 1000);

        return () => window.clearInterval(interval);
    }, [funding]);

    const canContribute =
        !fundingLoading &&
        funding?.status === "OPEN" &&
        !deadlinePassed;

    const ready = authReady && storageReady && !completed;
    const inputsDisabled = !ready || !canContribute || loading || attempt !== null;

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        if (!ready || submitting.current || completedRequest.current) return;

        const currentUser = getCurrentUserId();
        if (currentUser !== userId || !storageKey) {
            if (currentUser === null) {
                router.push(`/login?next=${encodeURIComponent(window.location.pathname + window.location.search)}`);
            } else {
                setError("로그인 정보가 변경되었습니다. 페이지를 새로고침해 주세요.");
            }
            return;
        }

        submitting.current = true;
        try {
            const saved = readContributionAttempt(sessionStorage, storageKey);
            const contributionAmount = Number(amount);
            if (!saved) {
                if (!canContribute || !funding ||
                    !isFundingBeforeDeadline(funding.expiredAt, Date.now())) {
                    setError("현재 참여할 수 없는 펀딩입니다.");
                    return;
                }
                if (!Number.isSafeInteger(contributionAmount) || contributionAmount < 1000) {
                    setError("최소 1,000원 이상 입력해 주세요.");
                    return;
                }
                if (contributionAmount > funding.remainingAmount) {
                    setError(`남은 금액 ${funding.remainingAmount.toLocaleString()}원을 초과할 수 없습니다.`);
                    return;
                }
            }

            const pending = saved ?? prepareContributionAttempt(
                sessionStorage, storageKey, contributionAmount, anonymous
            );
            setAttempt(pending);
            setAmount(String(pending.amount));
            setAnonymous(pending.anonymous);
            setLoading(true);
            setError("");

            const result = await sendContributionAttempt(apiFetch, fundingId, pending);
            const activeAccount = mounted.current && getCurrentUserId() === userId;

            if (result.kind === "unknown" || result.kind === "unauthorized" || result.kind === "conflict") {
                if (activeAccount) {
                    setError(result.kind === "conflict"
                        ? `${result.message} 기존 참여 내역을 확인해 주세요.`
                        : unknownMessage);
                }
                return;
            }

            // Only clear the attempt that produced this result.
            if (readContributionAttempt(sessionStorage, storageKey)?.key === pending.key) {
                sessionStorage.removeItem(storageKey);
            }
            if (!activeAccount) return;
            setAttempt(null);

            if (result.kind === "completed") {
                completedRequest.current = true;
                setCompleted(true);
                router.push(`/fundings/${fundingId}`);
                router.refresh();
                return;
            }
            if (result.kind === "rejected") {
                setError(result.message);
            } else {
                setError("모의 결제에 실패했습니다. 다시 참여할 수 있습니다.");
            }
            setFundingLoading(true);
            setFundingRevision((revision) => revision + 1);
        } catch {
            setError("참여 요청을 저장하거나 정리할 수 없습니다. 브라우저 저장소를 확인해 주세요.");
        } finally {
            submitting.current = false;
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
                                disabled={inputsDisabled}
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
                                    disabled={inputsDisabled}
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
                            disabled={inputsDisabled}
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
                        disabled={!ready || loading || (!attempt && !canContribute)}
                        className="w-full rounded-xl bg-[#606472] py-4 font-semibold text-white transition hover:bg-[#4f5360] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading
                            ? "참여 처리 중..."
                            : attempt
                                ? "같은 요청으로 결과 확인"
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
