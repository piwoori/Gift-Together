import { Suspense } from "react";
import WishlistContent from "./WishlistContent";

export default function WishlistPage() {
    return (
        <Suspense
            fallback={
                <main className="min-h-screen bg-[#f8faf9]">
                    <div className="mx-auto min-h-screen max-w-md bg-white px-6 py-8">
                        <p className="text-sm text-gray-500">
                            위시리스트를 불러오는 중...
                        </p>
                    </div>
                </main>
            }
        >
            <WishlistContent />
        </Suspense>
    );
}