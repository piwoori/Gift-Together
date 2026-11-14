
"use client";

import Link from "next/link";
import Image from "next/image";
import { useEffect, useState } from "react";
import { getProducts, type Product } from "@/lib/product";
import { addWishlistItem, getWishlist } from "@/lib/wishlist";

export default function ProductsPage() {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");
    const [addingProductId, setAddingProductId] = useState<number | null>(null);
    const [message, setMessage] = useState("");
    const [messageProductId, setMessageProductId] = useState<number | null>(null);
    const [wishlistedProductIds, setWishlistedProductIds] = useState<number[]>([]);
    const [failedImageIds, setFailedImageIds] = useState<number[]>([]);

    useEffect(() => {
        let active = true;

        async function loadProducts() {
            try {
                const [productData, wishlistData] = await Promise.all([
                    getProducts(),
                    getWishlist(),
                ]);

                if (!active) return;

                setProducts(productData);
                setWishlistedProductIds(
                    wishlistData.map((item) => item.productId)
                );
            } catch (err) {
                if (active) {
                    setError(
                        err instanceof Error
                            ? err.message
                            : "상품 목록을 불러오지 못했습니다."
                    );
                }
            } finally {
                if (active) setLoading(false);
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

            setMessage("위시리스트에 상품을 담았어요!");
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

    const filteredProducts = products.filter((product) =>
        product.name.toLowerCase().includes(search.trim().toLowerCase())
    );

    return (
        <main className="min-h-screen bg-surface-muted">
            <div className="mx-auto min-h-screen max-w-md bg-white shadow-sm">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link
                        href="/"
                        aria-label="홈으로 돌아가기"
                        className="text-xl text-gray-600"
                    >
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        선물 둘러보기
                    </h1>
                    <Link
                        href="/wishlist"
                        className="ml-auto text-sm font-semibold text-accent"
                    >
                        내 위시리스트
                    </Link>
                </header>

                <section className="px-5 pb-12 pt-8">
                    <p className="text-xs font-semibold text-accent">
                        GIFT COLLECTION
                    </p>
                    <h2 className="mt-2 text-2xl font-bold text-gray-900">
                        어떤 선물을 받고 싶나요?
                    </h2>
                    <p className="mt-2 text-sm leading-6 text-gray-500">
                        마음에 드는 선물을 골라 위시리스트에 담아보세요.
                    </p>


                    <div className="relative mt-7">
                        <label htmlFor="product-search" className="sr-only">
                            상품 검색
                        </label>

                        <svg
                            aria-hidden="true"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#9296a1]"
                        >
                            <circle cx="11" cy="11" r="8"/>
                            <path d="m21 21-4.35-4.35"/>
                        </svg>

                        <input
                            id="product-search"
                            type="search"
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="찾고 싶은 선물을 검색해 보세요"
                            className="w-full rounded-2xl border border-[#e5e7eb] bg-[#f7f8fa] py-4 pl-12 pr-4 text-sm text-gray-900 outline-none transition-colors placeholder:text-[#9296a1] focus:border-[#606472] focus:bg-white focus:ring-2 focus:ring-[#606472]/10"
                        />
                    </div>

                    {loading ? (
                        <div className="mt-8 grid grid-cols-2 gap-3" aria-label="상품 불러오는 중">
                            {[1, 2, 3, 4].map((item) => (
                                <div
                                    key={item}
                                    className="animate-pulse rounded-2xl border border-gray-100 p-3"
                                >
                                    <div className="aspect-square rounded-xl bg-gray-100"/>
                                    <div className="mt-4 h-4 rounded bg-gray-100"/>
                                    <div className="mt-3 h-4 w-2/3 rounded bg-gray-100"/>
                                    <div className="mt-5 h-10 rounded-xl bg-gray-100"/>
                                </div>
                            ))}
                        </div>
                    ) : error ? (
                        <p
                            role="alert"
                            className="mt-8 rounded-2xl bg-red-50 p-5 text-sm text-red-600"
                        >
                            {error}
                        </p>
                    ) : products.length === 0 ? (
                        <div className="mt-8 rounded-2xl border border-gray-100 px-5 py-12 text-center">
                            <p className="text-4xl">🎁</p>
                            <p className="mt-4 text-sm text-gray-500">
                                아직 등록된 상품이 없어요.
                            </p>
                        </div>
                    ) : (
                        <>


                            <div className="mt-8 flex items-center gap-2">
                                <h3 className="text-base font-bold text-[#20232b]">
                                    전체 상품
                                </h3>
                                <span
                                    className="rounded-full bg-[#f0f1f4] px-2.5 py-1 text-xs font-semibold text-[#606472]">
                                    {filteredProducts.length}개
                                </span>
                            </div>

                            {filteredProducts.length === 0 ? (
                                <div className="mt-6 rounded-2xl bg-gray-50 px-5 py-12 text-center">
                                    <p className="text-3xl">🔎</p>
                                    <p className="mt-3 text-sm text-gray-500">
                                        검색 결과가 없어요.
                                    </p>
                                </div>
                            ) : (
                                <div className="mt-5 grid grid-cols-2 gap-4">
                                    {filteredProducts.map((product) => {
                                        const isWishlisted = wishlistedProductIds.includes(
                                            product.productId
                                        );
                                        const imageUrl = product.imageUrl;
                                        const showImage =
                                            Boolean(imageUrl) &&
                                            !failedImageIds.includes(product.productId);

                                        return (
                                            <article
                                                key={product.productId}
                                                className="flex min-w-0 flex-col rounded-2xl border border-[#e9eaee] bg-white p-3 transition-shadow hover:shadow-md"
                                            >
                                                <div
                                                    className="relative flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-[#f7f8fa]">
                                                    {showImage && imageUrl ? (
                                                        <Image
                                                            src={imageUrl}
                                                            alt={product.name}
                                                            fill
                                                            unoptimized
                                                            sizes="(max-width: 448px) 50vw, 190px"
                                                            className="object-contain p-3"
                                                            onError={() =>
                                                                setFailedImageIds((prev) =>
                                                                    prev.includes(product.productId)
                                                                        ? prev
                                                                        : [...prev, product.productId]
                                                                )
                                                            }
                                                        />
                                                    ) : (
                                                        <span className="text-5xl" aria-hidden="true">
                              🎁
                            </span>
                                                    )}
                                                </div>

                                                <div className="flex flex-1 flex-col">
                                                    <h3 className="mt-4 line-clamp-2 min-h-10 text-sm font-semibold leading-5 text-gray-900">
                                                        {product.name}
                                                    </h3>
                                                    <p className="mt-2 text-lg font-extrabold tracking-tight text-[#606472]">
                                                        {product.price.toLocaleString("ko-KR")}원
                                                    </p>

                                                    <div className="mt-auto pt-4">
                                                        {isWishlisted ? (
                                                            <Link
                                                                href="/wishlist"
                                                                className="flex min-h-11 items-center justify-center rounded-xl bg-surface-accent px-2 text-center text-xs font-semibold text-accent"
                                                            >
                                                                ✓ 담긴 상품
                                                            </Link>
                                                        ) : (
                                                            <button
                                                                type="button"
                                                                onClick={() =>
                                                                    handleAddWishlist(product.productId)
                                                                }
                                                                disabled={addingProductId !== null}
                                                                className="min-h-11 w-full rounded-xl bg-primary px-2 text-xs font-semibold text-white transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
                                                            >
                                                                {addingProductId === product.productId
                                                                    ? "담는 중..."
                                                                    : "위시리스트 담기"}
                                                            </button>
                                                        )}
                                                    </div>
                                                </div>

                                                {messageProductId === product.productId && message && (
                                                    <p
                                                        role="status"
                                                        className="mt-3 text-xs leading-5 text-gray-600"
                                                    >
                                                        {message}
                                                    </p>
                                                )}
                                            </article>
                                        );
                                    })}
                                </div>
                            )}
                        </>
                    )}
                </section>
            </div>
        </main>
    );
}
