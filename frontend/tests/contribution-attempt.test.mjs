import assert from "node:assert/strict";
import test from "node:test";
import {
    clearResolvedContributionAttempt,
    contributionStorageKey,
    lookupContributionAttempt,
    prepareContributionAttempt,
    readContributionAttempt,
    sendContributionAttempt,
    tryStartContributionAction,
} from "../lib/contribution-attempt.ts";

function storage() {
    const data = new Map();
    return {
        getItem: (key) => data.get(key) ?? null,
        setItem: (key, value) => data.set(key, value),
        removeItem: (key) => data.delete(key),
    };
}

test("persists the first request before sending and reuses its key and body after a lost response and reload", async () => {
    const tab = storage();
    const scope = contributionStorageKey(7, "42");
    const first = prepareContributionAttempt(tab, scope, 3_000, true);
    const requests = [];
    const send = async (path, options) => {
        requests.push({ path, headers: options.headers, body: options.body });
        if (requests.length === 1) throw new TypeError("network disconnected");
        return Response.json({ contributionStatus: "COMPLETED" }, { status: 201 });
    };

    assert.equal((await sendContributionAttempt(send, "42", first)).kind, "unknown");
    const recovered = readContributionAttempt(tab, scope);
    assert.deepEqual(recovered, first);
    const retry = prepareContributionAttempt(tab, scope, 9_000, false);
    assert.equal((await sendContributionAttempt(send, "42", retry)).kind, "completed");
    assert.deepEqual(requests[0], requests[1]);
    assert.equal(requests[0].path, "/api/fundings/42/contributions");
    assert.equal(requests[0].headers["Idempotency-Key"], first.key);
    assert.deepEqual(JSON.parse(requests[0].body), {
        amount: 3_000, anonymous: true, simulatePaymentFailure: false,
    });
});

test("isolates saved requests by user and funding and generates fresh keys after a terminal result is cleared", () => {
    const tab = storage();
    const scope = contributionStorageKey(7, "42");
    const first = prepareContributionAttempt(tab, scope, 3_000, false);

    assert.equal(readContributionAttempt(tab, contributionStorageKey(8, "42")), null);
    assert.equal(readContributionAttempt(tab, contributionStorageKey(7, "43")), null);
    tab.removeItem(scope);
    const second = prepareContributionAttempt(tab, scope, 3_000, false);
    assert.notEqual(first.key, second.key);
    tab.removeItem(scope);
    assert.notEqual(prepareContributionAttempt(tab, scope, 3_000, false).key, second.key);
});

test("does not overwrite unreadable pending data", () => {
    const tab = storage();
    const scope = contributionStorageKey(7, "42");
    tab.setItem(scope, "invalid JSON");
    assert.throws(() => prepareContributionAttempt(tab, scope, 3_000, false));
    assert.equal(tab.getItem(scope), "invalid JSON");
});

test("distinguishes a confirmed failure, a funding rejection, and a key conflict", async () => {
    const attempt = prepareContributionAttempt(storage(), "request", 1_000, false);
    async function classify(status, body) {
        return sendContributionAttempt(async () => Response.json(body, { status }), "42", attempt);
    }

    assert.deepEqual(await classify(201, { contributionStatus: "FAILED" }), { kind: "failed" });
    const deadline = { code: "FUNDING_DEADLINE_PASSED", message: "Deadline copy changed." };
    assert.deepEqual(await classify(409, deadline), { kind: "rejected", message: deadline.message });
    const closed = { code: "FUNDING_NOT_OPEN", message: "Closed copy changed." };
    assert.deepEqual(await classify(409, closed), { kind: "rejected", message: closed.message });
    const collision = { code: "IDEMPOTENCY_KEY_CONFLICT", message: "Conflict copy changed." };
    assert.deepEqual(await classify(409, collision), { kind: "conflict", message: collision.message });
    assert.deepEqual(await classify(400, { code: "BAD_REQUEST", message: "남은 금액 초과" }),
        { kind: "rejected", message: "남은 금액 초과" });
    assert.deepEqual(await classify(401, {}), { kind: "unauthorized" });
});

test("a changed idempotency conflict message still preserves the original key and body", async () => {
    const tab = storage();
    const scope = contributionStorageKey(7, "42");
    const attempt = prepareContributionAttempt(tab, scope, 3_000, true);
    const requests = [];
    const send = async (_path, options) => {
        requests.push({ headers: options.headers, body: options.body });
        return Response.json({ code: "IDEMPOTENCY_KEY_CONFLICT", message: "이미 처리된 요청과 내용이 일치하지 않습니다." },
            { status: 409 });
    };

    assert.equal((await sendContributionAttempt(send, "42", attempt)).kind, "conflict");
    const retry = prepareContributionAttempt(tab, scope, 9_000, false);
    assert.deepEqual(retry, attempt);
    assert.equal((await sendContributionAttempt(send, "42", retry)).kind, "conflict");
    assert.deepEqual(requests[1], requests[0]);
});

test("only explicitly recognized funding conflicts can end a pending attempt", async () => {
    const attempt = prepareContributionAttempt(storage(), "request", 1_000, false);
    for (const body of [
        { code: "CONFLICT", message: "이 펀딩은 이미 마감되었습니다." },
        { code: "OTHER_CONFLICT", message: "마감된 펀딩입니다." },
        { message: "마감된 펀딩입니다." },
        { code: "CONFLICT" },
    ]) {
        const result = await sendContributionAttempt(
            async () => Response.json(body, { status: 409 }), "42", attempt
        );
        assert.ok(["unknown", "conflict"].includes(result.kind));
    }
});

test("keeps 5xx, malformed responses and timeouts unresolved", async () => {
    const attempt = prepareContributionAttempt(storage(), "request", 1_000, false);
    for (const response of [
        Response.json({ code: "BAD_REQUEST", message: "unexpected" }, { status: 500 }),
        new Response("<html>broken</html>", { status: 201 }),
        Response.json({ contributionStatus: "PENDING" }, { status: 201 }),
        new Response("", { status: 409 }),
    ]) {
        assert.equal((await sendContributionAttempt(async () => response, "42", attempt)).kind, "unknown");
    }
    assert.equal((await sendContributionAttempt(async () => {
        throw new DOMException("Timed out", "TimeoutError");
    }, "42", attempt)).kind, "unknown");
});

test("recovers and sends a final contribution below 1,000 won with the original amount and key", async () => {
    const tab = storage();
    const scope = contributionStorageKey(7, "42");
    const attempt = prepareContributionAttempt(tab, scope, 1, true);
    assert.deepEqual(readContributionAttempt(tab, scope), attempt);
    const retry = prepareContributionAttempt(tab, scope, 999, false);
    const result = await sendContributionAttempt(async (_path, options) => {
        assert.equal(options.headers["Idempotency-Key"], attempt.key);
        assert.deepEqual(JSON.parse(options.body), {
            amount: 1, anonymous: true, simulatePaymentFailure: false,
        });
        return Response.json({ contributionStatus: "COMPLETED" }, { status: 201 });
    }, "42", retry);
    assert.equal(result.kind, "completed");
});

test("lookup clears a matching completed or failed attempt and allows another key", async () => {
    for (const status of ["COMPLETED", "FAILED"]) {
        const tab = storage();
        const scope = contributionStorageKey(7, "42");
        const attempt = prepareContributionAttempt(tab, scope, 999, true);
        const result = await lookupContributionAttempt(async (path, options) => {
            assert.equal(path, "/api/fundings/42/contributions/idempotency");
            assert.equal(options.method, "GET");
            assert.equal(options.headers["Idempotency-Key"], attempt.key);
            assert.equal(options.body, undefined);
            return Response.json({
                request: { amount: 999, anonymous: true, simulatePaymentFailure: false },
                result: { contributionStatus: status },
            });
        }, "42", attempt);

        assert.equal(result.kind, status.toLowerCase());
        assert.equal(clearResolvedContributionAttempt(tab, scope, attempt, result), true);
        assert.equal(readContributionAttempt(tab, scope), null);
        assert.notEqual(prepareContributionAttempt(tab, scope, 999, true).key, attempt.key);
    }
});

test("lookup preserves the saved key and body on absence, errors, or any request mismatch", async () => {
    const tab = storage();
    const scope = contributionStorageKey(7, "42");
    const attempt = prepareContributionAttempt(tab, scope, 999, true);
    const request = { amount: 999, anonymous: true, simulatePaymentFailure: false };
    const completed = { contributionStatus: "COMPLETED" };
    const lostNetwork = async () => { throw new TypeError("network disconnected"); };
    for (const [send, kind] of [
        [async () => Response.json({ code: "CONTRIBUTION_ATTEMPT_NOT_FOUND" }, { status: 404 }), "not-found"],
        [async () => new Response("", { status: 401 }), "unauthorized"],
        [async () => new Response("", { status: 503 }), "unknown"],
        [lostNetwork, "unknown"],
        ...[{ amount: 998 }, { anonymous: false }, { simulatePaymentFailure: true }].map((change) => [
            async () => Response.json({ request: { ...request, ...change }, result: completed }), "mismatch",
        ]),
    ]) {
        const result = await lookupContributionAttempt(send, "42", attempt);
        assert.equal(result.kind, kind);
        assert.equal(clearResolvedContributionAttempt(tab, scope, attempt, result), false);
        assert.deepEqual(prepareContributionAttempt(tab, scope, 1, false), attempt);
    }
});

test("lookup and retry share a synchronous guard that can be released for a later attempt", () => {
    const inFlight = { current: false };
    assert.equal(tryStartContributionAction(inFlight), true); // lookup
    assert.equal(tryStartContributionAction(inFlight), false); // concurrent retry
    inFlight.current = false;
    assert.equal(tryStartContributionAction(inFlight), true); // retry
    assert.equal(tryStartContributionAction(inFlight), false); // concurrent lookup
});
