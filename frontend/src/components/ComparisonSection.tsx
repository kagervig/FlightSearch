"use client";

/**
 * Comparison table showing how CityHopper differs from single-leg flight tools.
 * On mobile, each row becomes an accordion card.
 */

import { useState } from "react";
import { Check, X, ChevronDown } from "lucide-react";

const ROWS = [
  {
    feature: "Multi-city search",
    others: "Manual, leg by leg",
    ch: "Automatic, end-to-end",
  },
  {
    feature: "Route optimization",
    others: "None",
    ch: "Cost, time, or stops",
  },
  {
    feature: "Combinations considered",
    others: "1",
    ch: "All possible sequences",
  },
  {
    feature: "Result",
    others: "A list of flights",
    ch: "A complete journey",
  },
] as const;

export function ComparisonSection() {
  const [expandedRow, setExpandedRow] = useState<number | null>(null);

  return (
    <section className="py-24" style={{ background: "var(--background)" }}>
      <div className="max-w-4xl mx-auto px-6">

        <h2
          className="text-4xl font-semibold text-center mb-16"
          style={{ fontFamily: "var(--font-display)", color: "var(--foreground)" }}
        >
          Built differently, on purpose.
        </h2>

        {/* Desktop table */}
        <div className="hidden md:block rounded-2xl overflow-hidden border" style={{ borderColor: "var(--border)" }}>
          <table className="w-full text-sm">
            <thead>
              <tr>
                <th
                  className="text-left px-6 py-4 font-semibold"
                  style={{ background: "var(--card)", color: "var(--ch-muted)" }}
                >
                  Feature
                </th>
                <th
                  className="text-center px-6 py-4 font-semibold"
                  style={{ background: "var(--card)", color: "var(--ch-muted)" }}
                >
                  Other Tools
                </th>
                <th
                  className="text-center px-6 py-4 font-bold text-white"
                  style={{ background: "var(--ch-accent)", color: "#0F1F3D" }}
                >
                  CityHopper
                </th>
              </tr>
            </thead>
            <tbody>
              {ROWS.map(({ feature, others, ch }, i) => (
                <tr key={feature} style={{ background: i % 2 === 0 ? "var(--background)" : "var(--card)" }}>
                  <td className="px-6 py-4 font-medium" style={{ color: "var(--foreground)" }}>
                    {feature}
                  </td>
                  <td className="px-6 py-4 text-center" style={{ color: "var(--ch-muted)" }}>
                    {others}
                  </td>
                  <td
                    className="px-6 py-4 text-center font-semibold"
                    style={{ color: "var(--primary)", background: "rgba(15,31,61,0.03)" }}
                  >
                    {ch}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Mobile accordion */}
        <div className="md:hidden space-y-3">
          {ROWS.map(({ feature, others, ch }, i) => (
            <div key={feature} className="rounded-xl border overflow-hidden" style={{ borderColor: "var(--border)", background: "var(--card)" }}>
              <button
                type="button"
                className="w-full flex items-center justify-between px-4 py-3 text-left"
                onClick={() => setExpandedRow(expandedRow === i ? null : i)}
                aria-expanded={expandedRow === i}
              >
                <span className="font-medium text-sm" style={{ color: "var(--foreground)" }}>{feature}</span>
                <ChevronDown
                  className="w-4 h-4 transition-transform duration-200"
                  style={{
                    color: "var(--ch-muted)",
                    transform: expandedRow === i ? "rotate(180deg)" : "none",
                  }}
                />
              </button>
              {expandedRow === i && (
                <div className="px-4 pb-4 space-y-2 border-t" style={{ borderColor: "var(--border)" }}>
                  <div className="flex items-start gap-2 pt-3">
                    <X className="w-4 h-4 shrink-0 mt-0.5" style={{ color: "var(--ch-muted)" }} />
                    <span className="text-sm" style={{ color: "var(--ch-muted)" }}>{others}</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <Check className="w-4 h-4 shrink-0 mt-0.5" style={{ color: "var(--primary)" }} />
                    <span className="text-sm font-semibold" style={{ color: "var(--foreground)" }}>{ch}</span>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
