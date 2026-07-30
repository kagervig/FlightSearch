/*
 * board/page.tsx — Departures and arrivals board for a selected airport.
 * Fetches from GET /api/flights/board?airport=XXX
 */

"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { Plane, ArrowLeft, AlertCircle, ChevronDown, ChevronUp } from "lucide-react";
import { AirportAutocomplete } from "@/components/AirportAutocomplete";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Skeleton } from "@/components/ui/Skeleton";
import { fetchBoardFlights, type BoardFlight, type BoardResponse } from "@/lib/boardApi";
import { cn, formatDuration } from "@/lib/utils";

type Tab = "departures" | "arrivals";

const TIME_SLOTS = [
  { label: "Night (0–6)",       min: 0,  max: 6  },
  { label: "Morning (6–12)",    min: 6,  max: 12 },
  { label: "Afternoon (12–18)", min: 12, max: 18 },
  { label: "Evening (18–24)",   min: 18, max: 24 },
];

// ── Spider map ────────────────────────────────────────────────────────────────

// Shared world-topology cache (same pattern as NetworkMap)
let worldDataCache: unknown = null;
async function getWorldData(): Promise<unknown> {
  if (worldDataCache) return worldDataCache;
  const res = await fetch("https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json");
  worldDataCache = await res.json();
  return worldDataCache;
}

interface SpiderMapProps {
  hubCode: string;
  hubLat: number;
  hubLon: number;
  departures: BoardFlight[];
  arrivals: BoardFlight[];
}

interface Tooltip {
  x: number;
  y: number;
  code: string;
  city: string;
}

function SpiderMap({ hubCode, hubLat, hubLon, departures, arrivals }: SpiderMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [tooltip, setTooltip] = useState<Tooltip | null>(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const obs = new ResizeObserver(entries => {
      const { width, height } = entries[0].contentRect;
      if (width && height) setSize({ width: Math.floor(width), height: Math.floor(height) });
    });
    obs.observe(el);
    setSize({ width: Math.floor(el.clientWidth), height: Math.floor(el.clientHeight) });
    return () => obs.disconnect();
  }, []);

  useEffect(() => {
    const { width, height } = size;
    if (!svgRef.current || width === 0 || height === 0) return;

    let cancelled = false;

    async function draw() {
      const [d3, { feature }, worldData] = await Promise.all([
        import("d3"),
        import("topojson-client"),
        getWorldData(),
      ]);
      if (cancelled || !svgRef.current) return;

      const isDark = document.documentElement.classList.contains("dark");
      const oceanFill  = isDark ? "hsl(222 47% 11%)" : "hsl(210 40% 96%)";
      const landFill   = isDark ? "hsl(220 40% 18%)" : "hsl(215 20% 86%)";
      const landStroke = isDark ? "hsl(220 40% 26%)" : "hsl(215 20% 74%)";
      const dotFill    = isDark ? "hsl(214 84% 65%)" : "hsl(214 84% 50%)";

      // Unique connected airports
      const airportMap = new Map<string, { lat: number; lon: number; city: string }>();
      for (const f of departures)
        airportMap.set(f.destination, { lat: f.destinationLat, lon: f.destinationLon, city: f.destinationCity });
      for (const f of arrivals)
        airportMap.set(f.origin, { lat: f.originLat, lon: f.originLon, city: f.originCity });
      const airports = Array.from(airportMap.entries()).map(([code, pos]) => ({ code, ...pos }));

      // Build a GeoJSON FeatureCollection of hub + all destinations so fitExtent
      // can auto-scale to show exactly the relevant region.
      const PAD = 32;
      const allPoints = {
        type: "FeatureCollection" as const,
        features: [
          { type: "Feature" as const, properties: {}, geometry: { type: "Point" as const, coordinates: [hubLon, hubLat] } },
          ...airports.map(a => ({
            type: "Feature" as const, properties: {}, geometry: { type: "Point" as const, coordinates: [a.lon, a.lat] },
          })),
        ],
      };

      const projection = d3.geoNaturalEarth1()
        .fitExtent([[PAD, PAD], [width - PAD, height - PAD]], allPoints);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const pathGen = d3.geoPath().projection(projection) as any;

      const svg = d3.select(svgRef.current);
      svg.selectAll("*").remove();
      svg.attr("width", width).attr("height", height);

      // Ocean background
      svg.append("rect")
        .attr("width", width).attr("height", height)
        .attr("fill", oceanFill);

      // Land
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const world = worldData as any;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const countries = feature(world, world.objects.countries) as any;
      svg.append("g")
        .selectAll("path")
        .data(countries.features)
        .join("path")
        .attr("d", pathGen)
        .attr("fill", landFill)
        .attr("stroke", landStroke)
        .attr("stroke-width", 0.4);

      // Route arcs — geoPath interpolates great circles automatically
      for (const a of airports) {
        svg.append("path")
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          .datum({ type: "LineString", coordinates: [[hubLon, hubLat], [a.lon, a.lat]] } as any)
          .attr("d", pathGen)
          .attr("fill", "none")
          .attr("stroke", "var(--ch-accent)")
          .attr("stroke-width", 1)
          .attr("stroke-opacity", 0.5)
          .attr("stroke-linecap", "round");
      }

      // Destination dots with mouseover tooltips
      for (const a of airports) {
        const p = projection([a.lon, a.lat]);
        if (!p) continue;
        svg.append("circle")
          .attr("cx", p[0]).attr("cy", p[1])
          .attr("r", 3)
          .attr("fill", dotFill)
          .attr("opacity", 0.85)
          .style("cursor", "pointer")
          .on("mouseenter", (event: MouseEvent) => {
            const rect = containerRef.current?.getBoundingClientRect();
            if (rect) setTooltip({ x: event.clientX - rect.left, y: event.clientY - rect.top, code: a.code, city: a.city });
          })
          .on("mouseleave", () => setTooltip(null));
      }

      // Hub dot — same tooltip behaviour as destination dots
      const hubP = projection([hubLon, hubLat]);
      if (hubP) {
        svg.append("circle")
          .attr("cx", hubP[0]).attr("cy", hubP[1])
          .attr("r", 7)
          .attr("fill", "var(--primary)")
          .style("cursor", "pointer")
          .on("mouseenter", (event: MouseEvent) => {
            const rect = containerRef.current?.getBoundingClientRect();
            if (rect) setTooltip({ x: event.clientX - rect.left, y: event.clientY - rect.top, code: hubCode, city: "" });
          })
          .on("mouseleave", () => setTooltip(null));
      }
    }

    draw();
    return () => { cancelled = true; };
  }, [size, hubCode, hubLat, hubLon, departures, arrivals, setTooltip]);

  return (
    <div ref={containerRef} className="relative w-full" style={{ height: 440 }}>
      <svg ref={svgRef} aria-label="Route spider map" role="img" />
      {tooltip && (
        <div
          className="absolute pointer-events-none px-3 py-2 rounded-xl text-xs shadow-xl z-10"
          style={{
            left: tooltip.x + 14,
            top: tooltip.y - 10,
            background: "var(--card)",
            border: "1px solid var(--border)",
          }}
        >
          <p className="font-bold" style={{ color: "var(--ch-accent)" }}>{tooltip.code}</p>
          <p style={{ color: "var(--foreground)" }}>{tooltip.city}</p>
        </div>
      )}
    </div>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────

export default function BoardPage() {
  const [airportCode, setAirportCode] = useState("");
  const [airportCity, setAirportCity] = useState("");
  const [activeTab, setActiveTab] = useState<Tab>("departures");
  const [selectedSlots, setSelectedSlots] = useState<Set<number>>(new Set());
  const [selectedAirline, setSelectedAirline] = useState("");
  const [mapOpen, setMapOpen] = useState(true);

  const { mutate, data, isPending, error } = useMutation<BoardResponse, Error, string>({
    mutationFn: fetchBoardFlights,
  });

  function handleSearch() {
    if (!airportCode) return;
    setSelectedSlots(new Set());
    setSelectedAirline("");
    mutate(airportCode);
  }

  function toggleSlot(index: number) {
    setSelectedSlots(prev => {
      const next = new Set(prev);
      next.has(index) ? next.delete(index) : next.add(index);
      return next;
    });
  }

  const allAirlines = data
    ? Array.from(
        new Set([...data.departures, ...data.arrivals].map(f => f.airlineName))
      ).sort()
    : [];

  function applyFilters(flights: BoardFlight[]): BoardFlight[] {
    return flights.filter(f => {
      const hour = parseInt(f.departureTime.split(":")[0], 10);
      const slotOk =
        selectedSlots.size === 0 ||
        Array.from(selectedSlots).some(i => hour >= TIME_SLOTS[i].min && hour < TIME_SLOTS[i].max);
      const airlineOk = !selectedAirline || f.airlineName === selectedAirline;
      return slotOk && airlineOk;
    });
  }

  const flights: BoardFlight[] = data ? applyFilters(data[activeTab]) : [];

  return (
    <div className="min-h-screen flex flex-col" style={{ background: "var(--background)" }}>

      {/* Header */}
      <header
        className="shrink-0 px-6 py-4 flex items-center justify-between"
        style={{ borderBottom: "1px solid var(--border)" }}
      >
        <div className="flex items-center gap-4">
          <Link
            href="/"
            className="flex items-center gap-1.5 text-sm font-medium transition-colors hover:opacity-100"
            style={{ color: "var(--ch-muted)", opacity: 0.85 }}
          >
            <ArrowLeft className="w-4 h-4" />
            Back
          </Link>
          <div className="h-5 w-px" style={{ background: "var(--border)" }} />
          <div className="flex items-center gap-2">
            <Plane className="w-4 h-4" style={{ color: "var(--primary)" }} />
            <span
              className="font-semibold"
              style={{ fontFamily: "var(--font-display)", color: "var(--foreground)" }}
            >
              Flight Board
            </span>
          </div>
        </div>
        <ThemeToggle />
      </header>

      {/* Search */}
      <div className="max-w-2xl mx-auto w-full px-6 pt-8 pb-6 flex gap-3 items-end">
        <div className="flex-1">
          <AirportAutocomplete
            value={airportCode}
            onChange={(code, city) => {
              setAirportCode(code);
              setAirportCity(city);
            }}
            placeholder="Search airport..."
            icon={<Plane className="w-4 h-4" />}
          />
        </div>
        <button
          onClick={handleSearch}
          disabled={!airportCode || isPending}
          className={cn(
            "px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors",
            "bg-primary text-primary-foreground",
            "disabled:opacity-40 disabled:cursor-not-allowed"
          )}
        >
          Search
        </button>
      </div>

      <main className="flex-1 max-w-5xl mx-auto w-full px-6 pb-10 space-y-4">

        {/* Loading */}
        {isPending && (
          <div className="space-y-2">
            {Array.from({ length: 8 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        )}

        {/* Error */}
        {error && !isPending && (
          <div className="rounded-xl border border-border/50 bg-card p-5 flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-destructive shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-destructive">Search failed</p>
              <p className="text-xs text-muted mt-0.5">{error.message}</p>
            </div>
          </div>
        )}

        {data && !isPending && (
          <>
            {/* Spider map */}
            <div className="rounded-xl border border-border/50 bg-card overflow-hidden">
              <button
                onClick={() => setMapOpen(o => !o)}
                className="w-full flex items-center justify-between px-4 py-3 text-sm font-semibold hover:bg-primary/5 transition-colors"
                style={{ color: "var(--foreground)" }}
              >
                <span>Route Map</span>
                {mapOpen
                  ? <ChevronUp className="w-4 h-4 text-muted-foreground" />
                  : <ChevronDown className="w-4 h-4 text-muted-foreground" />}
              </button>
              {mapOpen && (
                <div style={{ borderTop: "1px solid var(--border)" }}>
                  <SpiderMap
                    hubCode={data.airport}
                    hubLat={data.hubLat}
                    hubLon={data.hubLon}
                    departures={data.departures}
                    arrivals={data.arrivals}
                  />
                </div>
              )}
            </div>

            {/* Filters */}
            <div className="flex flex-wrap items-center gap-3">
              {/* Time of day */}
              {TIME_SLOTS.map((slot, i) => (
                <button
                  key={slot.label}
                  onClick={() => toggleSlot(i)}
                  className={cn(
                    "px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors border",
                    selectedSlots.has(i)
                      ? "bg-primary/20 text-primary border-primary/40"
                      : "text-muted-foreground border-border hover:bg-primary/10"
                  )}
                >
                  {slot.label}
                </button>
              ))}

              {/* Airline dropdown */}
              <select
                value={selectedAirline}
                onChange={e => setSelectedAirline(e.target.value)}
                className={cn(
                  "px-3 py-1.5 rounded-xl text-xs font-semibold border transition-colors",
                  "bg-card text-foreground border-border",
                  "hover:bg-primary/10 cursor-pointer"
                )}
              >
                <option value="">All airlines</option>
                {allAirlines.map(a => (
                  <option key={a} value={a}>{a}</option>
                ))}
              </select>
            </div>

            {/* Tabs */}
            <div role="tablist" aria-label="Flight board" className="flex gap-2">
              {(["departures", "arrivals"] as Tab[]).map(tab => (
                <button
                  key={tab}
                  role="tab"
                  aria-selected={activeTab === tab}
                  onClick={() => setActiveTab(tab)}
                  className={cn(
                    "px-4 py-2 rounded-xl text-sm font-semibold capitalize transition-colors",
                    activeTab === tab
                      ? "bg-primary/20 text-primary"
                      : "text-muted-foreground hover:bg-primary/10"
                  )}
                >
                  {tab} ({applyFilters(data[tab]).length})
                </button>
              ))}
            </div>

            {/* Table */}
            <div role="tabpanel" aria-label={activeTab}>
              {flights.length === 0 ? (
                <div className="rounded-xl border border-border/50 bg-card p-10 flex flex-col items-center gap-3 text-center">
                  <Plane className="w-10 h-10 text-muted" />
                  <p className="text-sm text-muted-foreground">No flights match the current filters.</p>
                </div>
              ) : (
                <div className="rounded-xl border border-border/50 bg-card shadow-xl overflow-x-auto">
                  <table className="w-full text-sm">
                    <caption className="sr-only">
                      {activeTab === "departures"
                        ? `Departures from ${airportCity || airportCode}`
                        : `Arrivals to ${airportCity || airportCode}`}
                    </caption>
                    <thead>
                      <tr
                        className="text-left text-xs font-semibold text-muted-foreground"
                        style={{ borderBottom: "1px solid var(--border)" }}
                      >
                        <th className="px-4 py-3">Flight</th>
                        <th className="px-4 py-3">Airline</th>
                        <th className="px-4 py-3 hidden md:table-cell">Aircraft</th>
                        <th className="px-4 py-3">From</th>
                        <th className="px-4 py-3">To</th>
                        <th className="px-4 py-3">Departs</th>
                        <th className="px-4 py-3">Arrives</th>
                        <th className="px-4 py-3 hidden sm:table-cell">Duration</th>
                        <th className="px-4 py-3">Price</th>
                      </tr>
                    </thead>
                    <tbody>
                      {flights.map(f => (
                        <tr
                          key={f.flightNumber}
                          className="hover:bg-primary/10 transition-colors"
                          style={{ borderBottom: "1px solid color-mix(in srgb, var(--border) 50%, transparent)" }}
                        >
                          <td className="px-4 py-3 font-mono font-medium">{f.flightNumber}</td>
                          <td className="px-4 py-3">{f.airlineName}</td>
                          <td className="px-4 py-3 hidden md:table-cell text-muted-foreground">{f.aircraftName}</td>
                          <td className="px-4 py-3">
                            {f.originCity}{" "}
                            <span className="text-xs text-muted-foreground">{f.origin}</span>
                          </td>
                          <td className="px-4 py-3">
                            {f.destinationCity}{" "}
                            <span className="text-xs text-muted-foreground">{f.destination}</span>
                          </td>
                          <td className="px-4 py-3 tabular-nums">{f.departureTime}</td>
                          <td className="px-4 py-3 tabular-nums">{f.arrivalTime}</td>
                          <td className="px-4 py-3 hidden sm:table-cell text-muted-foreground">
                            {formatDuration(f.durationMinutes)}
                          </td>
                          <td className="px-4 py-3 font-medium">${f.price}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
