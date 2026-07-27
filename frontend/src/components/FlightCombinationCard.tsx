"use client";

/**
 * Collapsible card displaying one route returned by the multicity search.
 * Collapsed: summary row with rank, city route, total price, and total duration.
 * Expanded: per-leg breakdown with best flight details and alternatives.
 */

import { useState, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown, ChevronUp, Plane, AlertTriangle, X } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { RouteMap } from "@/components/RouteMap";
import { cn, formatDuration } from "@/lib/utils";

interface FlightOption {
  flightNumber: string;
  price: number;
  departureTime: string; // "HH:MM:SS" from Java LocalTime
  arrivalTime: string;
  durationMinutes: number;
  cheapest: boolean;
  airlineName?: string;
  aircraftName?: string;
}

interface Leg {
  from: string;
  to: string;
  fromCity: string;
  fromCountry: string;
  fromLat: number;
  fromLon: number;
  toCity: string;
  toCountry: string;
  toLat: number;
  toLon: number;
  flights: FlightOption[];
  date?: string;
  isConnection?: boolean;
  connectionMinutes?: number;
  isOvernightConnection?: boolean;
}

export interface BackendRoute {
  airports: string[];
  cheapestTotalPrice: number;
  legs: Leg[];
  hasConnections?: boolean;
}

interface FlightCombinationCardProps {
  route: BackendRoute;
  rank: number;
  defaultOpen?: boolean;
}

// Java LocalTime serialises as "HH:MM:SS" — strip the seconds for display
function formatTime(time: string): string {
  return time.slice(0, 5);
}

function formatLayover(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return "";
  // Append time to avoid UTC offset shifting the date
  const d = new Date(dateStr + "T12:00:00");
  return d.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" });
}

function isEarlyMorning(time: string): boolean {
  return parseInt(time.slice(0, 2), 10) < 7;
}

function EarlyTimeTag({ time }: { time: string }) {
  if (!isEarlyMorning(time)) return <>{formatTime(time)}</>;
  return (
    <span className="relative inline-block group/early cursor-help">
      <span className="text-red-400 font-medium">{formatTime(time)}</span>
      <span className="pointer-events-none absolute bottom-full left-0 mb-1.5 max-w-[180px] rounded bg-foreground px-2 py-1 text-xs text-background opacity-0 transition-opacity group-hover/early:opacity-100 z-50">
        This is an early morning flight!
      </span>
    </span>
  );
}

function totalDuration(route: BackendRoute): number {
  return route.legs.reduce((sum, leg) => {
    const best = leg.flights.find((f) => f.cheapest) ?? leg.flights[0];
    const layover = leg.isConnection ? (leg.connectionMinutes ?? 0) : 0;
    return sum + (best?.durationMinutes ?? 0) + layover;
  }, 0);
}

// For a connection group starting at startIndex, returns the connecting airports
// and the intended destination (the first non-connection leg's destination).
function getConnectionGroupInfo(
  legs: Leg[],
  startIndex: number
): { viaAirports: string[]; intendedDest: string } {
  const viaAirports: string[] = [];
  let i = startIndex;
  while (i < legs.length && legs[i].isConnection) {
    viaAirports.push(legs[i].to);
    i++;
  }
  const intendedDest = i < legs.length ? legs[i].to : legs[legs.length - 1].to;
  return { viaAirports, intendedDest };
}

export function FlightCombinationCard({
  route,
  rank,
  defaultOpen = false,
}: FlightCombinationCardProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const [bookingModalOpen, setBookingModalOpen] = useState(false);
  // tracks which legs have their alternative flights shown
  const [expandedLegs, setExpandedLegs] = useState<Record<number, boolean>>({});

  const airportCoords = useMemo(() => {
    const map = new Map<string, { code: string; lat: number; lng: number }>();
    route.legs.forEach((leg) => {
      map.set(leg.from, { code: leg.from, lat: leg.fromLat, lng: leg.fromLon });
      map.set(leg.to, { code: leg.to, lat: leg.toLat, lng: leg.toLon });
    });
    return Array.from(map.values());
  }, [route.legs]);

  function toggleLeg(index: number) {
    setExpandedLegs((prev) => ({ ...prev, [index]: !prev[index] }));
  }

  const cityRoute = route.airports.join(" → ");
  const duration = totalDuration(route);

  return (
    <div
      className={cn(
        "glass transition-all duration-300",
        isOpen && "ring-2 ring-primary/20 shadow-xl shadow-primary/10"
      )}
    >
      {/* Collapsed summary row */}
      <div
        role="button"
        tabIndex={0}
        onClick={() => setIsOpen((o) => !o)}
        onKeyDown={(e) => e.key === "Enter" || e.key === " " ? setIsOpen((o) => !o) : undefined}
        className="w-full flex items-center gap-3 p-5 text-left cursor-pointer"
      >
        {/* Rank badge */}
        <Badge className="shrink-0 text-sm px-3 py-1">#{rank}</Badge>

        {/* City route */}
        <span className="flex-1 text-sm font-medium truncate">{cityRoute}</span>

        {/* Duration */}
        <span className="shrink-0 text-xs text-muted hidden sm:block">
          {formatDuration(duration)}
        </span>

        {/* Price */}
        <span className="shrink-0 font-bold text-primary text-base">
          ${route.cheapestTotalPrice}
        </span>

        {/* Book button */}
        <Button
          className="shrink-0 px-3 py-1.5 text-xs"
          onClick={(e) => {
            e.stopPropagation();
            setBookingModalOpen(true);
          }}
        >
          Book
        </Button>

        {/* Toggle icon */}
        <span className="shrink-0 text-muted">
          {isOpen ? (
            <ChevronUp className="w-4 h-4" />
          ) : (
            <ChevronDown className="w-4 h-4" />
          )}
        </span>
      </div>

      {/* Expanded leg details */}
      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div
            key="details"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.25 }}
            className="overflow-hidden"
          >
            <div className="px-5 pb-5 space-y-4 border-t border-border/30 pt-4">
              <RouteMap journey={route.airports} airports={airportCoords} />
              {route.legs.map((leg, legIndex) => {
                const best =
                  leg.flights.find((f) => f.cheapest) ?? leg.flights[0];
                const hasAlternatives = leg.flights.length > 1;
                const legExpanded = expandedLegs[legIndex] ?? false;

                const prevLeg = legIndex > 0 ? route.legs[legIndex - 1] : null;
                // First sub-leg of a connection chain (e.g. LHR→BOG when LHR→GYE has no direct)
                const isFirstConnectionHop =
                  !!leg.isConnection && !prevLeg?.isConnection;
                // Any leg that follows a connection sub-leg gets a layover banner
                const showLayoverBanner = !!prevLeg?.isConnection;

                const connGroupInfo = isFirstConnectionHop
                  ? getConnectionGroupInfo(route.legs, legIndex)
                  : null;

                return (
                  <div key={legIndex}>
                    {/* "No direct flight" notice — shown once before each connection group */}
                    {isFirstConnectionHop && connGroupInfo && (
                      <div className="flex items-center gap-2 px-2 py-1.5 mb-1">
                        <div className="flex-1 border-t border-dashed border-border/50" />
                        <span className="flex items-center gap-1.5 text-xs font-medium px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-400">
                          <AlertTriangle className="w-3 h-3 shrink-0" />
                          No direct {leg.from} → {connGroupInfo.intendedDest} · Connecting via{" "}
                          {connGroupInfo.viaAirports.join(", ")}
                        </span>
                        <div className="flex-1 border-t border-dashed border-border/50" />
                      </div>
                    )}

                    {/* Layover banner — shown before each leg that follows a connection hop */}
                    {showLayoverBanner && (
                      <div className="flex items-center gap-2 px-2 py-1.5 mb-1">
                        <div className="flex-1 border-t border-dashed border-border/50" />
                        <span
                          className={cn(
                            "text-xs font-medium px-2 py-0.5 rounded-full",
                            prevLeg?.isOvernightConnection
                              ? "bg-amber-500/15 text-amber-400"
                              : "bg-muted/30 text-muted"
                          )}
                        >
                          {prevLeg?.isOvernightConnection ? "Overnight · " : ""}
                          {prevLeg?.connectionMinutes != null
                            ? formatLayover(prevLeg.connectionMinutes)
                            : ""}{" "}
                          layover at {leg.from}
                        </span>
                        <div className="flex-1 border-t border-dashed border-border/50" />
                      </div>
                    )}

                    <div
                      className={cn(
                        "rounded-xl border overflow-hidden",
                        leg.isConnection
                          ? "border-border/25 bg-background/20 ml-4"
                          : "border-border/40 bg-background/30"
                      )}
                    >
                      {/* Leg header — best flight summary */}
                      <div className="p-4">
                        <div className="flex items-start justify-between gap-3 mb-2">
                          <div>
                            {/* Date */}
                            {leg.date && (
                              <div className="text-xs text-muted mb-0.5">
                                {formatDate(leg.date)}
                              </div>
                            )}
                            <div className="flex items-center gap-2 text-sm font-medium">
                              <Plane className="w-3.5 h-3.5 text-primary shrink-0" />
                              <span>
                                {leg.fromCity || leg.from}
                                <span className="text-muted mx-1">→</span>
                                {leg.toCity || leg.to}
                              </span>
                            </div>
                            <div className="text-xs text-muted mt-0.5">
                              {leg.from} → {leg.to}
                            </div>
                          </div>

                          <Badge variant="green" className="shrink-0">
                            ${best.price}
                          </Badge>
                        </div>

                        {/* Best flight details */}
                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                          <span className="font-medium text-foreground">
                            {best.flightNumber}
                          </span>
                          <span>
                            <EarlyTimeTag time={best.departureTime} />
                            {" → "}
                            {formatTime(best.arrivalTime)}
                          </span>
                          <span>{formatDuration(best.durationMinutes)}</span>
                          {best.airlineName && <span>{best.airlineName}</span>}
                          {best.aircraftName && <span className="text-muted/70">{best.aircraftName}</span>}
                          {best.cheapest && leg.flights.length > 1 && (
                            <Badge variant="default" className="text-xs">
                              Cheapest
                            </Badge>
                          )}
                        </div>
                      </div>

                      {/* Alternatives toggle */}
                      {hasAlternatives && (
                        <>
                          <button
                            type="button"
                            onClick={() => toggleLeg(legIndex)}
                            className="w-full px-4 py-2 border-t border-border/30 text-xs text-muted hover:text-foreground hover:bg-primary/5 transition-colors flex items-center gap-1"
                          >
                            {legExpanded ? (
                              <ChevronUp className="w-3 h-3" />
                            ) : (
                              <ChevronDown className="w-3 h-3" />
                            )}
                            {legExpanded
                              ? "Hide alternatives"
                              : `${leg.flights.length - 1} other option${leg.flights.length > 2 ? "s" : ""}`}
                          </button>

                          <AnimatePresence initial={false}>
                            {legExpanded && (
                              <motion.div
                                key="alts"
                                initial={{ opacity: 0, height: 0 }}
                                animate={{ opacity: 1, height: "auto" }}
                                exit={{ opacity: 0, height: 0 }}
                                transition={{ duration: 0.2 }}
                                className="overflow-hidden"
                              >
                                <div className="px-4 pb-3 space-y-2">
                                  {leg.flights
                                    .filter((f) => !f.cheapest)
                                    .map((f, fi) => (
                                      <div
                                        key={fi}
                                        className="rounded-lg border border-border/30 bg-background/20 p-3"
                                      >
                                        <div className="flex items-center justify-between mb-1">
                                          <span className="text-xs font-medium">
                                            {f.flightNumber}
                                          </span>
                                          <span className="text-xs font-semibold text-foreground">
                                            ${f.price}
                                          </span>
                                        </div>
                                        <div className="text-xs text-muted">
                                          <EarlyTimeTag time={f.departureTime} />
                                          {" → "}
                                          {formatTime(f.arrivalTime)} ·{" "}
                                          {formatDuration(f.durationMinutes)}
                                          {f.airlineName && ` · ${f.airlineName}`}
                                          {f.aircraftName && ` · ${f.aircraftName}`}
                                        </div>
                                      </div>
                                    ))}
                                </div>
                              </motion.div>
                            )}
                          </AnimatePresence>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Booking unavailable modal */}
      <AnimatePresence>
        {bookingModalOpen && (
          <motion.div
            key="booking-modal-backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
            onClick={() => setBookingModalOpen(false)}
          >
            <motion.div
              key="booking-modal"
              initial={{ opacity: 0, scale: 0.95, y: 8 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 8 }}
              transition={{ duration: 0.2 }}
              className="glass rounded-2xl p-6 max-w-sm w-full shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-start justify-between gap-4 mb-3">
                <h2 className="text-base font-semibold">Booking Coming Soon</h2>
                <button
                  type="button"
                  onClick={() => setBookingModalOpen(false)}
                  className="text-muted hover:text-foreground transition-colors"
                  aria-label="Close"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <p className="text-sm text-muted">
                Sorry, we can&apos;t facilitate bookings at this time, but we&apos;re working to
                bring it to you soon.
              </p>
              <Button
                className="mt-5 w-full"
                onClick={() => setBookingModalOpen(false)}
              >
                Got it
              </Button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
