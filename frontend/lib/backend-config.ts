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

export function assertBackendRewriteMatches(manifest: unknown, backend: string): void {
    const rewrites = manifest && typeof manifest === "object" && "rewrites" in manifest
        ? manifest.rewrites
        : null;
    const afterFiles = rewrites && typeof rewrites === "object" && "afterFiles" in rewrites
        ? rewrites.afterFiles
        : null;
    const matches = Array.isArray(afterFiles)
        ? afterFiles.filter((route) => route?.source === "/api/:path*")
        : [];

    if (matches.length !== 1 || typeof matches[0].destination !== "string") {
        throw new Error("Cannot verify the built API rewrite. Rebuild before starting the server.");
    }
    if (matches[0].destination !== `${backend}/api/:path*`) {
        throw new Error("BACKEND_BASE_URL does not match the built API rewrite. Rebuild with the runtime setting.");
    }
}
