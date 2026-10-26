"use client";

import { apiFetch } from "@/lib/api";
import { getCurrentUserId } from "@/lib/auth";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

type Contribution = {
    contributionId: number;
    contributorId: number | null;
    contributorNickname: string;
    amount: number;
    status: string;
    anonymous: boolean;
};

type Props = {
    fundingId: number;
    fundingStatus: string;
    expiredAt: string;
};

function getContributionStatusLabel(status: string) {
    switch (status) {
        case "COMPLETED":
            return "참여 완료";
        case "CANCELLED":
            return "참여 취소";
        case "REFUNDED":
            return "환불 완료";
        case "FAILED":
            return "결제 실패";
        case "PENDING":
            return "결제 대기";
        default:
            return status;
    }
}

export default function ContributionHistory({
                                                fundingId,
                                                fundingStatus,
                                                expiredAt,
                                            }: Props) {
    const router = useRouter();

    const [currentUserId, setCurrentUserId] = useState<number | null>(null);
    const [contributions, setContributions] = useState<Contribution[]>([]);
    const [loading, setLoading] = useState(true);
    const [cancellingId, setCancellingId] = useState<number | null>(null);
    const [error, setError] = useState("");
    const [now, setNow] = useState<number | null>(null);

    useEffect(() => {
        const timeout = window.setTimeout(() => {
            setCurrentUserId(getCurrentUserId());
        }, 0);

        return () => window.clearTimeout(timeout);
    }, []);

    useEffect(() => {
        function updateNow() {
            setNow(Date.now());
        }

        updateNow();

        const interval = window.setInterval(updateNow, 1000);

        return () => window.clearInterval(interval);
    }, []);

    useEffect(() => {
        let active = true;

        async function loadContributions() {
            setLoading(true);
            setError("");

            try {
                const response = await fetch(
                    `/api/fundings/${fundingId}/contributions`,
                    { cache: "no-store" }
                );

                if (!response.ok) {
                    throw new Error("참여 내역 조회 실패");
                }

                const data: Contribution[] = await response.json();

                if (active) {
                    setContributions(data);
                }
            } catch {
                if (active) {
                    setError("참여 내역을 불러오지 못했습니다.");
                }
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        }

        loadContributions();

        return () => {
            active = false;
        };
    }, [fundingId, fundingStatus]);

    async function handleCancel(contributionId: number) {
        if (!window.confirm("이 선물 참여를 취소하시겠습니까?")) {
            return;
        }

        setCancellingId(contributionId);
        setError("");

        try {
            const response = await apiFetch(
                `/api/fundings/${fundingId}/contributions/${contributionId}/cancel`,
                {
                    method: "POST",
                }
            );

            if (!response.ok) {
                throw new Error("참여 취소 실패");
            }

            setContributions((previous) =>
                previous.map((item) =>
                    item.contributionId === contributionId
                        ? { ...item, status: "CANCELLED" }
                        : item
                )
            );

            router.refresh();
        } catch {
            setError("참여를 취소하지 못했습니다.");
        } finally {
            setCancellingId(null);
        }
    }

    return (
        <section className="mt-8 rounded-2xl border border-gray-100 bg-white p-5">
            <div className="mb-5 flex items-center justify-between">
                <h2 className="text-base font-bold text-gray-900">
                    🎁 선물 참여 내역
                </h2>

                <span className="text-sm text-gray-500">
          {contributions.length}건
        </span>
            </div>

            {loading && (
                <p className="text-sm text-gray-500">
                    참여 내역을 불러오는 중...
                </p>
            )}

            {!loading && contributions.length === 0 && (
                <p className="text-sm text-gray-500">
                    아직 참여한 사람이 없어요.
                </p>
            )}

            <div className="space-y-4">
                {contributions.map((item) => {
                    const isMine =
                        currentUserId !== null &&
                        item.contributorId !== null &&
                        item.contributorId === currentUserId;

                    const isDeadlinePassed =
                        now === null ||
                        !Number.isFinite(new Date(expiredAt).getTime()) ||
                        new Date(expiredAt).getTime() <= now;

                    const canCancel =
                        isMine &&
                        item.status === "COMPLETED" &&
                        fundingStatus === "OPEN" &&
                        !isDeadlinePassed;

                    return (
                        <div
                            key={item.contributionId}
                            className="flex items-center justify-between border-b border-gray-100 pb-4 last:border-0"
                        >
                            <div>
                                <p className="font-semibold text-gray-900">
                                    {item.contributorNickname}
                                </p>

                                <p className="mt-1 text-sm text-gray-500">
                                    {item.amount.toLocaleString()}원 · {getContributionStatusLabel(item.status)}
                                </p>
                            </div>

                            {canCancel && (
                                <button
                                    type="button"
                                    disabled={cancellingId !== null}
                                    onClick={() => handleCancel(item.contributionId)}
                                    className="rounded-lg border border-gray-200 px-3 py-2 text-xs text-gray-600 disabled:opacity-50"
                                >
                                    {cancellingId === item.contributionId
                                        ? "취소 중..."
                                        : "참여 취소"}
                                </button>
                            )}
                        </div>
                    );
                })}
            </div>

            {error && (
                <p role="alert" className="mt-4 text-sm text-red-600">
                    {error}
                </p>
            )}
        </section>
    );
}