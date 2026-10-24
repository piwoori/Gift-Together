import { getAccessToken, logout } from "@/lib/auth";

export async function apiFetch(
    path: string,
    options: RequestInit = {}
): Promise<Response> {
    const token = getAccessToken();
    const headers = new Headers(options.headers);

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(path, {
        ...options,
        headers,
        cache: "no-store",
    });

    // JWT가 만료되었거나 유효하지 않은 경우
    if (response.status === 401 && token && typeof window !== "undefined") {

        logout();

        const currentPath =
            window.location.pathname + window.location.search;

        const loginUrl = `/login?next=${encodeURIComponent(currentPath)}`;

        window.location.replace(loginUrl);
    }

    return response;
}