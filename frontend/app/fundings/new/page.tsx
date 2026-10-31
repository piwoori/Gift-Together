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

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        // 1. 상품 ID 유효성 검증
        if (!Number.isSafeInteger(productId) || productId <= 0) {
            setError("상품 정보가 올바르지 않습니다.");
            return;
        }

        // 2. 실제 조회된 상품과 일치하는지 검증
        if (!product || product.productId !== productId) {
            setError("상품 정보를 확인할 수 없습니다.");
            return;
        }

        // 3. 마감일 검증
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

    useEffect(() => {
        let active = true;

        async function loadProduct() {
            if (active) {
                setProduct(null);
                setProductError("");
                setProductLoading(true);
            }

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

    return (
        <main className="min-h-screen bg-[#f8faf9]">
            <div className="mx-auto min-h-screen max-w-md bg-white">
                <header className="flex items-center gap-4 border-b border-gray-100 px-6 py-5">
                    <Link href="/wishlist" className="text-xl text-gray-600">
                        ←
                    </Link>
                    <h1 className="text-lg font-bold text-gray-900">
                        공동 선물 펀딩 만들기
                    </h1>
                </header>

                <form onSubmit={handleSubmit} className="space-y-7 px-6 py-8">
                    {productLoading ? (
                        <p className="text-sm text-gray-500">
                            상품 정보를 불러오는 중...
                        </p>
                    ) : productError ? (
                        <p className="rounded-xl bg-red-50 p-4 text-sm text-red-600">
                            {productError}
                        </p>
                    ) : product ? (
                        <div className="rounded-2xl border border-gray-100 p-5 shadow-sm">
                            <p className="mb-4 text-sm font-semibold text-gray-500">
                                선택한 선물 🎁
                            </p>

                            <div className="flex items-center gap-4">
                                <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-xl bg-[#e9f7f4] text-4xl">
                                    🎧
                                </div>

                                <div>
                                    <h2 className="font-bold text-gray-900">
                                        {product.name}
                                    </h2>

                                    <p className="mt-2 text-lg font-bold text-[#328c82]">
                                        {product.price.toLocaleString("ko-KR")}원
                                    </p>
                                </div>
                            </div>
                        </div>
                    ) : null}
                    <div>
                        <label
                            htmlFor="expiredAt"
                            className="mb-2 block text-sm font-semibold text-gray-700"
                        >
                            펀딩 마감일
                        </label>
                        <input
                            id="expiredAt"
                            type="datetime-local"
                            required
                            value={expiredAt}
                            onChange={(e) => setExpiredAt(e.target.value)}
                            className="w-full rounded-xl border border-gray-200 p-4 text-gray-900"
                        />
                    </div>

                    <div>
                        <label
                            htmlFor="message"
                            className="mb-2 block text-sm font-semibold text-gray-700"
                        >
                            친구들에게 전할 메시지
                        </label>
                        <textarea
                            id="message"
                            rows={4}
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            placeholder="생일 선물로 갖고 싶어요 🎂"
                            className="w-full resize-none rounded-xl border border-gray-200 p-4 text-gray-900"
                        />
                    </div>

                    <div className="rounded-xl bg-[#e9f7f4] p-4 text-sm leading-6 text-gray-700">
                        목표 금액을 달성하면 상품 선물이 완료됩니다.
                        <br />
                        목표 금액을 달성하지 못하면 모인 금액은
                        선물 받는 사람의 카카오머니로 전달됩니다.
                    </div>

                    {error && (
                        <p className="text-sm text-red-600">{error}</p>
                    )}

                    <button
                        type="submit"
                        disabled={loading || productLoading || !product}
                        className="w-full rounded-xl bg-[#328c82] py-4 font-semibold text-white disabled:opacity-50"
                    >
                        {loading ? "펀딩 생성 중..." : "펀딩 생성하기"}
                    </button>
                </form>
            </div>
        </main>
    );
}

export default function FundingCreatePage() {
    return (
        <Suspense fallback={<p className="p-6">화면을 불러오는 중...</p>}>
            <FundingCreateForm />
        </Suspense>
    );
}