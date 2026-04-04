/*
 * page.tsx - CityHopper main page.
 *
 * Zone 1 (above the fold): full-screen hero with headline copy and search panel.
 * Zone 2 (below the fold): trust-building landing content shown when no search
 * has been performed. Replaced by results once the user searches.
 *
 * API called: GET /api/flights/multicity?from=<code>&destinations=<codes>
 */

"use client";

import { useState, useEffect, useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { Plane, AlertCircle, ChevronDown } from "lucide-react";
import { FlightSearchForm, type SearchFormValues } from "@/components/FlightSearchForm";
import { FlightCombinationCard, type BackendRoute } from "@/components/FlightCombinationCard";
import { FlightFilterSort } from "@/components/FlightFilterSort";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Skeleton } from "@/components/ui/Skeleton";
import { ProblemSection } from "@/components/ProblemSection";
import { HowItWorksSection } from "@/components/HowItWorksSection";
import { ComparisonSection } from "@/components/ComparisonSection";
import { FinalCTASection } from "@/components/FinalCTASection";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface SearchResult {
  from: string;
  routes: BackendRoute[];
}

async function fetchRoutes(values: SearchFormValues): Promise<SearchResult> {
  const from = values.homeAirport.code.toUpperCase();
  const destinations = values.destinations.map((d) => d.code.toUpperCase()).join(",");
  const days = values.destinations.map((d) => d.days).join(",");

  const res = await fetch(
    `${API_URL}/api/flights/multicity?from=${from}&destinations=${destinations}&departureDate=${values.departureDate}&daysAtEachDestination=${days}&optimizeBy=${values.optimizeBy}`
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
  const [showScrollCue, setShowScrollCue] = useState(false);
  const [hasScrolled, setHasScrolled] = useState(false);
  const resultsRef = useRef<HTMLDivElement>(null);

  const { mutate, data, isPending, error, reset } = useMutation<
    SearchResult,
    Error,
    SearchFormValues
  >({
    mutationFn: fetchRoutes,
  });

  // Only show loading animations if the request takes more than 1 second.
  const [showLoadingAnimation, setShowLoadingAnimation] = useState(false);
  useEffect(() => {
    if (!isPending) {
      setShowLoadingAnimation(false);
      return;
    }
    const timer = setTimeout(() => setShowLoadingAnimation(true), 1000);
    return () => clearTimeout(timer);
  }, [isPending]);

  // Scroll cue: fades in after 2 s, disappears once the user scrolls.
  useEffect(() => {
    const timer = setTimeout(() => setShowScrollCue(true), 2000);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    function onScroll() {
      if (window.scrollY > 80) setHasScrolled(true);
    }
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Scroll to results after they arrive.
  useEffect(() => {
    if (data && resultsRef.current) {
      resultsRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, [data]);

  const routes = data ? sortedRoutes(data.routes, sortBy) : [];
  const hasSearchState = !!(data || isPending || error);

  return (
    <div className="min-h-screen flex flex-col" style={{ background: "var(--background)" }}>

      {/* Navigation — sits at the top of the hero, scrolls away with the page */}
      <header className="absolute top-0 left-0 right-0 z-30 px-6 py-5">
        <div className="max-w-6xl mx-auto flex items-center justify-end">
          <nav className="hidden md:flex items-center gap-6">
            {[
              { label: "How It Works", href: "#how-it-works" },
              { label: "About", href: "#problem" },
              { label: "Route Map", href: "/route-map/" },
            ].map(({ label, href }) => (
              <a
                key={label}
                href={href}
                className="text-sm font-bold text-white transition-opacity hover:opacity-100"
                style={{ opacity: 0.85 }}
              >
                {label}
              </a>
            ))}
            <ThemeToggle />
          </nav>
        </div>
      </header>

      {/* Zone 1 — full-screen hero */}
      <section id="hero" className="relative min-h-screen flex flex-col">

        {/* Light mode photo: airy teal sky with clouds */}
        <img
          src="/hero-light.jpg"
          alt=""
          aria-hidden="true"
          className="absolute inset-0 w-full h-full object-cover object-[center_35%] dark:hidden"
        />
        {/* Dark mode photo: deep navy sky with wing */}
        <img
          src="/hero-dark.jpg"
          alt=""
          aria-hidden="true"
          className="absolute inset-0 w-full h-full object-cover object-[center_30%] hidden dark:block"
        />
        {/* Contrast overlay — values differ by mode via CSS variables */}
        <div
          className="absolute inset-0"
          style={{ background: "linear-gradient(to bottom, var(--hero-overlay-from), var(--hero-overlay-to))" }}
        />
        {/* Bottom fade into page background */}
        <div
          className="absolute inset-x-0 bottom-0 h-64 pointer-events-none"
          style={{ background: "linear-gradient(to bottom, transparent, var(--background))" }}
        />

        {/* Hero content */}
        <div className="relative z-10 flex flex-col items-center justify-center flex-1 px-6 pt-24 pb-16 text-center">

          {/* Brand lockup above the headline */}
          <div className="flex items-center gap-3 mb-8">
            <Plane className="w-8 h-8 text-white" style={{ opacity: 0.9 }} />
            <span
              className="text-2xl font-bold text-white tracking-wide"
              style={{ fontFamily: "var(--font-display)", opacity: 0.9 }}
            >
              CityHopper
            </span>
          </div>

          <h1
            className="text-5xl md:text-6xl lg:text-7xl font-bold text-white mb-5 leading-tight"
            style={{ fontFamily: "var(--font-display)", textShadow: "0 2px 20px rgba(0,0,0,0.5)" }}
          >
            Multi-city travel,<br />finally solved.
          </h1>
          <p
            className="text-lg md:text-xl max-w-2xl mb-10 leading-relaxed"
            style={{ color: "rgba(255,255,255,0.80)", textShadow: "0 1px 8px rgba(0,0,0,0.4)" }}
          >
            Add your cities. We&apos;ll find the optimal route — not just the cheapest individual
            flights, but the smartest way to connect them all.
          </p>

          {/* Search panel */}
          <div className="hero-glass p-6 w-full max-w-4xl text-left">
            <FlightSearchForm
              onSearch={(values) => {
                reset();
                setSortBy("price");
                mutate(values);
              }}
              isDisabled={isPending}
              isLoading={showLoadingAnimation}
            />
          </div>

        </div>

        {/* Scroll cue — fades in after 2 s, hidden once scrolled or after a search */}
        <AnimatePresence>
          {showScrollCue && !hasScrolled && !hasSearchState && (
            <motion.button
              key="scroll-cue"
              type="button"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => window.scrollBy({ top: window.innerHeight, behavior: "smooth" })}
              className="absolute bottom-10 left-0 right-0 flex flex-col items-center z-10 cursor-pointer bg-transparent border-0"
              aria-label="Scroll down"
            >
              <motion.div
                animate={{ y: [0, 6, 0] }}
                transition={{ repeat: Infinity, duration: 1.6, ease: "easeInOut" }}
              >
                <ChevronDown className="w-5 h-5" style={{ color: "rgba(255,255,255,0.4)" }} />
              </motion.div>
            </motion.button>
          )}
        </AnimatePresence>

      </section>

      {/* Zone 2 — trust-building content, shown only before any search */}
      {!hasSearchState && (
        <>
          <HowItWorksSection />
          <ProblemSection />
          <ComparisonSection />
          <FinalCTASection />
        </>
      )}

      {/* Results — shown after a search is triggered */}
      {hasSearchState && (
        <div ref={resultsRef} className="flex-1 max-w-4xl mx-auto w-full px-6 py-10">
          <div className="flex flex-col gap-6">
            <AnimatePresence mode="wait">

              {/* Loading skeletons */}
              {showLoadingAnimation && (
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
      )}

      {/* Footer */}
      <footer
        className="py-8 text-center text-sm"
        style={{ borderTop: "1px solid var(--border)", color: "var(--ch-muted)" }}
      >
        CityHopper © 2026
        <span className="mx-3">·</span>
        <a href="#problem" className="hover:text-foreground transition-colors">About</a>
        <span className="mx-3">·</span>
        <a href="mailto:kpallin90@gmail.com" className="hover:text-foreground transition-colors">Contact</a>
      </footer>

    </div>
  );
}
