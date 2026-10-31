import { apiFetch } from "@/lib/api";

export type FundingCreateRequest = {
    productId: number;
    expiredAt: string;
    message: string;
};

export type FundingCreateResponse = {
    fundingId: number;
};

export async function createFunding(
    request: FundingCreateRequest
): Promise<FundingCreateResponse> {
    const response = await apiFetch("/api/fundings", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
    });

    if (!response.ok) {
        throw new Error("펀딩 생성에 실패했습니다.");
    }

    return response.json();
}

export type FundingStatus =
    | "OPEN"
    | "COMPLETED"
    | "EXPIRED"
    | "CANCELLED";

export type Funding = {
    fundingId: number;
    productName: string;
    productImageUrl: string;
    targetAmount: number;
    currentAmount: number;
    status: FundingStatus;
};

export async function getMyFundings(
    signal?: AbortSignal
): Promise<Funding[]> {
    const response = await apiFetch("/api/fundings/my", {
        method: "GET",
        cache: "no-store",
        signal,
    });

    if (!response.ok) {
        throw new Error("펀딩 목록 조회에 실패했습니다.");
    }

    return response.json();
}