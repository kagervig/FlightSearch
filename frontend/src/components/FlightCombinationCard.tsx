"use client";

/**
 * Collapsible card displaying one route returned by the multicity search.
 * Collapsed: summary row with rank, city route, total price, and total duration.
 * Expanded: per-leg breakdown with best flight details and alternatives.
 */

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown, ChevronUp, Plane } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { cn, formatDuration } from "@/lib/utils";

interface FlightOption {
  flightNumber: string;
  price: number;
  departureTime: string; // "HH:MM:SS" from Java LocalTime
  arrivalTime: string;
  durationMinutes: number;
  cheapest: boolean;
}

interface Leg {
  from: string;
  to: string;
  fromCity: string;
  fromCountry: string;
  toCity: string;
  toCountry: string;
  flights: FlightOption[];
}

export interface BackendRoute {
  airports: string[];
  cheapestTotalPrice: number;
  legs: Leg[];
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

function totalDuration(route: BackendRoute): number {
  return route.legs.reduce((sum, leg) => {
    const best = leg.flights.find((f) => f.cheapest) ?? leg.flights[0];
    return sum + (best?.durationMinutes ?? 0);
  }, 0);
}

export function FlightCombinationCard({
  route,
  rank,
  defaultOpen = false,
}: FlightCombinationCardProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  // tracks which legs have their alternative flights shown
  const [expandedLegs, setExpandedLegs] = useState<Record<number, boolean>>({});

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
      <button
        type="button"
        onClick={() => setIsOpen((o) => !o)}
        className="w-full flex items-center gap-3 p-5 text-left"
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

        {/* Toggle icon */}
        <span className="shrink-0 text-muted">
          {isOpen ? (
            <ChevronUp className="w-4 h-4" />
          ) : (
            <ChevronDown className="w-4 h-4" />
          )}
        </span>
      </button>

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
            <div className="px-5 pb-5 space-y-3 border-t border-border/30 pt-4">
              {route.legs.map((leg, legIndex) => {
                const best =
                  leg.flights.find((f) => f.cheapest) ?? leg.flights[0];
                const hasAlternatives = leg.flights.length > 1;
                const legExpanded = expandedLegs[legIndex] ?? false;

                return (
                  <div
                    key={legIndex}
                    className="rounded-xl border border-border/40 bg-background/30 overflow-hidden"
                  >
                    {/* Leg header — best flight summary */}
                    <div className="p-4">
                      <div className="flex items-start justify-between gap-3 mb-2">
                        <div>
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
                          {formatTime(best.departureTime)} → {formatTime(best.arrivalTime)}
                        </span>
                        <span>{formatDuration(best.durationMinutes)}</span>
                        {best.cheapest && (
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
                                        {formatTime(f.departureTime)} →{" "}
                                        {formatTime(f.arrivalTime)} ·{" "}
                                        {formatDuration(f.durationMinutes)}
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
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
