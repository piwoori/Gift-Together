"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
    getWishlist,
    type WishlistItem,
} from "@/lib/wishlist";

export default function WishlistContent() {
    const [items, setItems] = useState<WishlistItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
        let active = true;

        async function loadWishlist() {
            try {
                const data = await getWishlist();

                if (active) {
                    setItems(data);
                }
            } catch {
                if (active) {
                    setError(true);
                }
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        }

        loadWishlist();

        return () => {
            active = false;
        };
    }, []);

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link href="/" className="text-xl text-gray-600">
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        나의 위시리스트
                    </h1>
                </header>

                <section className="px-6 py-8">
                    <p className="text-sm text-gray-500">
                        받고 싶은 선물을 선택하고
                        <br />
                        친구들과 함께 선물받아 보세요.
                    </p>

                    {loading ? (
                        <p className="mt-8 text-sm text-gray-500">
                            위시리스트를 불러오는 중...
                        </p>
                    ) : error ? (
                        <p className="mt-8 rounded-xl bg-red-50 p-4 text-sm text-red-600">
                            위시리스트를 불러오지 못했습니다.
                            Spring Boot 서버가 실행 중인지 확인해 주세요.
                        </p>
                    ) : items.length === 0 ? (
                        <p className="mt-8 text-sm text-gray-500">
                            위시리스트에 등록된 상품이 없습니다.
                        </p>
                    ) : (
                        <div className="mt-8 space-y-5">
                            {items.map((item) => (
                                <div
                                    key={item.wishlistItemId}
                                    className="rounded-2xl border border-gray-100 p-5 shadow-sm"
                                >
                                    <div className="flex h-40 items-center justify-center rounded-xl bg-[#e9f7f4] text-5xl">
                                        🎧
                                    </div>

                                    <h2 className="mt-5 text-lg font-bold text-gray-900">
                                        {item.productName}
                                    </h2>

                                    <p className="mt-2 text-xl font-bold text-[#328c82]">
                                        {item.price.toLocaleString("ko-KR")}원
                                    </p>

                                    <Link
                                        href={`/fundings/new?productId=${item.productId}`}
                                        className="mt-6 block w-full rounded-xl bg-[#328c82] py-4 text-center font-semibold text-white"
                                    >
                                        이 상품으로 펀딩 만들기
                                    </Link>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            </div>
        </main>
    );
}