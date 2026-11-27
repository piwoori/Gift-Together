import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { NextConfig } from "next";
import { PHASE_PRODUCTION_SERVER } from "next/constants";
import { assertBackendRewriteMatches, backendBaseUrl } from "./lib/backend-config";

export default function configureNext(phase: string): NextConfig {
    const backend = backendBaseUrl();
    const distDir = ".next";

    if (phase === PHASE_PRODUCTION_SERVER) {
        let manifest: unknown;
        try {
            // Next uses this built manifest for rewrites when serving the production build.
            manifest = JSON.parse(readFileSync(join(__dirname, distDir, "routes-manifest.json"), "utf8"));
        } catch {
            throw new Error("Cannot verify the built API rewrite. Rebuild before starting the server.");
        }
        assertBackendRewriteMatches(manifest, backend);
    }

    return {
        distDir,
        async rewrites() {
            return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
        },
    };
}
