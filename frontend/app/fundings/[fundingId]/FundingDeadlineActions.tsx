
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import FundingCancelButton from "./FundingCancelButton";

type Props = {
    fundingId: number;
    expiredAt: string;
    status: "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";
};

function getDeadlineTime(expiredAt: string): number {
    // 백엔드 LocalDateTime은 한국 시간 기준이라고 가정
    const hasTimezone = /(?:Z|[+-]\d{2}:\d{2})$/i.test(expiredAt);
    const dateString = hasTimezone
        ? expiredAt
        : `${expiredAt}+09:00`;

    return new Date(dateString).getTime();
}

export default function FundingDeadlineActions({
                                                   fundingId,
                                                   expiredAt,
                                                   status,
                                               }: Props) {
    const router = useRouter();
    const [deadlinePassed, setDeadlinePassed] = useState<boolean | null>(null);

    useEffect(() => {
        if (status !== "OPEN") return;

        const deadline = getDeadlineTime(expiredAt);

        function checkDeadline() {
            const passed = Date.now() >= deadline;
            setDeadlinePassed(passed);
        }

        checkDeadline();

        const interval = window.setInterval(checkDeadline, 1000);

        return () => window.clearInterval(interval);
    }, [expiredAt, status]);

    // 완료/취소/만료된 펀딩에는 참여 버튼을 표시하지 않음
    if (status !== "OPEN") return null;

    // 브라우저에서 마감 시각을 확인하기 전에는 버튼을 숨김
    if (deadlinePassed === null) return null;

    if (deadlinePassed) {
        return (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5">
                <h2 className="font-bold text-gray-900">
                    ⏳ 펀딩 마감 처리 중
                </h2>

                <p className="mt-2 text-sm leading-6 text-gray-600">
                    참여 가능 시간이 종료되었어요.
                    마감 처리 결과를 확인해 주세요.
                </p>

                <button
                    type="button"
                    onClick={() => router.refresh()}
                    className="mt-4 rounded-xl bg-white px-4 py-2 text-sm font-semibold text-gray-700"
                >
                    마감 결과 새로고침
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-3">
            <Link
                href={`/fundings/${fundingId}/contribute`}
                className="block w-full rounded-xl bg-[#328c82] py-4 text-center font-semibold text-white"
            >
                이 선물에 참여하기 🎁
            </Link>

            <FundingCancelButton fundingId={fundingId} />
        </div>
    );
}
