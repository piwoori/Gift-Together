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
        <header className="flex items-center justify-between border-b border-gray-100 px-6 py-5">
            <Link
                href="/"
                className="text-xl font-bold text-[#328c82]"
            >
                Gift Together
            </Link>

            <nav className="flex items-center gap-3 text-sm">
                {!isReady ? null : isLoggedIn ? (
                    <>
                        <Link
                            href="/fundings"
                            className="font-medium text-gray-600 hover:text-[#328c82]"
                        >
                            내 펀딩
                        </Link>

                        <button
                            type="button"
                            onClick={handleLogout}
                            className="font-semibold text-[#328c82]"
                        >
                            로그아웃
                        </button>
                    </>
                ) : (
                    <>
                        <Link
                            href="/login"
                            className="font-medium text-gray-600 hover:text-[#328c82]"
                        >
                            로그인
                        </Link>

                        <Link
                            href="/signup"
                            className="font-semibold text-[#328c82]"
                        >
                            회원가입
                        </Link>
                    </>
                )}
            </nav>
        </header>
    );
}