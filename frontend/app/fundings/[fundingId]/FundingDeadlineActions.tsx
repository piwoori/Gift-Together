
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { isFundingBeforeDeadline } from "@/lib/deadline";

type Props = {
    fundingId: number;
    expiredAt: string;
    status: "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";
};

export default function FundingDeadlineActions({
                                                   fundingId,
                                                   expiredAt,
                                                   status,
                                               }: Props) {
    const router = useRouter();
    const [deadlinePassed, setDeadlinePassed] = useState<boolean | null>(null);

    useEffect(() => {
        if (status !== "OPEN") return;

        function checkDeadline() {
            setDeadlinePassed(
                !isFundingBeforeDeadline(expiredAt, Date.now())
            );
        }

        checkDeadline();

        const interval = window.setInterval(checkDeadline, 1000);

        return () => window.clearInterval(interval);
    }, [expiredAt, status]);

    if (status !== "OPEN") return null;

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
        <Link
            href={`/fundings/${fundingId}/contribute`}
            className="block w-full rounded-xl bg-[#606471] py-4 text-center font-semibold text-white transition hover:bg-[#4f535f]"
        >
            이 선물에 참여하기 🎁
        </Link>
    );
}
