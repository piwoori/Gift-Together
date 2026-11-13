
"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "./AuthProvider";

export default function Header() {
    const router = useRouter();
    const { isLoggedIn, isReady, signOut } = useAuth();

    function handleLogout() {
        signOut();
        router.push("/");
        router.refresh();
    }

    return (
        <header className="flex items-center justify-between border-b border-[#eef0f2] bg-white px-6 py-5">
            <Link
                href="/"
                className="text-xl font-extrabold tracking-tight text-[#606472] transition-colors hover:text-[#4f5360]"
            >
                Gift Together
            </Link>

            <nav className="flex items-center gap-3 text-sm">
                {!isReady ? null : isLoggedIn ? (
                    <>
                        <Link
                            href="/fundings"
                            className="font-medium text-gray-600 transition-colors hover:text-[#606472]"
                        >
                            내 펀딩
                        </Link>

                        <button
                            type="button"
                            onClick={handleLogout}
                            className="rounded-lg px-2 py-2 font-semibold text-[#606472] transition-colors hover:bg-[#f0f1f4]"
                        >
                            로그아웃
                        </button>
                    </>
                ) : (
                    <>
                        <Link
                            href="/login"
                            className="font-medium text-gray-600 transition-colors hover:text-[#606472]"
                        >
                            로그인
                        </Link>

                        <Link
                            href="/signup"
                            className="rounded-lg bg-[#f0f1f4] px-3 py-2 font-semibold text-[#606472] transition-colors hover:bg-[#e5e7eb]"
                        >
                            회원가입
                        </Link>
                    </>
                )}
            </nav>
        </header>
    );
}
