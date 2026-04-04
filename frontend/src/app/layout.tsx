import type { Metadata } from "next";
import "./globals.css";
import Providers from "./providers";

export const metadata: Metadata = {
  title: "CityHopper | Multi-City Travel, Finally Solved",
  description: "Plan multi-city trips with up to 5 destinations. CityHopper finds the cheapest or fastest combination of flights and brings you home.",
  openGraph: {
    title: "CityHopper | Multi-City Travel, Finally Solved",
    description: "Plan multi-city trips with up to 5 destinations. CityHopper finds the cheapest or fastest combination of flights and brings you home.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        {/* Apply saved theme before first paint to avoid flash */}
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){var t=localStorage.getItem('theme')||'dark';document.documentElement.classList.toggle('dark',t==='dark');})()`,
          }}
        />
      </head>
      <body className="antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
