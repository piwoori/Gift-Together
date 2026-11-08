
"use client";

import Link from "next/link";
import { useAuth } from "./AuthProvider";

const primaryButton =
    "flex min-h-14 items-center justify-center rounded-2xl bg-[#374151] px-5 py-4 text-center text-sm font-semibold text-white transition-colors hover:bg-[#1f2937] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#374151]";

const secondaryButton =
    "flex min-h-12 items-center justify-center rounded-2xl border border-gray-200 bg-white px-4 py-3 text-center text-sm font-semibold text-[#374151] transition-colors hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#374151]";

export default function HomeActions() {
    const { isLoggedIn, isReady } = useAuth();

    if (!isReady) {
        return (
            <div
                className="h-28 animate-pulse rounded-2xl bg-gray-100"
                aria-label="사용자 정보 확인 중"
            />
        );
    }

    if (!isLoggedIn) {
        return (
            <div className="space-y-3">
                <Link href="/login?next=/wishlist" className={primaryButton}>
                    🎁 로그인하고 선물 만들기
                </Link>

                <Link href="/signup" className={secondaryButton}>
                    처음이신가요? 회원가입
                </Link>
            </div>
        );
    }

    return (
        <div className="space-y-3">
            <Link href="/wishlist" className={primaryButton}>
                🎁 공동 선물 시작하기
            </Link>

            <div className="grid grid-cols-2 gap-3">
                <Link href="/fundings" className={secondaryButton}>
                    내 펀딩
                </Link>

                <Link href="/wallet" className={secondaryButton}>
                    선물 지갑
                </Link>
            </div>
        </div>
    );
}
