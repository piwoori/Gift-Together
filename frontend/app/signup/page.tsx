"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { signup } from "@/lib/auth";

export default function SignupPage() {
    const router = useRouter();

    const [email, setEmail] = useState("");
    const [nickname, setNickname] = useState("");
    const [password, setPassword] = useState("");
    const [passwordConfirm, setPasswordConfirm] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError("");

        if (password !== passwordConfirm) {
            setError("비밀번호가 일치하지 않습니다.");
            return;
        }

        if (password.length < 8) {
            setError("비밀번호는 8자 이상 입력해 주세요.");
            return;
        }

        setLoading(true);

        try {
            await signup({
                email: email.trim(),
                nickname: nickname.trim(),
                password,
            });

            alert("회원가입이 완료되었습니다! 로그인해 주세요.");
            router.push("/login");
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : "회원가입 중 오류가 발생했습니다."
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6">
            <h1 className="mb-2 text-3xl font-bold">
                Gift Together 🎁
            </h1>

            <p className="mb-8 text-gray-500">
                회원가입하고 함께 선물하는 즐거움을 시작하세요.
            </p>

            <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                    <label htmlFor="nickname" className="mb-2 block text-sm font-medium">
                        닉네임
                    </label>
                    <input
                        id="nickname"
                        type="text"
                        value={nickname}
                        onChange={(event) => setNickname(event.target.value)}
                        placeholder="닉네임 입력"
                        autoComplete="nickname"
                        required
                        className="w-full rounded-lg border p-3"
                    />
                </div>

                <div>
                    <label htmlFor="email" className="mb-2 block text-sm font-medium">
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
                    <label htmlFor="password" className="mb-2 block text-sm font-medium">
                        비밀번호
                    </label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        placeholder="8자 이상 입력"
                        autoComplete="new-password"
                        minLength={8}
                        required
                        className="w-full rounded-lg border p-3"
                    />
                </div>

                <div>
                    <label htmlFor="passwordConfirm" className="mb-2 block text-sm font-medium">
                        비밀번호 확인
                    </label>
                    <input
                        id="passwordConfirm"
                        type="password"
                        value={passwordConfirm}
                        onChange={(event) => setPasswordConfirm(event.target.value)}
                        placeholder="비밀번호 다시 입력"
                        autoComplete="new-password"
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
                    {loading ? "가입 중..." : "회원가입"}
                </button>
            </form>

            <p className="mt-6 text-center text-sm text-gray-500">
                이미 계정이 있으신가요?{" "}
                <Link href="/login" className="font-semibold text-teal-600">
                    로그인
                </Link>
            </p>
        </main>
    );
}