
"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

type Props = {
    fundingId: number;
};

export default function FundingCancelButton({ fundingId }: Props) {
    const router = useRouter();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleCancel() {
        const confirmed = window.confirm(
            "펀딩을 취소하시겠습니까?\n참여한 금액은 각 참여자에게 환불 처리됩니다."
        );

        if (!confirmed) return;

        setLoading(true);
        setError("");

        try {
            const response = await fetch(`/api/fundings/${fundingId}/cancel`, {
                method: "POST",
                headers: {
                    "X-USER-ID": "1",
                },
            });

            if (!response.ok) {
                const data = await response.json().catch(() => null);
                throw new Error(data?.message ?? "펀딩 취소에 실패했습니다.");
            }

            router.refresh();
        } catch (err) {
            setError(
                err instanceof Error ? err.message : "잠시 후 다시 시도해 주세요."
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="mt-4">
            <button
                type="button"
                onClick={handleCancel}
                disabled={loading}
                className="w-full rounded-xl border border-red-200 py-3 text-sm font-semibold text-red-600 disabled:opacity-50"
            >
                {loading ? "펀딩 취소 중..." : "펀딩 취소하기"}
            </button>

            {error && (
                <p role="alert" className="mt-2 text-sm text-red-600">
                    {error}
                </p>
            )}
        </div>
    );
}