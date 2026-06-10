/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Allow opening the dev server from a phone/laptop on the LAN (HMR + dev
  // resources). Add your machine's LAN IPs here. Dev-only; ignored in prod.
  allowedDevOrigins: ["192.168.0.8", "192.168.1.8", "localhost"],
};

export default nextConfig;
