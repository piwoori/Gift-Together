import assert from "node:assert/strict";
import test from "node:test";
import {
    contributionStorageKey,
    prepareContributionAttempt,
    readContributionAttempt,
    sendContributionAttempt,
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
    const deadline = { code: "CONFLICT", message: "마감된 펀딩입니다." };
    assert.deepEqual(await classify(409, deadline), { kind: "rejected", message: deadline.message });
    const collision = { code: "CONFLICT", message: "이미 다른 참여 요청에 사용된 Idempotency-Key입니다." };
    assert.deepEqual(await classify(409, collision), { kind: "conflict", message: collision.message });
    assert.deepEqual(await classify(400, { code: "BAD_REQUEST", message: "남은 금액 초과" }),
        { kind: "rejected", message: "남은 금액 초과" });
    assert.deepEqual(await classify(401, {}), { kind: "unauthorized" });
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
