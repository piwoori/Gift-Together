import assert from "node:assert/strict";
import test from "node:test";
import {
    formatFundingDeadline,
    getDeadlineTime,
    isFundingBeforeDeadline,
} from "../lib/deadline.ts";

const seoulDeadline = "2026-12-01T18:00:00";
const utcDeadline = "2026-12-01T09:00:00Z";
const deadlineTime = Date.parse(utcDeadline);

test("interprets both datetime-local input and legacy API values as Seoul time", () => {
    assert.equal(getDeadlineTime("2026-12-01T18:00"), deadlineTime);
    assert.equal(getDeadlineTime(seoulDeadline), deadlineTime);
});

test("only permits actions strictly before the deadline", () => {
    assert.equal(isFundingBeforeDeadline(seoulDeadline, deadlineTime - 1), true);
    assert.equal(isFundingBeforeDeadline(seoulDeadline, deadlineTime), false);
    assert.equal(isFundingBeforeDeadline(seoulDeadline, deadlineTime + 1), false);
});

test("formats the deadline in Seoul time regardless of the caller's time zone", () => {
    assert.match(formatFundingDeadline(seoulDeadline), /오후 6:00/);
    assert.equal(formatFundingDeadline(utcDeadline), formatFundingDeadline(seoulDeadline));
});
