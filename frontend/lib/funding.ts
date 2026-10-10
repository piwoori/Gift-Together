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
    const response = await fetch("/api/fundings", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-USER-ID": "1",
            },
            body: JSON.stringify(request),
            cache: "no-store",
        }
    );

    if (!response.ok) {
        throw new Error("펀딩 생성에 실패했습니다.");
    }

    return response.json();
}