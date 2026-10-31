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

export type WishlistItem = {
    wishlistItemId: number;
    productId: number;
    productName: string;
    price: number;
    imageUrl: string | null;
};

export async function getWishlist(): Promise<WishlistItem[]> {
    const response = await apiFetch("/api/wishlist", {
        method: "GET",
        cache: "no-store",
    });

    if (!response.ok) {
        throw new Error("위시리스트를 불러오지 못했습니다.");
    }

    return response.json();
}

export async function deleteWishlistItem(
    wishlistItemId: number
): Promise<void> {
    const response = await apiFetch(
        `/api/wishlist/${wishlistItemId}`,
        {
            method: "DELETE",
        }
    );

    if (!response.ok) {
        throw new Error("위시리스트 상품 삭제에 실패했습니다.");
    }
}