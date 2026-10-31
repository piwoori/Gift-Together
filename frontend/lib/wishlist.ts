import { apiFetch } from "@/lib/api";

export type WishlistAddRequest = {
    productId: number;
};

export async function addWishlistItem(
    request: WishlistAddRequest
): Promise<void> {
    const response = await apiFetch("/api/wishlist", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
    });

    if (response.status === 409) {
        throw new Error("이미 위시리스트에 담긴 상품입니다.");
    }

    if (!response.ok) {
        throw new Error("위시리스트에 상품을 추가하지 못했습니다.");
    }
}