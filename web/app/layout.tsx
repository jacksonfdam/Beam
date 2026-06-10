import type { Metadata, Viewport } from "next";
import "./globals.css";

const title = "Beam — Turn any PDF into a flawless presentation";
const description =
  "Beam turns any exported PDF into a fullscreen presentation, controlled from your phone. Local-first, no accounts, no cloud. Open → Present → Done.";

export const metadata: Metadata = {
  metadataBase: new URL("https://beam.local"),
  title,
  description,
  applicationName: "Beam",
  keywords: [
    "presentation",
    "PDF presenter",
    "phone remote",
    "local-first",
    "Keynote",
    "PowerPoint",
  ],
  openGraph: {
    type: "website",
    title,
    description,
    siteName: "Beam",
  },
  twitter: {
    card: "summary_large_image",
    title,
    description,
  },
};

export const viewport: Viewport = {
  themeColor: "#07080b",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" data-scroll-behavior="smooth">
      <body className="min-h-screen font-sans antialiased">{children}</body>
    </html>
  );
}
