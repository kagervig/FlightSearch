/*
 * route-map/page.tsx — Full-screen visualisation of all airport connections
 * in the flight graph. Fetches from GET /api/graph/connections and renders
 * great-circle arcs on a world map via NetworkMap.
 */

"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Plane, ArrowLeft } from "lucide-react";
import { NetworkMap, type Airport, type Connection } from "@/components/NetworkMap";
import { ThemeToggle } from "@/components/ThemeToggle";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface GraphData {
  airports: Airport[];
  connections: Connection[];
}

async function fetchGraphConnections(): Promise<GraphData> {
  const res = await fetch(`${API_URL}/api/graph/connections`);
  if (!res.ok) throw new Error("Failed to load network data");
  return res.json();
}

export default function RouteMapPage() {
  const { data, isPending, error } = useQuery<GraphData>({
    queryKey: ["graph-connections"],
    queryFn: fetchGraphConnections,
  });

  return (
    <div className="h-screen flex flex-col" style={{ background: "var(--background)" }}>

      {/* Header */}
      <header
        className="shrink-0 px-6 py-4 flex items-center justify-between"
        style={{ borderBottom: "1px solid var(--border)" }}
      >
        <div className="flex items-center gap-4">
          <Link
            href="/"
            className="flex items-center gap-1.5 text-sm font-medium transition-colors hover:opacity-100"
            style={{ color: "var(--ch-muted)", opacity: 0.85 }}
          >
            <ArrowLeft className="w-4 h-4" />
            Back
          </Link>
          <div className="h-5 w-px" style={{ background: "var(--border)" }} />
          <div className="flex items-center gap-2">
            <Plane className="w-4 h-4" style={{ color: "var(--primary)" }} />
            <span
              className="font-semibold"
              style={{ fontFamily: "var(--font-display)", color: "var(--foreground)" }}
            >
              Route Network
            </span>
          </div>
        </div>

        {data && (
          <div
            className="hidden sm:flex items-center gap-4 text-xs"
            style={{ color: "var(--ch-muted)" }}
          >
            <span>{data.airports.length} airports</span>
            <span>·</span>
            <span>{data.connections.length} connections</span>
          </div>
        )}

        <ThemeToggle />
      </header>

      {/* Map */}
      <main className="flex-1 relative overflow-hidden">
        {isPending && (
          <div className="absolute inset-0 flex items-center justify-center">
            <p className="text-sm" style={{ color: "var(--ch-muted)" }}>Loading network…</p>
          </div>
        )}
        {error && (
          <div className="absolute inset-0 flex items-center justify-center">
            <p className="text-sm text-destructive">Failed to load network data.</p>
          </div>
        )}
        {data && <NetworkMap airports={data.airports} connections={data.connections} />}
      </main>

    </div>
  );
}
