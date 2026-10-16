
"use client";

import { useState } from "react";

type ShareFundingProps = {
    fundingId: number;
    productName: string;
};

export default function ShareFunding({
                                         fundingId,
                                         productName,
                                     }: ShareFundingProps) {
    const [message, setMessage] = useState("");

    function getShareUrl() {
        return `${window.location.origin}/fundings/${fundingId}`;
    }

    async function copyLink() {
        try {
            await navigator.clipboard.writeText(getShareUrl());
            setMessage("펀딩 링크를 복사했어요!");
        } catch {
            setMessage("링크 복사에 실패했어요. 브라우저 권한을 확인해 주세요.");
        }
    }

    async function shareFunding() {
        const url = getShareUrl();

        if (!navigator.share) {
            await copyLink();
            return;
        }

        try {
            await navigator.share({
                title: "Gift Together - 공동 선물하기",
                text: `${productName} 선물을 함께 준비해 주세요! 🎁`,
                url,
            });
        } catch (error) {
            if (error instanceof DOMException && error.name === "AbortError") {
                return;
            }

            setMessage("공유하지 못했어요. 링크 복사를 이용해 주세요.");
        }
    }

    return (
        <div className="space-y-3">
            <button
                type="button"
                onClick={shareFunding}
                className="w-full rounded-2xl bg-[#328c82] py-4 font-semibold text-white transition hover:bg-[#28776e]"
            >
                🎁 친구에게 공유하기
            </button>

            <button
                type="button"
                onClick={copyLink}
                className="w-full rounded-2xl border border-[#328c82] py-4 font-semibold text-[#328c82] transition hover:bg-[#e9f7f4]"
            >
                🔗 링크 복사하기
            </button>

            {message && (
                <p role="status" className="text-center text-sm text-gray-600">
                    {message}
                </p>
            )}
        </div>
    );
}
