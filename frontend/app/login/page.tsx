"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { login, saveAccessToken } from "@/lib/auth";

export default function LoginPage() {
    const router = useRouter();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            const response = await login({ email, password });

            saveAccessToken(response.accessToken);

            router.push("/fundings/new?productId=1");
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : "로그인 중 오류가 발생했습니다."
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6">
            <h1 className="mb-2 text-3xl font-bold">
                Gift Together
            </h1>

            <p className="mb-8 text-gray-500">
                로그인하고 함께 선물하는 즐거움을 시작하세요.
            </p>

            <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                    <label
                        htmlFor="email"
                        className="mb-2 block text-sm font-medium"
                    >
                        이메일
                    </label>

                    <input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder="example@email.com"
                        autoComplete="email"
                        required
                        className="w-full rounded-lg border p-3"
                    />
                </div>

                <div>
                    <label
                        htmlFor="password"
                        className="mb-2 block text-sm font-medium"
                    >
                        비밀번호
                    </label>

                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        placeholder="비밀번호 입력"
                        autoComplete="current-password"
                        required
                        className="w-full rounded-lg border p-3"
                    />
                </div>

                {error && (
                    <p role="alert" className="text-sm text-red-500">
                        {error}
                    </p>
                )}

                <button
                    type="submit"
                    disabled={loading}
                    className="w-full rounded-lg bg-teal-600 p-3 font-semibold text-white disabled:opacity-50"
                >
                    {loading ? "로그인 중..." : "로그인"}
                </button>
            </form>

            <p className="mt-6 text-center text-sm text-gray-500">
                아직 계정이 없으신가요?{" "}
                <Link
                    href="/signup"
                    className="font-semibold text-teal-600"
                >
                    회원가입
                </Link>
            </p>
        </main>
    );
}