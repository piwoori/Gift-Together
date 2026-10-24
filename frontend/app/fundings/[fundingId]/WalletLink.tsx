"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getAccessToken } from "@/lib/auth";

type Props = {
    receiverId: number;
};

export default function WalletLink({ receiverId }: Props) {
    const [isOwner, setIsOwner] = useState(false);

    useEffect(() => {
        const token = getAccessToken();
        if (!token) return;

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

            const timeout = window.setTimeout(() => {
                setIsOwner(userId === receiverId);
            }, 0);

            return () => window.clearTimeout(timeout);
        } catch {
            // 유효하지 않은 토큰이면 버튼을 표시하지 않음
        }
    }, [receiverId]);

    if (!isOwner) return null;

    return (
        <Link
            href="/wallet"
            className="mt-5 block rounded-xl bg-[#328c82] py-3 text-center font-semibold text-white"
        >
            내 선물 지갑 확인하기 →
        </Link>
    );
}