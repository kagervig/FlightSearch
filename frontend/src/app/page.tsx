/*
 * page.tsx - CityHopper home page entry point.
 *
 * Thin server component that wraps the client content in Suspense so that
 * useSearchParams() inside HomeContent can be resolved during static prerendering.
 */

import { Suspense } from "react";
import { HomeContent } from "./HomeContent";

export default function Page() {
  return (
    <Suspense>
      <HomeContent />
    </Suspense>

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
                setSortBy(values.optimizeBy === "duration" ? "duration" : "price");
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
=======
    <div className="min-h-screen flex flex-col" style={{ background: "var(--background)" }}>

      {/* Navigation — sits at the top of the hero, scrolls away with the page */}
  );
}
