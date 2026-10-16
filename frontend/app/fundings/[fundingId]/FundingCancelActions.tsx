
"use client";

import { useEffect, useState } from "react";
import FundingCancelButton from "./FundingCancelButton";

type Props = {
    fundingId: number;
    expiredAt: string;
    status: "OPEN" | "COMPLETED" | "EXPIRED" | "CANCELLED";
};

export default function FundingCancelActions({
                                                 fundingId,
                                                 expiredAt,
                                                 status,
                                             }: Props) {
    const [canCancel, setCanCancel] = useState(false);

    useEffect(() => {
        if (status !== "OPEN") return;

        const deadlineString = /(?:Z|[+-]\d{2}:\d{2})$/i.test(expiredAt)
            ? expiredAt
            : `${expiredAt}+09:00`;

        const deadline = new Date(deadlineString).getTime();

        function checkDeadline() {
            setCanCancel(Date.now() < deadline);
        }

        checkDeadline();

        const interval = window.setInterval(checkDeadline, 1000);

        return () => window.clearInterval(interval);
    }, [expiredAt, status]);

    if (status !== "OPEN" || !canCancel) return null;

    return <FundingCancelButton fundingId={fundingId} />;
}
