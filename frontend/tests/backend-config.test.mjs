import assert from "node:assert/strict";
import test from "node:test";
import { resolveBackendBaseUrl } from "../lib/backend-config.ts";

test("normalizes the origin used for SSR and rewrites", () => {
    assert.equal(resolveBackendBaseUrl("http://127.0.0.1:18080/", false), "http://127.0.0.1:18080");
    assert.equal(resolveBackendBaseUrl("https://backend.example.test", false), "https://backend.example.test");
    assert.equal(resolveBackendBaseUrl("http://[::1]:8080/", false), "http://[::1]:8080");
});

test("requires a production origin and only defaults to loopback in development", () => {
    assert.equal(resolveBackendBaseUrl(undefined, true), "http://127.0.0.1:8080");
    assert.throws(() => resolveBackendBaseUrl(undefined, false), /is required/);
    assert.throws(() => resolveBackendBaseUrl("", true), /is required/);
});

test("rejects invalid or credential-bearing URLs without echoing the value", () => {
    for (const url of [
        "https://qa-user:qa-password@backend.example.test",
        "https://backend.example.test/api", "https://backend.example.test/../",
        "https://backend.example.test/?", "https://backend.example.test/#",
        "https://backend.example.test\\other", "https://backend.example.test:70000",
        "ftp://backend.example.test", "//backend.example.test",
    ]) {
        assert.throws(() => resolveBackendBaseUrl(url, false),
            (error) => /must be an HTTP/.test(error.message) && !error.message.includes(url));
    }
});
