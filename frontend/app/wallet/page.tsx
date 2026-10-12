
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type Wallet = {
    balance: number;
};

export default function WalletPage() {
    const [balance, setBalance] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadWallet() {
            try {
                const response = await fetch("/api/wallet", {
                    headers: {
                        "X-USER-ID": "1",
                    },
                    cache: "no-store",
                });

                if (!response.ok) {
                    throw new Error("지갑 조회 실패");
                }

                const data: Wallet = await response.json();
                setBalance(data.balance);
            } catch {
                setError("지갑 정보를 불러오지 못했습니다.");
            } finally {
                setLoading(false);
            }
        }

        loadWallet();
    }, []);

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link href="/" className="text-xl text-gray-600">
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        나의 선물 지갑
                    </h1>
                </header>

                <section className="space-y-7 px-6 py-8">
                    <div className="rounded-2xl bg-[#e9f7f4] p-6">
                        <p className="text-sm font-medium text-[#328c82]">
                            Gift Together Wallet
                        </p>

                        <p className="mt-5 text-sm text-gray-600">
                            사용 가능한 모의 잔액
                        </p>

                        {loading ? (
                            <p className="mt-2 text-lg text-gray-500">
                                불러오는 중...
                            </p>
                        ) : error ? (
                            <p role="alert" className="mt-2 text-sm text-red-600">
                                {error}
                            </p>
                        ) : (
                            <p className="mt-2 text-3xl font-bold text-[#328c82]">
                                {balance?.toLocaleString()}원
                            </p>
                        )}
                    </div>

                    <div className="rounded-xl bg-gray-50 p-5 text-sm leading-7 text-gray-600">
                        공동 선물 펀딩이 목표 금액을 달성하지 못한 채
                        마감되면, 모인 금액이 선물 받는 사람의
                        모의 지갑에 적립됩니다.
                    </div>

                    <p className="text-xs leading-6 text-gray-400">
                        현재는 포트폴리오 시연용 서비스로,
                        실제 카카오머니 충전이나 현금 출금은 지원하지 않습니다.
                    </p>

                    <Link
                        href="/wishlist"
                        className="block rounded-xl bg-[#328c82] py-4 text-center font-semibold text-white"
                    >
                        위시리스트로 돌아가기
                    </Link>
                </section>
            </div>
        </main>
    );
}
