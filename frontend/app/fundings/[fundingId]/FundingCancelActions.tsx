"use client";

import { useEffect, useState } from "react";
import { getCurrentUserId } from "@/lib/auth";
import FundingCancelButton from "./FundingCancelButton";
import { isFundingBeforeDeadline } from "@/lib/deadline";

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

        function checkDeadline() {
            setCanCancel(
                isFundingBeforeDeadline(expiredAt, Date.now())
            );
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