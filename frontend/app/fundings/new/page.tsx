
"use client";

import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { useSearchParams, useRouter } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { getProducts, type Product } from "@/lib/product";

function FundingCreateForm() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const productId = Number(searchParams.get("productId"));

    const [expiredAt, setExpiredAt] = useState("");
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [product, setProduct] = useState<Product | null>(null);
    const [productLoading, setProductLoading] = useState(true);
    const [productError, setProductError] = useState("");

    useEffect(() => {
        let active = true;

        async function loadProduct() {
            setProduct(null);
            setProductError("");
            setProductLoading(true);

            try {
                if (!Number.isSafeInteger(productId) || productId <= 0) {
                    throw new Error("올바르지 않은 상품입니다.");
                }

                const products = await getProducts();
                const selectedProduct = products.find(
                    (item) => item.productId === productId
                );

                if (!selectedProduct) {
                    throw new Error("상품을 찾을 수 없습니다.");
                }

                if (active) {
                    setProduct(selectedProduct);
                }
            } catch {
                if (active) {
                    setProductError("상품 정보를 불러오지 못했습니다.");
                }
            } finally {
                if (active) {
                    setProductLoading(false);
                }
            }
        }

        loadProduct();

        return () => {
            active = false;
        };
    }, [productId]);

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        if (!Number.isSafeInteger(productId) || productId <= 0) {
            setError("상품 정보가 올바르지 않습니다.");
            return;
        }

        if (!product || product.productId !== productId) {
            setError("상품 정보를 확인할 수 없습니다.");
            return;
        }

        const deadline = new Date(expiredAt).getTime();

        if (!expiredAt || !Number.isFinite(deadline) || deadline <= Date.now()) {
            setError("현재보다 미래의 마감일을 선택해 주세요.");
            return;
        }

        setLoading(true);
        setError("");

        try {
            const response = await apiFetch("/api/fundings", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    productId,
                    expiredAt: `${expiredAt}:00`,
                    message,
                }),
            });

            if (!response.ok) {
                throw new Error("펀딩 생성에 실패했습니다.");
            }

            const data = await response.json();
            router.push(`/fundings/${data.fundingId}`);
        } catch {
            setError("펀딩을 생성하지 못했습니다. 다시 시도해 주세요.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="min-h-screen bg-[#f8f9fa]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-6">
                    <Link
                        href="/wishlist"
                        aria-label="위시리스트로 돌아가기"
                        className="text-2xl text-gray-500 transition hover:text-gray-900"
                    >
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        공동 선물 만들기
                    </h1>
                </header>

                <form onSubmit={handleSubmit} className="px-6 pb-12 pt-10">
                    <div className="mb-10">
                        <p className="text-xs font-bold tracking-wider text-primary">
                            CREATE A GIFT
                        </p>
                        <h2 className="mt-3 text-2xl font-bold text-gray-900">
                            함께 받을 선물을
                            <br />
                            준비해 볼까요?
                        </h2>
                        <p className="mt-4 text-sm leading-7 text-gray-500">
                            원하는 선물과 마감일을 정하면
                            <br />
                            친구들과 함께 선물 금액을 모을 수 있어요.
                        </p>
                    </div>

                    <section className="mb-10">
                        <div className="mb-4 flex items-center justify-between">
                            <h3 className="text-base font-bold text-gray-900">
                                선택한 선물
                            </h3>
                            <Link
                                href="/wishlist"
                                className="text-xs font-medium text-gray-500 underline underline-offset-4"
                            >
                                변경하기
                            </Link>
                        </div>

                        {productLoading ? (
                            <div className="animate-pulse rounded-2xl border border-gray-100 p-5">
                                <div className="flex gap-4">
                                    <div className="h-24 w-24 rounded-xl bg-gray-100" />
                                    <div className="flex-1 space-y-3 py-3">
                                        <div className="h-4 w-3/4 rounded bg-gray-100" />
                                        <div className="h-5 w-1/2 rounded bg-gray-100" />
                                    </div>
                                </div>
                            </div>
                        ) : productError ? (
                            <div
                                role="alert"
                                className="rounded-2xl border border-red-100 bg-red-50 p-5 text-sm text-red-600"
                            >
                                {productError}
                                <Link
                                    href="/wishlist"
                                    className="mt-3 block font-semibold underline"
                                >
                                    위시리스트로 돌아가기
                                </Link>
                            </div>
                        ) : product ? (
                            <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
                                <div className="flex items-center gap-4">
                                    <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-xl bg-gray-50 text-4xl">
                                        🎁
                                    </div>
                                    <div className="min-w-0">
                                        <p className="text-xs font-medium text-gray-400">
                                            GIFT ITEM
                                        </p>
                                        <h4 className="mt-2 break-words text-base font-bold text-gray-900">
                                            {product.name}
                                        </h4>
                                        <p className="mt-2 text-xl font-bold text-primary">
                                            {product.price.toLocaleString("ko-KR")}원
                                        </p>
                                    </div>
                                </div>
                                <div className="mt-5 border-t border-gray-100 pt-4">
                                    <div className="flex items-center justify-between text-sm">
                                        <span className="text-gray-500">목표 금액</span>
                                        <span className="font-bold text-gray-900">
                      {product.price.toLocaleString("ko-KR")}원
                    </span>
                                    </div>
                                </div>
                            </div>
                        ) : null}
                    </section>

                    <section className="mb-8">
                        <label
                            htmlFor="expiredAt"
                            className="mb-3 block text-base font-bold text-gray-900"
                        >
                            펀딩 마감일 <span className="text-red-500">*</span>
                        </label>
                        <p className="mb-4 text-sm leading-6 text-gray-500">
                            친구들이 언제까지 참여할 수 있을지 정해 주세요.
                        </p>
                        <input
                            id="expiredAt"
                            type="datetime-local"
                            required
                            value={expiredAt}
                            onChange={(e) => {
                                setExpiredAt(e.target.value);
                                setError("");
                            }}
                            className="w-full rounded-xl border border-gray-200 bg-gray-50 px-4 py-4 text-sm text-gray-900 outline-none transition focus:border-primary focus:bg-white focus:ring-2 focus:ring-gray-100"
                        />
                        <p className="mt-3 text-xs text-gray-400">
                            현재 시각보다 미래의 날짜와 시간을 선택해 주세요.
                        </p>
                    </section>

                    <section className="mb-8">
                        <label
                            htmlFor="message"
                            className="mb-3 block text-base font-bold text-gray-900"
                        >
                            친구들에게 전할 메시지
                            <span className="ml-2 text-xs font-normal text-gray-400">
                선택
              </span>
                        </label>
                        <p className="mb-4 text-sm leading-6 text-gray-500">
                            선물을 받고 싶은 이유나 친구들에게 전할 말을 적어보세요.
                        </p>
                        <textarea
                            id="message"
                            rows={4}
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            placeholder={"이번 생일에는 이 선물을 받고 싶어요! 🎂\n조금씩 마음을 보태주세요."}
                            className="w-full resize-none rounded-xl border border-gray-200 bg-gray-50 px-4 py-4 text-sm leading-7 text-gray-900 outline-none transition placeholder:text-gray-400 focus:border-primary focus:bg-white focus:ring-2 focus:ring-gray-100"
                        />
                    </section>

                    <div className="mb-8 rounded-2xl bg-gray-50 p-5">
                        <p className="mb-3 text-sm font-bold text-gray-800">
                            🎁 공동 선물 안내
                        </p>
                        <p className="text-xs leading-6 text-gray-500">
                            친구들이 원하는 금액만큼 참여해 목표 금액을 함께
                            모을 수 있어요. 목표 금액 달성 후 선물 완료 여부와
                            마감 시 미달 금액의 처리 방식은 서비스 정책에 따라
                            결정돼요.
                        </p>
                    </div>

                    {error && (
                        <p
                            role="alert"
                            className="mb-5 rounded-xl bg-red-50 p-4 text-sm text-red-600"
                        >
                            {error}
                        </p>
                    )}

                    <button
                        type="submit"
                        disabled={loading || productLoading || !product}
                        className="w-full rounded-2xl bg-primary py-4 text-base font-bold text-white transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? "펀딩 생성 중..." : "🎁 공동 선물 만들기"}
                    </button>

                    <p className="mt-4 text-center text-xs text-gray-400">
                        펀딩 생성 후 친구들에게 링크를 공유할 수 있어요.
                    </p>
                </form>
            </div>
        </main>
    );
}

export default function FundingCreatePage() {
    return (
        <Suspense
            fallback={
                <main className="min-h-screen bg-[#f8f9fa] px-6 py-10 text-center text-sm text-gray-500">
                    화면을 불러오는 중...
                </main>
            }
        >
            <FundingCreateForm />
        </Suspense>
    );
}
