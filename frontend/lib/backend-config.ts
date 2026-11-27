export function resolveBackendBaseUrl(value: string | undefined, development: boolean): string {
    if (value === undefined && development) return "http://127.0.0.1:8080";
    if (!value) throw new Error("BACKEND_BASE_URL is required.");

    // Validate the original shape too, since URL normalizes some paths and delimiters.
    if (/^https?:\/\/[^/?#@\\\s]+\/?$/i.test(value)) {
        try {
            const url = new URL(value);
            if (url.pathname === "/") return url.origin;
        } catch {
            // URL parser errors may include the supplied value.
        }
    }
    throw new Error("BACKEND_BASE_URL must be an HTTP(S) origin without credentials, path, query or fragment.");
}

export function backendBaseUrl(): string {
    return resolveBackendBaseUrl(process.env.BACKEND_BASE_URL, process.env.NODE_ENV === "development");
}
