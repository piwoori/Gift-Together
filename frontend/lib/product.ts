import { apiFetch } from "@/lib/api";

export type Product = {
    productId: number;
    name: string;
    price: number;
    imageUrl: string | null;
};

export async function getProducts(): Promise<Product[]> {
    const response = await apiFetch("/api/products", {
        method: "GET",
        cache: "no-store",
    });

    if (!response.ok) {
        throw new Error("상품 목록을 불러오지 못했습니다.");
    }

    return response.json();
}