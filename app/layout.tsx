import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Navbar from "@/components/Navbar";
import BottomNav from "@/components/BottomNav";
import { Providers } from "@/components/Providers";
import { SettingsProvider } from "@/components/SettingsProvider";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Gym Tracker",
  description: "Track your gym progress and personal records",
  manifest: "/manifest.json",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${inter.className} bg-black text-white min-h-screen pb-20 sm:pb-0`}>
        <Providers>
          <SettingsProvider>
            <Navbar />
            {children}
            <BottomNav />
          </SettingsProvider>
        </Providers>
      </body>
    </html>
  );
}
