"use client";

/**
 * Renders all distinct airport connections from the flight graph as great-circle
 * arcs on a world map. Hovering an airport highlights its connections and shows
 * a tooltip. Supports scroll-to-zoom and drag-to-pan.
 */

import { useEffect, useRef, useId, useCallback, useState } from "react";
import { RotateCcw } from "lucide-react";
import type { ZoomBehavior } from "d3";

export interface Airport {
  code: string;
  city: string;
  country: string;
  lat: number;
  lon: number;
}

export interface Connection {
  from: string;
  to: string;
}

interface NetworkMapProps {
  airports: Airport[];
  connections: Connection[];
}

interface Tooltip {
  x: number;
  y: number;
  airport: Airport;
}

// Module-level cache — fetched once per page session
let worldDataCache: unknown = null;
async function getWorldData(): Promise<unknown> {
  if (worldDataCache) return worldDataCache;
  const res = await fetch("https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json");
  worldDataCache = await res.json();
  return worldDataCache;
}

type AirportDatum = Airport & { x: number; y: number };

export function NetworkMap({ airports, connections }: NetworkMapProps) {
  const uid = useId().replace(/:/g, "");
  const containerRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const zoomRef = useRef<ZoomBehavior<SVGSVGElement, unknown> | null>(null);
  const d3Ref = useRef<typeof import("d3") | null>(null);
  const [tooltip, setTooltip] = useState<Tooltip | null>(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const obs = new ResizeObserver((entries) => {
      const { width, height } = entries[0].contentRect;
      if (width && height) setSize({ width: Math.floor(width), height: Math.floor(height) });
    });
    obs.observe(el);
    setSize({ width: Math.floor(el.clientWidth), height: Math.floor(el.clientHeight) });
    return () => obs.disconnect();
  }, []);

  useEffect(() => {
    const { width, height } = size;
    if (!svgRef.current || width === 0 || height === 0 || airports.length === 0) return;

    let cancelled = false;

    async function draw() {
      const [d3, { feature }, worldData] = await Promise.all([
        import("d3"),
        import("topojson-client"),
        getWorldData(),
      ]);
      if (cancelled || !svgRef.current) return;

      d3Ref.current = d3;

      const isDark = document.documentElement.classList.contains("dark");
      const landFill   = isDark ? "hsl(220 40% 15%)" : "hsl(215 20% 88%)";
      const landStroke = isDark ? "hsl(220 40% 22%)" : "hsl(215 20% 76%)";
      const dotFill    = isDark ? "hsl(214 84% 65%)" : "hsl(214 84% 50%)";
      const dotStroke  = isDark ? "hsl(224 68% 18%)" : "white";

      const svg = d3.select(svgRef.current);
      svg.selectAll("*").remove();
      svg.attr("width", width).attr("height", height);

      const projection = d3.geoNaturalEarth1()
        .scale(width / 6.28)
        .translate([width / 2, height / 2]);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const pathGen = d3.geoPath().projection(projection) as any;

      // All zoomable content lives here
      const g = svg.append("g");

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const world = worldData as any;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const countries = feature(world, world.objects.countries) as any;
      g.append("g")
        .selectAll("path")
        .data(countries.features)
        .join("path")
        .attr("d", pathGen)
        .attr("fill", landFill)
        .attr("stroke", landStroke)
        .attr("stroke-width", 0.3);

      const airportLookup = new Map(airports.map((a) => [a.code, a]));

      // Draw all connection arcs
      for (const conn of connections) {
        const from = airportLookup.get(conn.from);
        const to   = airportLookup.get(conn.to);
        if (!from || !to) continue;

        g.append("path")
          .datum({ type: "LineString", coordinates: [[from.lon, from.lat], [to.lon, to.lat]] })
          .attr("class", "conn-arc")
          .attr("data-from", conn.from)
          .attr("data-to", conn.to)
          .attr("d", pathGen)
          .attr("fill", "none")
          .attr("stroke", "var(--ch-accent)")
          .attr("stroke-width", 0.6)
          .attr("stroke-opacity", 0.18)
          .attr("stroke-linecap", "round");
      }

      // Project all airports to screen coordinates
      const airportData: AirportDatum[] = [];
      for (const a of airports) {
        const projected = projection([a.lon, a.lat]);
        if (projected) airportData.push({ ...a, x: projected[0], y: projected[1] });
      }

      // Draw airport dots on top of arcs
      g.selectAll<SVGCircleElement, AirportDatum>(".airport-dot")
        .data(airportData)
        .join("circle")
        .attr("class", "airport-dot")
        .attr("data-code", (d) => d.code)
        .attr("cx", (d) => d.x)
        .attr("cy", (d) => d.y)
        .attr("r", 3.5)
        .attr("fill", dotFill)
        .attr("stroke", dotStroke)
        .attr("stroke-width", 1)
        .style("cursor", "pointer")
        .on("mouseenter", function(event: MouseEvent, d: AirportDatum) {
          // Highlight arcs connected to this airport
          g.selectAll<SVGPathElement, unknown>(".conn-arc")
            .attr("stroke-opacity", function() {
              const isConnected =
                this.getAttribute("data-from") === d.code ||
                this.getAttribute("data-to")   === d.code;
              return isConnected ? 0.85 : 0.04;
            })
            .attr("stroke-width", function() {
              const isConnected =
                this.getAttribute("data-from") === d.code ||
                this.getAttribute("data-to")   === d.code;
              return isConnected ? 1.4 : 0.6;
            });

          d3.select(this).attr("r", 6).attr("fill", "var(--ch-accent)");

          const rect = containerRef.current?.getBoundingClientRect();
          if (rect) {
            setTooltip({ x: event.clientX - rect.left, y: event.clientY - rect.top, airport: d });
          }
        })
        .on("mouseleave", function() {
          g.selectAll<SVGPathElement, unknown>(".conn-arc")
            .attr("stroke-opacity", 0.18)
            .attr("stroke-width", 0.6);

          d3.select(this).attr("r", 3.5).attr("fill", dotFill);
          setTooltip(null);
        });

      // Zoom/pan — inverse-scales strokes and dots so they stay visually constant
      const zoom = d3.zoom<SVGSVGElement, unknown>()
        .scaleExtent([0.5, 20])
        .on("zoom", (event) => {
          const { transform } = event;
          g.attr("transform", transform);
          const k = transform.k;
          g.selectAll<SVGPathElement, unknown>(".conn-arc").attr("stroke-width", 0.6 / k);
          g.selectAll<SVGCircleElement, AirportDatum>(".airport-dot")
            .attr("r", 3.5 / k)
            .attr("stroke-width", 1 / k);
          setTooltip(null);
        });

      svg.call(zoom);
      zoomRef.current = zoom;
    }

    draw();
    return () => { cancelled = true; };
    // uid is stable — include it only to satisfy the linter; it doesn't cause re-runs
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [airports, connections, size, uid]);

  const handleReset = useCallback(() => {
    if (!svgRef.current || !zoomRef.current || !d3Ref.current) return;
    d3Ref.current.select(svgRef.current)
      .transition()
      .duration(400)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .call(zoomRef.current.transform as any, d3Ref.current.zoomIdentity);
  }, []);

  return (
    <div className="relative w-full h-full">
      <div
        ref={containerRef}
        className="w-full h-full overflow-hidden cursor-grab active:cursor-grabbing"
      >
        <svg ref={svgRef} />
      </div>

      <button
        type="button"
        onClick={handleReset}
        className="absolute bottom-4 right-4 flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs glass transition-colors hover:opacity-80"
        style={{ color: "var(--foreground)" }}
        aria-label="Reset map view"
      >
        <RotateCcw className="w-3.5 h-3.5" />
        Reset
      </button>

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
          <p className="font-bold text-sm" style={{ color: "var(--ch-accent)" }}>
            {tooltip.airport.code}
          </p>
          <p className="font-medium" style={{ color: "var(--foreground)" }}>
            {tooltip.airport.city}
          </p>
          <p style={{ color: "var(--ch-muted)" }}>{tooltip.airport.country}</p>
        </div>
      )}
    </div>
  );
}
