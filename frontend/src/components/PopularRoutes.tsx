"use client";

/**
 * Three curated route cards that link to pre-filled searches.
 * Shown in Zone 2 as an on-ramp for first-time visitors.
 */

import { ArrowRight } from "lucide-react";
import { POPULAR_ROUTES, type PopularRoute } from "@/data/popular-routes";

function buildRouteUrl(route: PopularRoute): string {
  const date = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0];
  const params = new URLSearchParams({
    from: route.values.homeAirport.code,
    destinations: route.values.destinations.map((d) => d.code).join(","),
    days: route.values.destinations.map((d) => String(d.days)).join(","),
    optimizeBy: route.values.optimizeBy,
    date,
  });
  return `/?${params.toString()}`;
}

function RouteCard({ route }: { route: PopularRoute }) {
  const { homeAirport, destinations } = route.values;

  const stops = [
    { code: homeAirport.code, city: homeAirport.city, days: null },
    ...destinations.map((d) => ({ code: d.code, city: d.city, days: d.days })),
    { code: homeAirport.code, city: homeAirport.city, days: null },
  ];

  return (
    <a
      href={buildRouteUrl(route)}
      className="glass flex flex-col gap-5 p-6 transition-transform duration-200 hover:scale-[1.02] focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
    >
      <h3
        className="text-lg font-semibold"
        style={{ fontFamily: "var(--font-display)", color: "var(--foreground)" }}
      >
        {route.title}
      </h3>

      {/* Stop chain — horizontally scrollable on narrow screens */}
      <div className="flex items-start gap-1.5 overflow-x-auto pb-1">
        {stops.map((stop, i) => (
          <div key={`${stop.code}-${i}`} className="flex items-start gap-1.5 shrink-0">
            <div className="flex flex-col items-center gap-0.5 min-w-[2.75rem]">
              <span className="text-xs font-bold tracking-wide" style={{ color: "var(--primary)" }}>
                {stop.code}
              </span>
              <span className="text-[0.65rem] leading-tight text-center" style={{ color: "var(--ch-muted)" }}>
                {stop.city}
              </span>
              {stop.days !== null && (
                <span className="text-[0.6rem]" style={{ color: "var(--ch-muted)" }}>
                  {stop.days}n
                </span>
              )}
            </div>
            {i < stops.length - 1 && (
              <ArrowRight className="w-3 h-3 mt-0.5 shrink-0" style={{ color: "var(--border)" }} />
            )}
          </div>
        ))}
      </div>

      {/* CTA */}
      <div className="flex items-center gap-1 text-xs font-medium mt-auto" style={{ color: "var(--primary)" }}>
        Search this route
        <ArrowRight className="w-3 h-3" />
      </div>
    </a>
  );
}

export function PopularRoutes() {
  return (
    <section className="py-16" style={{ background: "var(--background)" }}>
      <div className="max-w-6xl mx-auto px-6">
        <h2
          className="text-3xl font-semibold text-center mb-10"
          style={{ fontFamily: "var(--font-display)", color: "var(--foreground)" }}
        >
          Popular routes
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {POPULAR_ROUTES.map((route) => (
            <RouteCard key={route.title} route={route} />
          ))}
        </div>
      </div>
    </section>
  );
}
