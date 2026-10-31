"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getProducts, type Product } from "@/lib/product";
import {
    addWishlistItem,
    getWishlist,
} from "@/lib/wishlist";

export default function ProductsPage() {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [addingProductId, setAddingProductId] = useState<number | null>(null);
    const [message, setMessage] = useState("");
    const [messageProductId, setMessageProductId] = useState<number | null>(null);
    const [wishlistedProductIds, setWishlistedProductIds] = useState<number[]>([]);

    useEffect(() => {
        let active = true;

        async function loadProducts() {
            try {
                const [productData, wishlistData] = await Promise.all([
                    getProducts(),
                    getWishlist(),
                ]);

                if (active) {
                    setProducts(productData);
                    setWishlistedProductIds(
                        wishlistData.map((item) => item.productId)
                    );
                }
            } catch (err) {
                if (active) {
                    setError(
                        err instanceof Error
                            ? err.message
                            : "상품 목록을 불러오지 못했습니다."
                    );
                }
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        }

        loadProducts();

        return () => {
            active = false;
        };
    }, []);

    async function handleAddWishlist(productId: number) {
        if (addingProductId !== null) return;

        setAddingProductId(productId);
        setMessage("");
        setMessageProductId(productId);

        try {
            await addWishlistItem({ productId });

            setWishlistedProductIds((prev) =>
                prev.includes(productId) ? prev : [...prev, productId]
            );

            setMessage("위시리스트에 상품을 담았어요! 🎁");
        } catch (err) {
            setMessage(
                err instanceof Error
                    ? err.message
                    : "상품을 추가하지 못했습니다."
            );
        } finally {
            setAddingProductId(null);
        }
    }

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white shadow-sm">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link href="/" className="text-xl text-gray-600">
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        선물 둘러보기
                    </h1>
                </header>

                <section className="px-6 py-8">
                    <h2 className="text-xl font-bold text-gray-900">
                        어떤 선물을 받고 싶나요? 🎁
                    </h2>

                    <p className="mt-2 text-sm leading-6 text-gray-500">
                        원하는 상품을 찾아 위시리스트에 담아보세요.
                    </p>

                    {loading ? (
                        <p className="mt-10 text-center text-sm text-gray-500">
                            상품을 불러오는 중...
                        </p>
                    ) : error ? (
                        <p
                            role="alert"
                            className="mt-10 rounded-xl bg-red-50 p-4 text-sm text-red-600"
                        >
                            {error}
                        </p>
                    ) : products.length === 0 ? (
                        <div className="mt-10 rounded-2xl border border-gray-100 p-8 text-center">
                            <p className="text-3xl">🎁</p>
                            <p className="mt-3 text-sm text-gray-500">
                                등록된 상품이 없습니다.
                            </p>
                        </div>
                    ) : (
                        <div className="mt-8 space-y-4">
                            {products.map((product) => (
                                <div
                                    key={product.productId}
                                    className="rounded-2xl border border-gray-100 p-5 shadow-sm"
                                >
                                    <div className="flex h-36 items-center justify-center rounded-xl bg-[#e9f7f4] text-5xl">
                                        🎧
                                    </div>

                                    <h3 className="mt-4 text-lg font-bold text-gray-900">
                                        {product.name}
                                    </h3>

                                    <p className="mt-2 text-xl font-bold text-[#328c82]">
                                        {product.price.toLocaleString("ko-KR")}원
                                    </p>

                                    {wishlistedProductIds.includes(product.productId) ? (
                                        <Link
                                            href="/wishlist"
                                            className="mt-5 block w-full rounded-xl border border-[#328c82] bg-[#e9f7f4] py-3 text-center font-semibold text-[#328c82]"
                                        >
                                            ✓ 담긴 상품 · 위시리스트 보기
                                        </Link>
                                    ) : (
                                        <button
                                            type="button"
                                            onClick={() => handleAddWishlist(product.productId)}
                                            disabled={addingProductId !== null}
                                            className="mt-5 w-full rounded-xl bg-[#328c82] py-3 font-semibold text-white transition hover:bg-[#28776e] disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            {addingProductId === product.productId
                                                ? "담는 중..."
                                                : "🎁 위시리스트에 담기"}
                                        </button>
                                    )}

                                    {messageProductId === product.productId && message && (
                                        <p role="status" className="mt-3 text-center text-sm text-gray-600">
                                            {message}
                                        </p>
                                    )}

                                    {messageProductId === product.productId && message && (
                                        <p
                                            role="status"
                                            className="mt-3 text-center text-sm text-gray-600"
                                        >
                                            {message}
                                        </p>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            </div>
        </main>
    );
}