import { SiteHeader } from "@/components/landing/SiteHeader";
import { Hero } from "@/components/landing/Hero";
import { Features } from "@/components/landing/Features";
import { HowToConnect } from "@/components/landing/HowToConnect";
import { Downloads } from "@/components/landing/Downloads";
import { SiteFooter } from "@/components/landing/SiteFooter";

export default function Home() {
  return (
    <>
      <SiteHeader />
      <main id="main">
        <Hero />
        <Features />
        <HowToConnect />
        <Downloads />
      </main>
      <SiteFooter />
    </>
  );
}
