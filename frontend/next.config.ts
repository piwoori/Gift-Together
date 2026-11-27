import type { NextConfig } from "next";
import { backendBaseUrl } from "./lib/backend-config";

const backend = backendBaseUrl();

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backend}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;