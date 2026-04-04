"use client";

/**
 * Landing page section that frames the multi-city travel pain point
 * and positions CityHopper as the solution. Animates on scroll:
 * "The old way" tiles stagger in one by one, then the route map draws.
 */

import { useRef, useState, useEffect } from "react";
import { motion, useInView } from "framer-motion";
import { RouteMap } from "@/components/RouteMap";

const DEMO_JOURNEY = ["LHR", "CDG", "FCO", "BCN", "LHR"];
const DEMO_AIRPORTS = [
  { code: "LHR", lat: 51.477, lng: -0.461 },
  { code: "CDG", lat: 49.009, lng: 2.547 },
  { code: "FCO", lat: 41.8, lng: 12.236 },
  { code: "BCN", lat: 41.297, lng: 2.078 },
];

const OLD_WAY_TABS = [
  "Search: LHR → CDG",
  "Search: CDG → FCO",
  "Search: FCO → BCN",
  "Search: BCN → LHR",
  "Wait, is that the best order?",
  "Try: LHR → FCO → CDG → BCN…",
];

// Duration each tile takes to appear + stagger between tiles
const TILE_STAGGER = 0.18;
const TILE_DURATION = 0.3;
// Map starts drawing once all tiles are visible
const MAP_DELAY_MS = (OLD_WAY_TABS.length * TILE_STAGGER + TILE_DURATION) * 1000 + 200;

export function ProblemSection() {
  const sectionRef = useRef<HTMLElement>(null);
  const inView = useInView(sectionRef, { once: true, margin: "-10% 0px" });
  const [showMap, setShowMap] = useState(false);

  useEffect(() => {
    if (!inView) return;
    const timer = setTimeout(() => setShowMap(true), MAP_DELAY_MS);
    return () => clearTimeout(timer);
  }, [inView]);

  return (
    <section id="problem" ref={sectionRef} className="py-24" style={{ background: "var(--ch-surface)" }}>
      <div className="max-w-6xl mx-auto px-6">

        {/* Copy */}
        <div className="max-w-3xl mx-auto text-center mb-16">
          <h2
            className="text-4xl font-semibold mb-6"
            style={{ fontFamily: "var(--font-display)", color: "var(--foreground)" }}
          >
            Other tools find you flights.
            <br />We find you a journey.
          </h2>
          <p className="text-lg leading-relaxed mb-4" style={{ color: "var(--ch-muted)" }}>
            Google Flights and Skyscanner are built for one thing: getting you from A to B. But if
            you&apos;re planning a trip through multiple cities, you&apos;re on your own — opening tab after
            tab, manually comparing combinations, hoping the route you stitched together actually
            makes sense.
          </p>
          <p className="text-lg font-medium" style={{ color: "var(--foreground)" }}>
            CityHopper does the thinking for you.
          </p>
        </div>

        {/* Illustration: chaos vs. clarity */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto items-stretch">

          {/* Left: the old way */}
          <div className="rounded-2xl border p-6 flex flex-col" style={{ borderColor: "var(--border)", background: "var(--card)" }}>
            <p className="text-xs font-semibold uppercase tracking-widest mb-5 text-center" style={{ color: "var(--ch-muted)" }}>
              The old way
            </p>
            <div className="flex-1 space-y-2">
              {OLD_WAY_TABS.map((tab, i) => (
                <motion.div
                  key={tab}
                  initial={{ opacity: 0, y: 8 }}
                  animate={inView ? { opacity: 1 - i * 0.1, y: 0 } : { opacity: 0, y: 8 }}
                  transition={{ duration: TILE_DURATION, delay: i * TILE_STAGGER, ease: "easeOut" }}
                  className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-xs border"
                  style={{
                    borderColor: "var(--border)",
                    background: "var(--background)",
                    color: "var(--ch-muted)",
                  }}
                >
                  <span
                    className="w-2 h-2 rounded-full shrink-0"
                    style={{ background: i >= 4 ? "var(--destructive)" : "var(--ch-muted)", opacity: 0.5 }}
                  />
                  {tab}
                </motion.div>
              ))}
            </div>
            <p className="text-xs text-center mt-4" style={{ color: "var(--ch-muted)" }}>
              6 tabs open. Still not sure.
            </p>
          </div>

          {/* Right: the CityHopper way */}
          <div className="rounded-2xl border p-6 flex flex-col" style={{ borderColor: "var(--border)", background: "var(--card)" }}>
            <div className="flex-1 flex flex-col justify-center">
              {showMap
                ? <RouteMap journey={DEMO_JOURNEY} airports={DEMO_AIRPORTS} mapHeight={260} />
                : <div />
              }
            </div>
            <p className="text-xs text-center mt-4" style={{ color: "var(--ch-muted)" }}>
              One search finds the best route for you.
            </p>
          </div>

        </div>
      </div>
    </section>
  );
}
