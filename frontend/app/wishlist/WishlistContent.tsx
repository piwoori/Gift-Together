
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
    getWishlist,
    deleteWishlistItem,
    type WishlistItem,
} from "@/lib/wishlist";

export default function WishlistContent() {
    const [items, setItems] = useState<WishlistItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);
    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [deleteError, setDeleteError] = useState("");

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

    async function handleDelete(wishlistItemId: number) {
        if (deletingId !== null) return;

        if (!window.confirm("이 상품을 위시리스트에서 삭제할까요?")) {
            return;
        }

        setDeletingId(wishlistItemId);
        setDeleteError("");

        try {
            await deleteWishlistItem(wishlistItemId);

            setItems((prev) =>
                prev.filter((item) => item.wishlistItemId !== wishlistItemId)
            );
        } catch {
            setDeleteError("상품을 삭제하지 못했습니다. 다시 시도해 주세요.");
        } finally {
            setDeletingId(null);
        }
    }

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white shadow-sm">
                <header className="flex items-center justify-between border-b border-gray-100 px-6 py-5">
                    <div className="flex items-center gap-4">
                        <Link
                            href="/"
                            aria-label="홈으로 돌아가기"
                            className="text-xl text-gray-600"
                        >
                            ←
                        </Link>

                        <h1 className="text-lg font-bold text-gray-900">
                            나의 위시리스트
                        </h1>
                    </div>

                    <Link
                        href="/products"
                        className="text-sm font-semibold text-primary"
                    >
                        + 선물 추가
                    </Link>
                </header>

                <section className="px-6 pb-12 pt-10">
                    <p className="text-sm font-semibold tracking-wide text-primary">
                        MY WISHLIST
                    </p>

                    <h2 className="mt-3 text-2xl font-bold text-gray-900">
                        받고 싶은 선물을 모아보세요
                    </h2>

                    <p className="mt-3 text-sm leading-6 text-gray-500">
                        마음에 드는 선물을 담고,
                        <br />
                        친구들과 함께 선물을 완성해 보세요.
                    </p>

                    {deleteError && (
                        <p
                            role="alert"
                            className="mt-6 rounded-xl bg-red-50 p-4 text-sm text-red-600"
                        >
                            {deleteError}
                        </p>
                    )}

                    {loading ? (
                        <div
                            role="status"
                            className="mt-10 space-y-4"
                        >
                            {[1, 2].map((item) => (
                                <div
                                    key={item}
                                    className="h-44 animate-pulse rounded-2xl bg-gray-100"
                                />
                            ))}
                            <span className="sr-only">
                위시리스트를 불러오는 중...
              </span>
                        </div>
                    ) : error ? (
                        <div className="mt-10 rounded-2xl border border-red-100 bg-red-50 p-6 text-center">
                            <p className="font-semibold text-gray-900">
                                위시리스트를 불러오지 못했어요.
                            </p>
                            <p className="mt-2 text-sm text-gray-500">
                                잠시 후 다시 시도해 주세요.
                            </p>
                            <button
                                type="button"
                                onClick={() => window.location.reload()}
                                className="mt-5 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-white"
                            >
                                다시 시도하기
                            </button>
                        </div>
                    ) : items.length === 0 ? (
                        <div className="mt-10 rounded-3xl border border-gray-100 bg-surface-muted px-6 py-12 text-center">
                            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-2xl bg-white text-4xl shadow-sm">
                                🎁
                            </div>

                            <h3 className="mt-6 text-lg font-bold text-gray-900">
                                아직 담아둔 선물이 없어요
                            </h3>

                            <p className="mt-3 text-sm leading-6 text-gray-500">
                                받고 싶은 선물을 찾아
                                <br />
                                위시리스트에 담아보세요.
                            </p>

                            <Link
                                href="/products"
                                className="mt-7 block rounded-xl bg-primary py-4 text-center font-semibold text-white transition hover:bg-primary-hover"
                            >
                                선물 둘러보기
                            </Link>
                        </div>
                    ) : (
                        <>
                            <div className="mt-10 flex items-center justify-between">
                                <h3 className="text-base font-bold text-gray-900">
                                    담아둔 선물
                                </h3>

                                <span className="text-sm text-gray-400">
                  {items.length}개
                </span>
                            </div>

                            <div className="mt-5 space-y-4">
                                {items.map((item) => (
                                    <article
                                        key={item.wishlistItemId}
                                        className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm"
                                    >
                                        <div className="flex items-center gap-4">
                                            <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-surface-muted text-4xl">
                                                🎁
                                            </div>

                                            <div className="min-w-0 flex-1">
                                                <p className="text-xs font-medium text-gray-400">
                                                    WISH ITEM
                                                </p>

                                                <h4 className="mt-2 break-words text-base font-bold text-gray-900">
                                                    {item.productName}
                                                </h4>

                                                <p className="mt-3 text-lg font-bold text-primary">
                                                    {item.price.toLocaleString("ko-KR")}원
                                                </p>
                                            </div>
                                        </div>

                                        <div className="mt-5 border-t border-gray-100 pt-5">
                                            <Link
                                                href={`/fundings/new?productId=${item.productId}`}
                                                className="block w-full rounded-xl bg-primary py-3.5 text-center text-sm font-semibold text-white transition hover:bg-primary-hover"
                                            >
                                                🎁 공동 선물 만들기
                                            </Link>

                                            <button
                                                type="button"
                                                onClick={() => handleDelete(item.wishlistItemId)}
                                                disabled={deletingId !== null}
                                                className="mt-3 w-full rounded-xl py-2 text-sm font-medium text-gray-400 transition hover:bg-gray-50 hover:text-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
                                            >
                                                {deletingId === item.wishlistItemId
                                                    ? "삭제 중..."
                                                    : "위시리스트에서 삭제"}
                                            </button>
                                        </div>
                                    </article>
                                ))}
                            </div>

                            <Link
                                href="/products"
                                className="mt-6 block rounded-2xl border border-dashed border-gray-300 py-5 text-center text-sm font-semibold text-gray-500 transition hover:bg-gray-50"
                            >
                                + 다른 선물 더 둘러보기
                            </Link>
                        </>
                    )}
                </section>
            </div>
        </main>
    );
}
