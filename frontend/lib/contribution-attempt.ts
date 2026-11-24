export type ContributionAttempt = {
    key: string;
    amount: number;
    anonymous: boolean;
    simulatePaymentFailure: boolean;
};

export function contributionStorageKey(userId: number, fundingId: string) {
    return `gift_together:contribution:${userId}:${fundingId}`;
}

export function readContributionAttempt(storage: Storage, storageKey: string): ContributionAttempt | null {
    const saved = storage.getItem(storageKey);
    if (saved === null) return null;

    const attempt = JSON.parse(saved);
    if (!attempt || typeof attempt.key !== "string" ||
        !/^[A-Za-z0-9._~-]{1,128}$/.test(attempt.key) ||
        !Number.isSafeInteger(attempt.amount) || attempt.amount < 1000 ||
        typeof attempt.anonymous !== "boolean" ||
        typeof attempt.simulatePaymentFailure !== "boolean") {
        throw new Error("저장된 참여 요청을 확인할 수 없습니다.");
    }
    return attempt;
}

export function prepareContributionAttempt(
    storage: Storage, storageKey: string, amount: number, anonymous: boolean
): ContributionAttempt {
    const saved = readContributionAttempt(storage, storageKey);
    if (saved) return saved;

    const attempt = { key: crypto.randomUUID(), amount, anonymous, simulatePaymentFailure: false };
    storage.setItem(storageKey, JSON.stringify(attempt));
    return attempt;
}

type ContributionResult =
    | { kind: "completed" | "failed" | "unknown" | "unauthorized" }
    | { kind: "rejected" | "conflict"; message: string };

export async function sendContributionAttempt(
    send: (path: string, options: RequestInit) => Promise<Response>,
    fundingId: string,
    { key, amount, anonymous, simulatePaymentFailure }: ContributionAttempt
): Promise<ContributionResult> {
    try {
        const response = await send(`/api/fundings/${fundingId}/contributions`, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Idempotency-Key": key },
            body: JSON.stringify({ amount, anonymous, simulatePaymentFailure }),
            signal: AbortSignal.timeout(15_000),
        });

        if (response.status === 401) return { kind: "unauthorized" };

        if (response.status === 201) {
            const body = await response.json();
            if (body?.contributionStatus === "COMPLETED") return { kind: "completed" };
            if (body?.contributionStatus === "FAILED") return { kind: "failed" };
        }

        if ([400, 404, 409].includes(response.status)) {
            const body = await response.json();
            if (typeof body?.message === "string") {
                if (response.status === 409) {
                    // Only known funding rejections prove that it is safe to clear the saved attempt.
                    if (body.code === "CONFLICT" &&
                        (body.message === "진행 중인 펀딩이 아닙니다." ||
                            body.message === "마감된 펀딩입니다.")) {
                        return { kind: "rejected", message: body.message };
                    }
                    return { kind: "conflict", message: body.message };
                }

                if ((response.status === 400 && body.code === "BAD_REQUEST") ||
                    (response.status === 404 && ["FUNDING_NOT_FOUND", "USER_NOT_FOUND"].includes(body.code))) {
                    return { kind: "rejected", message: body.message };
                }
            }
        }
    } catch {
        return { kind: "unknown" };
    }
    return { kind: "unknown" };
}
