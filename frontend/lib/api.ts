import { getAccessToken } from "@/lib/auth";

export async function apiFetch(
    path: string,
    options: RequestInit = {}
): Promise<Response> {
    const token = getAccessToken();

    const headers = new Headers(options.headers);

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    return fetch(path, {
        ...options,
        headers,
        cache: "no-store",
    });
}