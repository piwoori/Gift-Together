
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type Wallet = {
    balance: number;
};

type WalletTransaction = {
    transactionId: number;
    fundingId: number;
    amount: number;
    type: "FUNDING_EXPIRED_REWARD";
    createdAt: string;
};

export default function WalletPage() {
    const [balance, setBalance] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [transactions, setTransactions] = useState<WalletTransaction[]>([]);
    const [transactionsLoading, setTransactionsLoading] = useState(true);
    const [transactionsError, setTransactionsError] = useState("");

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

        async function loadTransactions() {
            try {
                const response = await fetch("/api/wallet/transactions", {
                    headers: {
                        "X-USER-ID": "1",
                    },
                    cache: "no-store",
                });

                if (!response.ok) {
                    throw new Error("거래 내역 조회 실패");
                }

                const data: WalletTransaction[] = await response.json();
                setTransactions(data);
            } catch {
                setTransactionsError("적립 내역을 불러오지 못했습니다.");
            } finally {
                setTransactionsLoading(false);
            }
        }

        loadTransactions();
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


                    <div className="rounded-2xl border border-gray-100 p-5">
                        <div className="mb-5 flex items-center justify-between">
                            <h2 className="font-bold text-gray-900">
                                🎁 선물 지갑 적립 내역
                            </h2>

                            <span className="text-sm text-gray-500">
                              {transactions.length}건
                            </span>
                        </div>

                        {transactionsLoading ? (
                            <p className="text-sm text-gray-500">
                                적립 내역을 불러오는 중...
                            </p>
                        ) : transactionsError ? (
                            <p role="alert" className="text-sm text-red-600">
                                {transactionsError}
                            </p>
                        ) : transactions.length === 0 ? (
                            <p className="text-sm text-gray-500">
                                아직 적립 내역이 없어요.
                            </p>
                        ) : (
                            <div className="space-y-4">
                                {transactions.map((transaction) => (
                                    <div
                                        key={transaction.transactionId}
                                        className="flex items-center justify-between border-b border-gray-100 pb-4 last:border-0 last:pb-0"
                                    >
                                        <div>
                                            <p className="text-sm font-semibold text-gray-900">
                                                공동 선물 펀딩 마감 적립
                                            </p>

                                            <p className="mt-1 text-xs text-gray-500">
                                                펀딩 #{transaction.fundingId} ·{" "}
                                                {transaction.createdAt.slice(0, 10)}
                                            </p>
                                        </div>

                                        <p className="font-bold text-[#328c82]">
                                            +{transaction.amount.toLocaleString()}원
                                        </p>
                                    </div>
                                ))}
                            </div>
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
