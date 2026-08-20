"use client";

/**
 * Three curated route cards that link to pre-filled searches.
 * Shown in Zone 2 as an on-ramp for first-time visitors.
 */

import { Fragment } from "react";
import { ArrowRight } from "lucide-react";
import { POPULAR_ROUTES, type PopularRoute } from "@/data/popular-routes";
import { RouteMap } from "@/components/RouteMap";

// 30 days expressed in milliseconds — used to default the departure date to ~a month from now
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

function buildRouteUrl(route: PopularRoute): string {
  const date = new Date(Date.now() + THIRTY_DAYS_MS).toISOString().split("T")[0];
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
    { code: homeAirport.code, city: homeAirport.city },
    ...destinations.map((d) => ({ code: d.code, city: d.city })),
    { code: homeAirport.code, city: homeAirport.city },
  ];

  const journey = stops.map((s) => s.code);
  const mapAirports = Object.entries(route.coords).map(([code, { lat, lng }]) => ({ code, lat, lng }));

  // Alternate stop columns (min-content) and arrow columns in the grid.
  // min-content sizes each column to its longest single word, so multi-word
  // city names wrap at spaces rather than breaking mid-character.
  const gridTemplateColumns = stops
    .flatMap((_, i) => i < stops.length - 1 ? ["min-content", "min-content"] : ["min-content"])
    .join(" ");

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

      <RouteMap journey={journey} airports={mapAirports} mapHeight={130} interactive={false} />

      <div style={{ display: "grid", gridTemplateColumns, columnGap: "4px", rowGap: "2px" }}>
        {/* Row 1: codes and arrows */}
        {stops.map((stop, i) => (
          <Fragment key={`code-${i}`}>
            <span
              className="text-xs font-bold tracking-wide text-center"
              style={{ color: "var(--primary)" }}
            >
              {stop.code}
            </span>
            {i < stops.length - 1 && (
              <ArrowRight className="w-2.5 h-2.5 self-center" style={{ color: "var(--border)" }} />
            )}
          </Fragment>
        ))}
        {/* Row 2: city names — wrap at word boundaries within their column */}
        {stops.map((stop, i) => (
          <Fragment key={`city-${i}`}>
            <span
              className="text-[0.6rem] leading-tight text-center"
              style={{ color: "var(--ch-muted)" }}
            >
              {stop.city}
            </span>
            {i < stops.length - 1 && <span aria-hidden="true" />}
          </Fragment>
        ))}
      </div>

      <div className="flex items-center gap-1 text-xs font-medium mt-auto" style={{ color: "var(--primary)" }}>
        Search this route
        <ArrowRight className="w-3 h-3" />
      </div>
    </a>
  );
}

export function PopularRoutes() {
  return (
    <section className="pt-1 pb-16" style={{ background: "var(--background)" }}>
      <div className="max-w-6xl mx-auto px-6">
        <h2
          className="text-3xl font-semibold text-center mb-6"
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
