/*
 * page.tsx - FlightSearch main page.
 *
 * Composes FlightSearchForm, FlightCombinationCard, and FlightFilterSort.
 * Search is triggered via React Query's useMutation; results are sorted
 * client-side since the multicity endpoint does not accept a sortBy param.
 *
 * Layout: the search form spans the full container width at the top; results
 * appear below it in a single-column flow.
 *
 * API called: GET /api/flights/multicity?from=<code>&destinations=<codes>
 */

"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { Plane, AlertCircle } from "lucide-react";
import { FlightSearchForm, type SearchFormValues } from "@/components/FlightSearchForm";
import { FlightCombinationCard, type BackendRoute } from "@/components/FlightCombinationCard";
import { FlightFilterSort } from "@/components/FlightFilterSort";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Skeleton } from "@/components/ui/Skeleton";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface SearchResult {
  from: string;
  routes: BackendRoute[];
}

async function fetchRoutes(values: SearchFormValues): Promise<SearchResult> {
  const from = values.homeAirport.code.toUpperCase();
  const destinations = values.destinations
    .map((d) => d.code.toUpperCase())
    .join(",");

  const res = await fetch(
    `${API_URL}/api/flights/multicity?from=${from}&destinations=${destinations}`
  );

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? "Failed to search routes");
  }

  return res.json();
}

function sortedRoutes(
  routes: BackendRoute[],
  sortBy: "price" | "duration"
): BackendRoute[] {
  return [...routes].sort((a, b) => {
    if (sortBy === "price") {
      return a.cheapestTotalPrice - b.cheapestTotalPrice;
    }
    // duration: sum of the cheapest flight per leg
    const dur = (route: BackendRoute) =>
      route.legs.reduce((sum, leg) => {
        const best = leg.flights.find((f) => f.cheapest) ?? leg.flights[0];
        return sum + (best?.durationMinutes ?? 0);
      }, 0);
    return dur(a) - dur(b);
  });
}

export default function Home() {
  const [sortBy, setSortBy] = useState<"price" | "duration">("price");

  const { mutate, data, isPending, error, reset } = useMutation<
    SearchResult,
    Error,
    SearchFormValues
  >({
    mutationFn: fetchRoutes,
  });

  const routes = data ? sortedRoutes(data.routes, sortBy) : [];

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: "var(--background)" }}
    >
      {/* Header */}
      <header className="shrink-0 px-6 py-4 border-b"
        style={{ borderColor: "var(--border)" }}>
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <Plane className="w-6 h-6 text-primary" />
            <span
              className="text-lg font-semibold"
              style={{ fontFamily: "var(--font-display)" }}
            >
              FlightSearch
            </span>
          </div>
          <ThemeToggle />
        </div>
      </header>

      {/* Page body */}
      <div className="flex-1 max-w-4xl mx-auto w-full px-6">
        <div className="flex flex-col gap-6">

          {/* Search form — full width */}
          <div className="pt-6">
            <FlightSearchForm
              onSearch={(values) => {
                reset();
                setSortBy("price");
                mutate(values);
              }}
              isLoading={isPending}
            />
          </div>

          {/* Results */}
          <div className="pb-6 space-y-4">
            <AnimatePresence mode="wait">

              {/* Loading skeletons */}
              {isPending && (
                <motion.div
                  key="loading"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="space-y-3"
                >
                  <Skeleton className="h-16 w-full" />
                  <Skeleton className="h-16 w-full" />
                  <Skeleton className="h-16 w-full" />
                </motion.div>
              )}

              {/* Error state */}
              {error && !isPending && (
                <motion.div
                  key="error"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className="glass p-5 flex items-start gap-3"
                >
                  <AlertCircle className="w-5 h-5 text-destructive shrink-0 mt-0.5" />
                  <div>
                    <p className="text-sm font-medium text-destructive">Search failed</p>
                    <p className="text-xs text-muted mt-0.5">{error.message}</p>
                  </div>
                </motion.div>
              )}

              {/* Results */}
              {data && !isPending && (
                <motion.div
                  key="results"
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.3 }}
                  className="space-y-4"
                >
                  {routes.length === 0 ? (
                    /* Empty state */
                    <div className="glass p-10 flex flex-col items-center gap-3 text-center">
                      <Plane className="w-10 h-10 text-muted" />
                      <p className="text-sm text-muted">
                        No routes found for these destinations.
                      </p>
                    </div>
                  ) : (
                    <>
                      <FlightFilterSort
                        resultCount={routes.length}
                        sortBy={sortBy}
                        onSortChange={setSortBy}
                      />

                      {routes.map((route, index) => (
                        <FlightCombinationCard
                          key={route.airports.join("-")}
                          route={route}
                          rank={index + 1}
                          defaultOpen={index === 0}
                        />
                      ))}
                    </>
                  )}
                </motion.div>
              )}

            </AnimatePresence>
          </div>

        </div>
      </div>
    </div>
  );
}
