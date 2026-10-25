"use client";

import { useEffect, useState } from "react";
import { getCurrentUserId } from "@/lib/auth";
import FundingCancelButton from "./FundingCancelButton";

type Props = {
    fundingId: number;
    receiverId: number;
    expiredAt: string;
    status: "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";
};

export default function FundingCancelActions({
                                                 fundingId,
                                                 receiverId,
                                                 expiredAt,
                                                 status,
                                             }: Props) {
    const [canCancel, setCanCancel] = useState(false);
    const [currentUserId, setCurrentUserId] = useState<number | null>(null);

    useEffect(() => {
        const timeout = window.setTimeout(() => {
            setCurrentUserId(getCurrentUserId());
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