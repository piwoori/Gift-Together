
export type LoginRequest = {
    email: string;
    password: string;
};

export type LoginResponse = {
    accessToken: string;
    userId: number;
    nickname: string;
};

const TOKEN_KEY = "gift_together_access_token";

export async function login(
    request: LoginRequest
): Promise<LoginResponse> {
    const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
        cache: "no-store",
    });

    if (!response.ok) {
        throw new Error("이메일 또는 비밀번호를 확인해 주세요.");
    }

    return response.json();
}

export function saveAccessToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
}

export function getAccessToken(): string | null {
    if (typeof window === "undefined") {
        return null;
    }

    return localStorage.getItem(TOKEN_KEY);
}

export function logout(): void {
    localStorage.removeItem(TOKEN_KEY);
}


export type SignupRequest = {
    email: string;
    password: string;
    nickname: string;
};

export async function signup(
    request: SignupRequest
): Promise<void> {
    const response = await fetch("/api/auth/signup", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
        cache: "no-store",
    });

    if (response.status === 409) {
        throw new Error("이미 가입된 이메일입니다.");
    }

    if (!response.ok) {
        throw new Error("회원가입에 실패했습니다. 입력 정보를 확인해 주세요.");
    }
}