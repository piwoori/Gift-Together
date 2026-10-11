
import Link from "next/link";

type Props = {
    productName: string;
    targetAmount: number;
    participantCount: number;
};

export default function FundingCompleted({
                                             productName,
                                             targetAmount,
                                             participantCount,
                                         }: Props) {
    return (
        <div className="rounded-2xl border border-[#d8eee9] bg-white p-6 text-center">
            <div className="mb-4 text-5xl">🎁</div>

            <h2 className="text-xl font-bold text-gray-900">
                공동 선물이 완성됐어요!
            </h2>

            <p className="mt-3 text-sm leading-6 text-gray-500">
                친구들의 마음이 모여
                <br />
                목표 금액을 달성했어요.
            </p>

            <div className="mt-7 rounded-xl bg-[#e9f7f4] p-5">
                <p className="text-sm text-gray-500">{productName}</p>

                <p className="mt-2 text-2xl font-bold text-[#328c82]">
                    {targetAmount.toLocaleString()}원
                </p>

                <div className="mt-4 h-2 overflow-hidden rounded-full bg-gray-200">
                    <div className="h-full w-full bg-[#328c82]" />
                </div>

                <p className="mt-3 text-sm font-semibold text-[#328c82]">
                    목표 달성 100%
                </p>
            </div>

            <p className="mt-5 text-sm text-gray-500">
                완료된 참여 {participantCount}건
            </p>

            <p className="mt-5 text-xs leading-5 text-gray-400">
                현재는 모의 결제 기반 서비스로,
                실제 상품 구매 및 배송은 진행되지 않습니다.
            </p>

            <Link
                href="/wishlist"
                className="mt-7 block rounded-xl bg-[#328c82] py-4 font-semibold text-white"
            >
                위시리스트로 돌아가기
            </Link>
        </div>
    );
}
