"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getCurrentUserId } from "@/lib/auth";

type Props = {
    receiverId: number;
};

export default function WalletLink({ receiverId }: Props) {
    const [isOwner, setIsOwner] = useState(false);

    useEffect(() => {
        const timeout = window.setTimeout(() => {
            setIsOwner(getCurrentUserId() === receiverId);
        }, 0);

        return () => window.clearTimeout(timeout);
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