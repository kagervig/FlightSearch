/*
 * RouteBuilderContent.tsx - Hop-by-hop flight explorer.
 *
 * Starting from a home airport, the user picks one cheap onward flight at a
 * time to build a custom journey — up to 10 stops — and then closes the loop
 * home or keeps it one way.
 *
 * Layout: sticky header (home + date), then a two-column panel (left: journey
 * details / right: RouteMap). On mobile the map moves to the top at a fixed
 * non-interactive height and the closing action sticks to the bottom.
 */

"use client";

import { useReducer, useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plane, AlertCircle, ArrowRight, RotateCcw } from "lucide-react";
import { journeyReducer, initialState, deriveJourney } from "@/lib/journeyReducer";
import { AirportAutocomplete } from "@/components/AirportAutocomplete";
import { RouteMap } from "@/components/RouteMap";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Skeleton } from "@/components/ui/Skeleton";
import { cn } from "@/lib/utils";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// --- API response shapes ---

interface ApiAirport {
  code: string;
  city: string;
  latitude: number;
  longitude: number;
}

interface RouteSearchResult {
  flightNumber: string;
  origin: string;
  destination: string;
  destinationCity: string;
  price: number;
  airline: string;
  departureTime: string;
  distanceKm: number;
  durationMinutes: number;
}

interface FlyHomeResult {
  direct: boolean;
  legs: RouteSearchResult[];
  totalPrice: number;
}

// --- Helper functions ---

function fmtPrice(p: number): string {
  return `£${p.toLocaleString()}`;
}

function fmtDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
}

function fmtDate(d: Date): string {
  return d.toLocaleDateString("en-GB", { day: "numeric", month: "short" });
}

// --- Main component ---

export function RouteBuilderContent() {
  const [homeCode, setHomeCode] = useState("");
  const [departureDate, setDepartureDate] = useState(
    () => new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
  );

  // Reducer is initialised with a placeholder; SET_HOME resets it once the
  // user picks a home airport.
  const [state, dispatch] = useReducer(journeyReducer, initialState("?"));

  const derived = useMemo(
    () => deriveJourney(state, new Date(departureDate)),
    [state, departureDate]
  );

  const { journey, closed } = state;
  const { hopCount, visitedSet, runningTotal, cumulativeDistanceKm, daysAway, backHomeDate, atCap } = derived;
  const currentCode = journey.at(-1)!.code;

  // --- Queries ---

  const airportsQuery = useQuery<ApiAirport[]>({
    queryKey: ["all-airports"],
    queryFn: async () => {
      const res = await fetch(`${API_URL}/api/airports`);
      if (!res.ok) throw new Error("Failed to load airports");
      return res.json();
    },
    staleTime: Infinity,
  });

  const excludeParam = [...visitedSet].join(",");

  const onwardQuery = useQuery<RouteSearchResult[]>({
    queryKey: ["onward-flights", currentCode, excludeParam],
    queryFn: async () => {
      const res = await fetch(
        `${API_URL}/api/routes/search?origin=${currentCode}&exclude=${excludeParam}`
      );
      if (!res.ok) throw new Error("Failed to load flights");
      return res.json();
    },
    enabled: !!homeCode && !closed && !atCap,
    staleTime: 60_000,
  });

  const flyHomeQuery = useQuery<FlyHomeResult>({
    queryKey: ["fly-home", currentCode, homeCode],
    queryFn: async () => {
      const res = await fetch(
        `${API_URL}/api/routes/home?from=${currentCode}&home=${homeCode}`
      );
      if (!res.ok) throw new Error("Failed to find route home");
      return res.json();
    },
    enabled: !!homeCode && hopCount > 0 && !closed && currentCode !== homeCode,
    staleTime: 60_000,
  });

  // Map coordinate lookup: backend uses "latitude"/"longitude", RouteMap expects "lat"/"lng"
  const mapCoords = useMemo(() => {
    if (!airportsQuery.data) return [];
    return airportsQuery.data.map((a) => ({
      code: a.code,
      lat: a.latitude,
      lng: a.longitude,
    }));
  }, [airportsQuery.data]);

  const journeyCodes = journey.map((h) => h.code);

  // Hops that get a nights stepper (all visited cities after home, excluding the
  // return-home leg at the end of a closed round trip)
  const nightHops = useMemo(() => {
    const hops = journey.slice(1);
    const trimmed =
      closed && hops.at(-1)?.code === homeCode ? hops.slice(0, -1) : hops;
    return trimmed.map((h, i) => ({ ...h, journeyIndex: i + 1 }));
  }, [journey, closed, homeCode]);

  // No-route-home: backend returns legs=[] when Dijkstra finds no path
  const noRouteHome =
    flyHomeQuery.isSuccess && flyHomeQuery.data.legs.length === 0 && !flyHomeQuery.data.direct;

  const flyHomePrice = flyHomeQuery.data?.totalPrice;
  const showClosingActions = !!homeCode && hopCount > 0 && !closed;

  // --- Handlers ---

  function handleHomeChange(code: string, _city: string) {
    void _city;
    setHomeCode(code);
    if (code.length === 3) {
      dispatch({ type: "SET_HOME", code });
    }
  }

  function handlePickFlight(flight: RouteSearchResult) {
    dispatch({
      type: "APPEND_HOP",
      code: flight.destination,
      price: flight.price,
      distanceKm: flight.distanceKm,
    });
  }

  function handleFlyHome() {
    if (!flyHomeQuery.data) return;
    const price = flyHomeQuery.data.totalPrice;
    const firstLeg = flyHomeQuery.data.legs[0];
    dispatch({ type: "FLY_HOME", price, distanceKm: firstLeg?.distanceKm });
  }

  function handleOneWay() {
    dispatch({ type: "ONE_WAY" });
  }

  function handleReset() {
    dispatch({ type: "RESET" });
  }

  // --- Render ---

  return (
    <div className="min-h-screen" style={{ background: "var(--background)" }}>
      {/* Sticky header: home airport selector + date + theme */}
      <header
        className="sticky top-0 z-30 border-b"
        style={{ borderColor: "var(--border)", background: "var(--card)" }}
      >
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center gap-3">
          <a href="/" className="flex items-center gap-2 text-sm font-semibold text-foreground shrink-0">
            <Plane className="w-4 h-4 text-primary" />
            <span className="hidden sm:inline">CityHopper</span>
          </a>

          <div className="flex-1 flex items-center gap-2 min-w-0 max-w-md">
            <div className="flex-1 min-w-0">
              <AirportAutocomplete
                value=""
                onChange={handleHomeChange}
                placeholder="Home airport"
                icon={<Plane className="w-4 h-4" />}
              />
            </div>
            <input
              type="date"
              value={departureDate}
              onChange={(e) => setDepartureDate(e.target.value)}
              className="h-9 px-3 rounded-lg border text-sm bg-card text-foreground shrink-0"
              style={{ borderColor: "var(--border)" }}
            />
          </div>

          <div className="ml-auto shrink-0">
            <ThemeToggle />
          </div>
        </div>
      </header>

      {/* Mobile map — non-interactive, fixed 300px */}
      <div
        className="md:hidden border-b"
        style={{ borderColor: "var(--border)" }}
      >
        <RouteMap
          journey={journeyCodes}
          airports={mapCoords}
          mapHeight={300}
          interactive={false}
        />
      </div>

      {/* Two-column layout */}
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row" style={{ minHeight: "calc(100vh - 57px)" }}>

        {/* ── Left panel ── */}
        <aside
          className="w-full md:w-[380px] shrink-0 md:border-r flex flex-col"
          style={{ borderColor: "var(--border)" }}
        >
          {!homeCode ? (
            /* Setup prompt */
            <div className="flex-1 flex flex-col items-center justify-center gap-3 p-10 text-center">
              <Plane className="w-10 h-10 text-muted" />
              <p className="text-sm font-medium text-foreground">Pick a home airport to start</p>
              <p className="text-xs text-muted max-w-[240px] leading-relaxed">
                Type your departure city above, then pick flights one hop at a time.
              </p>
            </div>
          ) : (
            <>
              {/* Trip header */}
              <div className="p-4 border-b" style={{ borderColor: "var(--border)" }}>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[11px] font-semibold uppercase tracking-widest text-muted">
                    {closed
                      ? `${hopCount} ${hopCount === 1 ? "city" : "cities"} · ${journey.at(-1)?.code === homeCode ? "round trip" : "one way"}`
                      : `Hop ${hopCount} / 10`}
                  </span>
                  <button
                    type="button"
                    onClick={handleReset}
                    className="flex items-center gap-1 text-xs text-muted hover:text-foreground transition-colors"
                  >
                    <RotateCcw className="w-3 h-3" />
                    Reset
                  </button>
                </div>

                {/* Breadcrumb */}
                <p className="text-sm tracking-wide leading-relaxed text-foreground">
                  {journey.map((h, i) => (
                    <span key={`${h.code}-${i}`}>
                      {i > 0 && <span className="text-muted mx-1">›</span>}
                      <span className={cn(h.code === currentCode && !closed && "text-primary font-semibold")}>
                        {h.code}
                      </span>
                    </span>
                  ))}
                </p>

                {/* 2×2 stat grid */}
                <div className="grid grid-cols-2 gap-x-6 gap-y-3 mt-3">
                  {[
                    ["Total", runningTotal > 0 ? fmtPrice(runningTotal) : "—"],
                    ["Distance", cumulativeDistanceKm > 0 ? `${cumulativeDistanceKm.toLocaleString()} km` : "—"],
                    ["Days away", daysAway > 0 ? `${daysAway} days` : "—"],
                    [
                      "Back home",
                      daysAway > 0 ? `${fmtDate(backHomeDate)}${closed ? " (booked)" : ""}` : "—",
                    ],
                  ].map(([label, value]) => (
                    <div key={label}>
                      <p className="text-[10px] font-semibold uppercase tracking-wider text-muted mb-0.5">
                        {label}
                      </p>
                      <p className="text-sm font-medium text-foreground">{value}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Nights per city */}
              {nightHops.length > 0 && !closed && (
                <div className="px-4 py-3 border-b" style={{ borderColor: "var(--border)" }}>
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-muted mb-2">
                    Nights per city
                  </p>
                  <div className="flex flex-col gap-2">
                    {nightHops.map((hop) => (
                      <div key={hop.code} className="flex items-center justify-between">
                        <span className="text-xs text-foreground">{hop.code}</span>
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() =>
                              dispatch({ type: "SET_NIGHTS", index: hop.journeyIndex, nights: hop.nights - 1 })
                            }
                            className="w-6 h-6 rounded border flex items-center justify-center text-muted hover:text-foreground hover:border-primary transition-colors text-xs leading-none"
                            style={{ borderColor: "var(--border)" }}
                            aria-label={`Decrease nights in ${hop.code}`}
                          >
                            −
                          </button>
                          <span className="w-12 text-center text-xs text-foreground">
                            {hop.nights} {hop.nights === 1 ? "nt" : "nts"}
                          </span>
                          <button
                            type="button"
                            onClick={() =>
                              dispatch({ type: "SET_NIGHTS", index: hop.journeyIndex, nights: hop.nights + 1 })
                            }
                            className="w-6 h-6 rounded border flex items-center justify-center text-muted hover:text-foreground hover:border-primary transition-colors text-xs leading-none"
                            style={{ borderColor: "var(--border)" }}
                            aria-label={`Increase nights in ${hop.code}`}
                          >
                            +
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Flight list header */}
              <div
                className="px-4 py-2 border-b flex items-center justify-between"
                style={{ borderColor: "var(--border)" }}
              >
                <span className="text-[11px] font-semibold uppercase tracking-wider text-muted">
                  {closed
                    ? "Trip closed"
                    : atCap
                    ? "City limit reached"
                    : `Cheapest out of ${currentCode}`}
                </span>
                {!closed && !atCap && (
                  <span className="text-[10px] text-muted">Price ↑</span>
                )}
              </div>

              {/* Flight list or booked itinerary */}
              <div className="flex-1 overflow-y-auto">
                {closed ? (
                  /* Booked itinerary */
                  <>
                    {journey.slice(1).map((hop, i) => {
                      const from = journey[i];
                      return (
                        <div
                          key={`${from.code}-${hop.code}-${i}`}
                          className="flex items-center justify-between px-4 py-3 border-b text-sm"
                          style={{ borderColor: "var(--border)" }}
                        >
                          <div>
                            <p className="font-medium text-foreground">
                              {from.code} → {hop.code}
                            </p>
                          </div>
                          <p className="text-sm font-medium text-foreground shrink-0 ml-4">
                            {hop.price > 0 ? fmtPrice(hop.price) : "—"}
                          </p>
                        </div>
                      );
                    })}
                    <div
                      className="flex items-center justify-between px-4 py-3 border-t"
                      style={{ borderColor: "var(--border)" }}
                    >
                      <span className="text-sm font-semibold text-foreground">
                        {fmtPrice(runningTotal)} total
                      </span>
                      <button
                        type="button"
                        onClick={handleReset}
                        className="text-xs border rounded-lg px-3 py-1.5 text-muted hover:text-foreground hover:border-primary transition-colors"
                        style={{ borderColor: "var(--border)" }}
                      >
                        Start a new chain
                      </button>
                    </div>
                  </>
                ) : atCap ? (
                  <div className="p-6 text-center text-sm text-muted">
                    Choose fly home or one way below.
                  </div>
                ) : onwardQuery.isPending ? (
                  <div className="p-3 flex flex-col gap-2">
                    {[...Array(5)].map((_, i) => (
                      <Skeleton key={i} className="h-14 w-full" />
                    ))}
                  </div>
                ) : onwardQuery.isError ? (
                  <div className="p-4 flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 text-destructive shrink-0 mt-0.5" />
                    <p className="text-xs text-muted">Could not load flights. Please try again.</p>
                  </div>
                ) : (onwardQuery.data ?? []).length === 0 ? (
                  <div className="p-8 flex flex-col items-center gap-3 text-center">
                    <Plane className="w-8 h-8 text-muted" />
                    <p className="text-sm text-muted">No onward flights found from {currentCode}.</p>
                  </div>
                ) : (
                  (onwardQuery.data ?? []).map((flight) => (
                    <button
                      key={flight.flightNumber}
                      type="button"
                      onClick={() => handlePickFlight(flight)}
                      className="w-full flex items-center gap-2 px-4 py-3 border-t text-left hover:bg-card/60 transition-colors group"
                      style={{ borderColor: "var(--border)" }}
                    >
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-foreground tracking-wide">
                          {flight.destination}
                          <span className="text-muted font-normal"> · {flight.destinationCity}</span>
                        </p>
                        <p className="text-[11px] text-muted mt-0.5">
                          {flight.airline} {flight.flightNumber}
                          {" · "}
                          {fmtDuration(flight.durationMinutes)}
                          {" · "}
                          {flight.distanceKm.toLocaleString()} km
                        </p>
                      </div>
                      <span className="text-sm font-semibold text-foreground shrink-0">
                        {fmtPrice(flight.price)}
                      </span>
                      <ArrowRight className="w-4 h-4 text-primary opacity-25 group-hover:opacity-100 transition-opacity shrink-0" />
                    </button>
                  ))
                )}
              </div>

              {/* Closing actions (open trip, ≥1 hop) */}
              {showClosingActions && (
                <div className="p-4 border-t flex flex-col gap-2" style={{ borderColor: "var(--border)" }}>
                  {noRouteHome ? (
                    <div className="flex items-start gap-2 text-xs text-muted">
                      <AlertCircle className="w-4 h-4 text-destructive shrink-0 mt-0.5" />
                      No route home from {currentCode} to {homeCode}.
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={handleFlyHome}
                        disabled={flyHomeQuery.isPending || !flyHomeQuery.data || noRouteHome}
                        className="flex-1 h-10 rounded-xl border border-primary text-primary text-sm font-medium hover:bg-primary/10 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                      >
                        {flyHomeQuery.isPending
                          ? "Finding route…"
                          : flyHomePrice != null
                          ? `Fly home to ${homeCode} · ${fmtPrice(flyHomePrice)}`
                          : `Fly home to ${homeCode}`}
                      </button>
                      <button
                        type="button"
                        onClick={handleOneWay}
                        className="h-10 px-4 rounded-xl border text-sm text-muted hover:text-foreground hover:border-primary/60 transition-colors"
                        style={{ borderColor: "var(--border)" }}
                      >
                        One way
                      </button>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </aside>

        {/* ── Right panel: map ── */}
        <div className="hidden md:flex flex-1 flex-col relative p-4">
          <RouteMap journey={journeyCodes} airports={mapCoords} />

          {/* 10-city cap modal — overlays the map only */}
          {atCap && !closed && (
            <div
              className="absolute inset-4 flex items-center justify-center rounded-2xl"
              style={{ background: "rgba(15, 17, 28, 0.78)" }}
            >
              <div
                className="mx-4 w-full max-w-sm p-6 rounded-2xl shadow-2xl"
                style={{
                  background: "var(--card)",
                  border: "1px solid var(--border)",
                  boxShadow: "0 16px 40px rgba(0,0,0,0.65)",
                }}
              >
                <h2 className="text-lg font-semibold text-foreground mb-2">Ten cities reached</h2>
                <p className="text-sm text-muted leading-relaxed mb-5">
                  That is the limit for one hop chain. Close the loop back to{" "}
                  <strong className="text-foreground">{homeCode}</strong>
                  {flyHomePrice != null && ` for ${fmtPrice(flyHomePrice)}`}, or stop here and
                  keep it one way.
                </p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleFlyHome}
                    disabled={flyHomeQuery.isPending || !flyHomeQuery.data}
                    className="flex-1 h-10 rounded-xl border border-primary text-primary text-sm font-medium hover:bg-primary/10 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                  >
                    {flyHomePrice != null
                      ? `Fly home to ${homeCode} · ${fmtPrice(flyHomePrice)}`
                      : "Finding route home…"}
                  </button>
                  <button
                    type="button"
                    onClick={handleOneWay}
                    className="h-10 px-4 rounded-xl border text-sm text-muted hover:text-foreground transition-colors"
                    style={{ borderColor: "var(--border)" }}
                  >
                    Finish one way
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Mobile sticky closing action */}
        {showClosingActions && (
          <div
            className="md:hidden sticky bottom-0 p-4 border-t"
            style={{ borderColor: "var(--border)", background: "var(--card)" }}
          >
            {noRouteHome ? (
              <div className="flex items-center gap-2 text-xs text-muted justify-center">
                <AlertCircle className="w-4 h-4 text-destructive shrink-0" />
                No route home from {currentCode} to {homeCode}.
              </div>
            ) : (
              <button
                type="button"
                onClick={handleFlyHome}
                disabled={flyHomeQuery.isPending || !flyHomeQuery.data}
                className="w-full h-12 rounded-xl border border-primary text-primary text-sm font-medium hover:bg-primary/10 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                {flyHomePrice != null
                  ? `Fly home to ${homeCode} · ${fmtPrice(flyHomePrice)}`
                  : "Finding route home…"}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
