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
        !Number.isSafeInteger(attempt.amount) || attempt.amount <= 0 ||
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
    | { kind: "completed" | "failed" | "unknown" | "unauthorized" | "not-found" | "mismatch" }
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
            const code = body?.code;
            const message = typeof body?.message === "string" ? body.message : "참여 요청이 거절되었습니다.";
            if (response.status === 409 && code === "IDEMPOTENCY_KEY_CONFLICT") {
                return { kind: "conflict", message };
            }
            if ((response.status === 409 && (code === "FUNDING_NOT_OPEN" || code === "FUNDING_DEADLINE_PASSED")) ||
                (response.status === 400 && code === "BAD_REQUEST") ||
                (response.status === 404 && (code === "FUNDING_NOT_FOUND" || code === "USER_NOT_FOUND"))) {
                return { kind: "rejected", message };
            }
        }
    } catch {
        return { kind: "unknown" };
    }
    return { kind: "unknown" };
}

export async function lookupContributionAttempt(
    send: (path: string, options: RequestInit) => Promise<Response>,
    fundingId: string,
    attempt: ContributionAttempt
): Promise<ContributionResult> {
    try {
        const response = await send(`/api/fundings/${fundingId}/contributions/idempotency`, {
            method: "GET",
            headers: { "Idempotency-Key": attempt.key },
            signal: AbortSignal.timeout(15_000),
        });
        if (response.status === 401) return { kind: "unauthorized" };
        if (response.status === 404) {
            const error = await response.json();
            if (error?.code === "CONTRIBUTION_ATTEMPT_NOT_FOUND") return { kind: "not-found" };
        }
        if (response.status === 200) {
            const body = await response.json();
            if (body?.request?.amount !== attempt.amount ||
                body.request.anonymous !== attempt.anonymous ||
                body.request.simulatePaymentFailure !== attempt.simulatePaymentFailure) {
                return { kind: "mismatch" };
            }
            if (body.result?.contributionStatus === "COMPLETED") return { kind: "completed" };
            if (body.result?.contributionStatus === "FAILED") return { kind: "failed" };
        }
    } catch {
        return { kind: "unknown" };
    }
    return { kind: "unknown" };
}

export function clearResolvedContributionAttempt(
    storage: Storage, storageKey: string, attempt: ContributionAttempt, result: ContributionResult
) {
    if (result.kind !== "completed" && result.kind !== "failed" && result.kind !== "rejected") return false;
    if (readContributionAttempt(storage, storageKey)?.key === attempt.key) {
        storage.removeItem(storageKey);
    }
    return true;
}

export function tryStartContributionAction(inFlight: { current: boolean }) {
    if (inFlight.current) return false;
    inFlight.current = true;
    return true;
}
