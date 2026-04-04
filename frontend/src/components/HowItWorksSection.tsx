"use client";

/**
 * Landing page section explaining the three-step CityHopper search process,
 * with a live route diagram showing a demo multi-city journey.
 */

import { MapPin, SlidersHorizontal, Route } from "lucide-react";
import { RouteMap } from "@/components/RouteMap";

const STEPS = [
  {
    Icon: MapPin,
    title: "Add Your Cities",
    body: "Tell us where you want to go — in any order. Add up to 5 destinations.",
  },
  {
    Icon: SlidersHorizontal,
    title: "Choose What Matters",
    body: "Optimize for cost, time, or number of connections. You set the priority.",
  },
  {
    Icon: Route,
    title: "Get Your Optimal Route",
    body: "We calculate every possible sequence and surface the route that actually makes sense — not just the cheapest leg, but the best journey overall.",
  },
] as const;



export function HowItWorksSection() {
  return (
    <section id="how-it-works" className="py-24" style={{ background: "var(--background)" }}>
      <div className="max-w-6xl mx-auto px-6">

        <h2
          className="text-4xl font-semibold text-center mb-16"
          style={{ fontFamily: "var(--font-display)", color: "var(--foreground)" }}
        >
          Simple search. Smart routing.
        </h2>

        {/* Three-step columns */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 mb-16">
          {STEPS.map(({ Icon, title, body }) => (
            <div key={title} className="flex flex-col items-center text-center gap-4">
              <div
                className="w-12 h-12 rounded-full flex items-center justify-center shrink-0"
                style={{ background: "var(--primary-light)" }}
              >
                <Icon className="w-5 h-5" style={{ color: "var(--primary)" }} />
              </div>
              <h3 className="text-lg font-semibold" style={{ color: "var(--foreground)" }}>{title}</h3>
              <p className="text-sm leading-relaxed" style={{ color: "var(--ch-muted)" }}>{body}</p>
            </div>
          ))}
        </div>

        

      </div>
    </section>
  );
}
