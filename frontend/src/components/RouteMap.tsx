"use client";

/**
 * Visualises a multi-city flight journey as animated great-circle arcs on a world map.
 * Uses D3's geoNaturalEarth1 projection and TopoJSON world topology data from CDN.
 * Supports scroll-to-zoom and drag-to-pan. Defaults to a view fitted around the route.
 */

import { useEffect, useRef, useState, useId, useCallback } from "react";
import { RotateCcw } from "lucide-react";
import type { ZoomBehavior, ZoomTransform } from "d3";

interface AirportCoord {
  code: string;
  lat: number;
  lng: number;
}

interface RouteMapProps {
  /** Ordered airport codes for the journey, e.g. ["LHR", "DXB", "JFK", "LHR"] */
  journey: string[];
  /** Coordinates for each airport code referenced in journey */
  airports: AirportCoord[];
  /** Override the auto-calculated SVG height (default: containerWidth × 0.46) */
  mapHeight?: number;
  /** Show the "The CityHopper Way" heading above the map (default: false) */
  showHeading?: boolean;
}

// Module-level cache so world data is fetched only once per page session
let worldDataCache: unknown = null;

async function getWorldData(): Promise<unknown> {
  if (worldDataCache) return worldDataCache;
  const res = await fetch(
    "https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json"
  );
  worldDataCache = await res.json();
  return worldDataCache;
}

export function RouteMap({ journey, airports, mapHeight, showHeading = false }: RouteMapProps) {
  const uid = useId().replace(/:/g, "");
  const containerRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const [containerWidth, setContainerWidth] = useState(0);
  const zoomRef = useRef<ZoomBehavior<SVGSVGElement, unknown> | null>(null);
  const initialTransformRef = useRef<ZoomTransform | null>(null);
  // Cached d3 module reference so the reset handler doesn't need a dynamic import
  const d3Ref = useRef<typeof import("d3") | null>(null);

  // Keep containerWidth in sync with the parent container's actual width
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const obs = new ResizeObserver((entries) => {
      const w = entries[0]?.contentRect.width;
      if (w) setContainerWidth(Math.floor(w));
    });
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  useEffect(() => {
    if (!svgRef.current || containerWidth === 0 || journey.length < 2) return;

    let cancelled = false;

    async function draw() {
      const [d3, { feature }, worldData] = await Promise.all([
        import("d3"),
        import("topojson-client"),
        getWorldData(),
      ]);

      if (cancelled || !svgRef.current) return;

      d3Ref.current = d3;

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const world = worldData as any;
      const width = containerWidth;
      const height = mapHeight ?? Math.round(width * 0.46);

      const svg = d3.select(svgRef.current);
      svg.selectAll("*").remove();
      svg.attr("width", width).attr("height", height);

      // Gradient lives outside the content group so it isn't affected by the zoom transform
      const gradientId = `route-gradient-${uid}`;
      const defs = svg.append("defs");
      const grad = defs
        .append("linearGradient")
        .attr("id", gradientId)
        .attr("x1", "0%")
        .attr("y1", "0%")
        .attr("x2", "100%")
        .attr("y2", "0%");
      grad.append("stop").attr("offset", "0%").attr("stop-color", "#3b82f6");
      grad.append("stop").attr("offset", "100%").attr("stop-color", "#8b5cf6");

      // All drawable content goes in this group — the zoom transform is applied here
      const g = svg.append("g");

      const projection = d3
        .geoNaturalEarth1()
        .scale(width / 6.28)
        .translate([width / 2, height / 2]);

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const pathGen = d3.geoPath().projection(projection) as any;

      // Country polygons
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const countries = feature(world, world.objects.countries) as any;
      g.append("g")
        .selectAll("path")
        .data(countries.features)
        .join("path")
        .attr("d", pathGen)
        .attr("fill", "#f1f5f9")
        .attr("stroke", "#e2e8f0")
        .attr("stroke-width", 0.4);

      const airportLookup = new Map(airports.map((a) => [a.code, a]));
      const prefersReducedMotion = window.matchMedia(
        "(prefers-reduced-motion: reduce)"
      ).matches;

      // Great-circle arc per leg — classed so the zoom handler can select them
      for (let i = 0; i < journey.length - 1; i++) {
        const from = airportLookup.get(journey[i]);
        const to = airportLookup.get(journey[i + 1]);
        if (!from || !to) continue;

        const lineFeature = {
          type: "LineString" as const,
          coordinates: [
            [from.lng, from.lat],
            [to.lng, to.lat],
          ],
        };

        const pathEl = g
          .append("path")
          .attr("class", "flight-path")
          .datum(lineFeature)
          .attr("d", pathGen)
          .attr("fill", "none")
          .attr("stroke", `url(#${gradientId})`)
          .attr("stroke-width", 2.5)
          .attr("stroke-linecap", "round");

        const totalLength = (pathEl.node() as SVGPathElement).getTotalLength();
        pathEl
          .attr("stroke-dasharray", totalLength)
          .attr("stroke-dashoffset", totalLength)
          .transition()
          .duration(prefersReducedMotion ? 0 : 1000)
          .delay(prefersReducedMotion ? 0 : i * 280)
          .ease(d3.easeQuadOut)
          .attr("stroke-dashoffset", 0);
      }

      // Tile label constants — shared between initial render and zoom handler
      const FONT_SIZE = 10;
      const TILE_PAD_X = 5;
      const TILE_PAD_Y = 2;
      const TILE_CORNER = 4;
      const TILE_OFFSET_X = 10;
      const TILE_OFFSET_Y = -7;

      // Build marker data — deduplicate airports (home appears at start and end)
      type MarkerDatum = { code: string; x: number; y: number; label: string; ox: number; oy: number };
      const markerData: MarkerDatum[] = [];
      const seen = new Set<string>();
      const homeCode = journey[0];
      let stopNum = 1;
      journey.forEach((code) => {
        if (seen.has(code)) return;
        seen.add(code);
        const airport = airportLookup.get(code);
        if (!airport) return;
        const projected = projection([airport.lng, airport.lat]);
        if (!projected) return;
        const stopLabel = code === homeCode ? "Home" : String(stopNum++);
        markerData.push({ code, x: projected[0], y: projected[1], label: `${stopLabel} · ${code}`, ox: TILE_OFFSET_X, oy: TILE_OFFSET_Y });
      });

      const isDark = document.documentElement.classList.contains("dark");
      const tileBg = isDark ? "hsl(224 68% 14%)" : "#ffffff";
      const tileFg = isDark ? "hsl(210 20% 90%)" : "#1e293b";
      const tileBorder = isDark ? "hsl(220 50% 30%)" : "#cbd5e1";

      // Markers and label tiles use data binding so the zoom handler can update
      // their positions using each datum's base coordinates
      const markerGroups = g
        .selectAll<SVGGElement, MarkerDatum>(".airport-marker-group")
        .data(markerData)
        .join("g")
        .attr("class", "airport-marker-group");

      markerGroups.append("circle")
        .attr("class", "airport-marker")
        .attr("cx", (d) => d.x)
        .attr("cy", (d) => d.y)
        .attr("r", 4.5)
        .attr("fill", "#3b82f6")
        .attr("stroke", "white")
        .attr("stroke-width", 1.5);

      // Each label tile is a group (rect + text) that gets scale(1/k) on zoom
      // so the tile stays visually constant size regardless of zoom level
      const labelTiles = markerGroups.append("g")
        .attr("class", "airport-label-group")
        .attr("transform", (d) => `translate(${d.x + d.ox}, ${d.y + d.oy})`);

      labelTiles.append("rect")
        .attr("class", "airport-label-bg")
        .attr("rx", TILE_CORNER)
        .attr("fill", tileBg)
        .attr("stroke", tileBorder)
        .attr("stroke-width", 0.8);

      labelTiles.append("text")
        .attr("class", "airport-label")
        .attr("x", TILE_PAD_X)
        .attr("y", FONT_SIZE + TILE_PAD_Y - 1)
        .attr("font-size", FONT_SIZE)
        .attr("font-weight", "600")
        .attr("font-family", "system-ui, sans-serif")
        .attr("fill", tileFg)
        .text((d) => d.label);

      // Size rects and pick the candidate offset with least overlap for each label (greedy)
      const labelH = FONT_SIZE + TILE_PAD_Y * 2;
      type Rect = { x: number; y: number; w: number; h: number };
      const placedRects: Rect[] = [];

      const overlapArea = (a: Rect, b: Rect) => {
        const dx = Math.max(0, Math.min(a.x + a.w, b.x + b.w) - Math.max(a.x, b.x));
        const dy = Math.max(0, Math.min(a.y + a.h, b.y + b.h) - Math.max(a.y, b.y));
        return dx * dy;
      };

      markerGroups.each(function(d) {
        const grp = d3.select(this);
        const textEl = grp.select<SVGTextElement>(".airport-label").node()!;
        const labelW = textEl.getBBox().width + TILE_PAD_X * 2;

        grp.select(".airport-label-bg").attr("width", labelW).attr("height", labelH);

        // Six candidate positions: right-above, right-below, left-above, left-below, right-mid, left-mid
        const candidates = [
          { ox: TILE_OFFSET_X, oy: TILE_OFFSET_Y },
          { ox: TILE_OFFSET_X, oy: 8 },
          { ox: -labelW - TILE_OFFSET_X, oy: TILE_OFFSET_Y },
          { ox: -labelW - TILE_OFFSET_X, oy: 8 },
          { ox: TILE_OFFSET_X, oy: -labelH / 2 },
          { ox: -labelW - TILE_OFFSET_X, oy: -labelH / 2 },
        ];

        let bestOx = TILE_OFFSET_X;
        let bestOy = TILE_OFFSET_Y;
        let minOverlap = Infinity;

        for (const c of candidates) {
          const rect: Rect = { x: d.x + c.ox, y: d.y + c.oy, w: labelW, h: labelH };
          const total = placedRects.reduce((sum, p) => sum + overlapArea(rect, p), 0);
          if (total < minOverlap) {
            minOverlap = total;
            bestOx = c.ox;
            bestOy = c.oy;
          }
        }

        d.ox = bestOx;
        d.oy = bestOy;
        placedRects.push({ x: d.x + bestOx, y: d.y + bestOy, w: labelW, h: labelH });

        grp.select<SVGGElement>(".airport-label-group")
          .attr("transform", `translate(${d.x + bestOx}, ${d.y + bestOy})`);
      });

      // Compute initial transform to fit the route's bounding box with padding
      const uniqueCodes = [...new Set(journey)];
      const projectedCoords = uniqueCodes
        .map((code) => airportLookup.get(code))
        .filter(Boolean)
        .map((a) => projection([a!.lng, a!.lat]))
        .filter(Boolean) as [number, number][];

      let initialTransform = d3.zoomIdentity;

      if (projectedCoords.length >= 2) {
        const xs = projectedCoords.map((c) => c[0]);
        const ys = projectedCoords.map((c) => c[1]);
        const minX = Math.min(...xs);
        const maxX = Math.max(...xs);
        const minY = Math.min(...ys);
        const maxY = Math.max(...ys);
        const routeW = maxX - minX;
        const routeH = maxY - minY;

        const padding = Math.min(80, width * 0.12);
        const scale = Math.min(
          routeW > 0 ? (width - 2 * padding) / routeW : 8,
          routeH > 0 ? (height - 2 * padding) / routeH : 8,
          10 // cap so very short routes don't zoom in absurdly
        );

        const cx = (minX + maxX) / 2;
        const cy = (minY + maxY) / 2;
        initialTransform = d3.zoomIdentity
          .translate(width / 2 - scale * cx, height / 2 - scale * cy)
          .scale(scale);
      }

      // Zoom behaviour — transforms the content group and inverse-scales
      // strokes/markers/labels so they stay visually constant size
      const zoom = d3.zoom<SVGSVGElement, unknown>()
        .scaleExtent([Math.min(0.1, initialTransform.k * 0.8), 15])
        .on("zoom", (event) => {
          const { transform } = event;
          g.attr("transform", transform);
          const k = transform.k;

          g.selectAll<SVGPathElement, unknown>(".flight-path")
            .attr("stroke-width", 2.5 / k);

          g.selectAll<SVGCircleElement, MarkerDatum>(".airport-marker")
            .attr("r", 4.5 / k)
            .attr("stroke-width", 1.5 / k);

          g.selectAll<SVGGElement, MarkerDatum>(".airport-label-group")
            .attr("transform", (d) =>
              `translate(${d.x + d.ox / k}, ${d.y + d.oy / k}) scale(${1 / k})`
            );
        });

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      svg.call(zoom).call(zoom.transform as any, initialTransform);

      zoomRef.current = zoom;
      initialTransformRef.current = initialTransform;
    }

    draw();
    return () => {
      cancelled = true;
    };
  }, [journey, airports, containerWidth, uid, mapHeight]);

  const handleReset = useCallback(() => {
    if (!svgRef.current || !zoomRef.current || !initialTransformRef.current || !d3Ref.current) return;
    const d3 = d3Ref.current;
    d3.select(svgRef.current)
      .transition()
      .duration(400)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .call(zoomRef.current.transform as any, initialTransformRef.current);
  }, []);

  return (
    <div>
      <div className="relative flex items-center justify-center mb-3">
        {showHeading && (
          <span className="text-xs font-semibold uppercase tracking-widest text-muted">
            The CityHopper Way
          </span>
        )}
        <button
          type="button"
          onClick={handleReset}
          className="absolute right-0 flex items-center gap-1 text-xs text-muted hover:text-foreground transition-colors"
          aria-label="Reset map view"
        >
          <RotateCcw className="w-3 h-3" />
          Reset
        </button>
      </div>
      <div
        ref={containerRef}
        className="rounded-2xl border border-border/40 bg-background/30 overflow-hidden cursor-grab active:cursor-grabbing"
        style={{ transform: "translateZ(0)" }}
      >
        <svg ref={svgRef} />
      </div>
    </div>
  );
}
