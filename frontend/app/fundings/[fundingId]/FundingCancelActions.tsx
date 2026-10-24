"use client";

import { useEffect, useState } from "react";
import { getAccessToken } from "@/lib/auth";
import FundingCancelButton from "./FundingCancelButton";

type Props = {
    fundingId: number;
    receiverId: number;
    expiredAt: string;
    status: "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";
};

function getCurrentUserId(): number | null {
    const token = getAccessToken();

    if (!token) return null;

    try {
        const payload = JSON.parse(
            atob(
                token
                    .split(".")[1]
                    .replace(/-/g, "+")
                    .replace(/_/g, "/")
            )
        );

        const userId = Number(payload.sub);

        return Number.isSafeInteger(userId) && userId > 0
            ? userId
            : null;
    } catch {
        return null;
    }
}

export default function FundingCancelActions({
                                                 fundingId,
                                                 receiverId,
                                                 expiredAt,
                                                 status,
                                             }: Props) {
    const [canCancel, setCanCancel] = useState(false);
    const [currentUserId, setCurrentUserId] = useState<number | null>(null);

    useEffect(() => {
        const token = getAccessToken();

        if (!token) return;

        const userId = getCurrentUserId();

        // 비동기적으로 로그인 사용자 정보 반영
        const timeout = window.setTimeout(() => {
            setCurrentUserId(userId);
        }, 0);

        return () => window.clearTimeout(timeout);
    }, []);

    useEffect(() => {
        if (status !== "OPEN") return;

        const deadlineString = /(?:Z|[+-]\d{2}:\d{2})$/i.test(expiredAt)
            ? expiredAt
            : `${expiredAt}+09:00`;

        const deadline = new Date(deadlineString).getTime();

        function checkDeadline() {
            setCanCancel(Date.now() < deadline);
        }

        const interval = window.setInterval(checkDeadline, 1000);

        return () => window.clearInterval(interval);
    }, [expiredAt, status]);

    const isOwner = currentUserId === receiverId;

    if (status !== "OPEN" || !canCancel || !isOwner) {
        return null;
    }

    return <FundingCancelButton fundingId={fundingId} />;
}