"use client";

import Link from "next/link";
import { useAuth } from "./AuthProvider";

export default function HomeActions() {
    const { isLoggedIn, isReady } = useAuth();

    if (!isReady) {
        return <div className="mt-10 h-32" />;
    }

    if (!isLoggedIn) {
        return (
            <div className="mt-10 space-y-3">
                <Link
                    href="/login?next=/wishlist"
                    className="block rounded-2xl bg-[#328c82] py-4 text-center font-semibold text-white transition hover:bg-[#28776e]"
                >
                    로그인하고 시작하기
                </Link>

                <Link
                    href="/signup"
                    className="block rounded-2xl border border-[#328c82] py-4 text-center font-semibold text-[#328c82] transition hover:bg-[#e9f7f4]"
                >
                    회원가입
                </Link>

                <p className="pt-1 text-center text-xs text-gray-400">
                    로그인 후 친구들과 함께 선물을 모아보세요.
                </p>
            </div>
        );
    }

    return (
        <div className="mt-10 space-y-3">
            <Link
                href="/wishlist"
                className="block rounded-2xl bg-[#328c82] py-4 text-center font-semibold text-white transition hover:bg-[#28776e]"
            >
                🎁 공동 선물 시작하기
            </Link>

            <Link
                href="/fundings"
                className="block rounded-2xl border border-[#328c82] py-4 text-center font-semibold text-[#328c82] transition hover:bg-[#e9f7f4]"
            >
                내 펀딩 목록 보기
            </Link>

            <Link
                href="/wallet"
                className="block py-3 text-center text-sm font-medium text-gray-500 transition hover:text-[#328c82]"
            >
                선물 지갑 확인하기
            </Link>

            <p className="text-center text-xs text-gray-400">
                위시리스트에서 원하는 선물을 선택해 보세요.
            </p>
        </div>
    );
}