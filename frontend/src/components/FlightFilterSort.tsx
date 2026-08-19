"use client";

/**
 * Result count display and sort controls.
 */

import { DollarSign, Clock, X } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

interface FlightFilterSortProps {
  resultCount: number;
  sortBy: "price" | "duration";
  onSortChange: (sort: "price" | "duration") => void;
  onClear: () => void;
}

export function FlightFilterSort({
  resultCount,
  sortBy,
  onSortChange,
  onClear,
}: FlightFilterSortProps) {
  return (
    <Card className="p-4">
      <div className="flex flex-col sm:flex-row sm:items-center gap-4">

        {/* Clear search button */}
        <button
          type="button"
          onClick={onClear}
          className="group flex items-center gap-1.5 shrink-0 rounded-lg border border-border/50 px-3 py-1.5 text-sm text-muted transition-colors hover:border-destructive/50 hover:text-destructive"
        >
          Clear search
          <X className="w-3.5 h-3.5" />
        </button>

        {/* Result count + sort controls */}
        <div className="flex items-center gap-3 sm:ml-auto shrink-0">
          <span className="text-sm text-muted">
            {resultCount} route{resultCount !== 1 ? "s" : ""} found
          </span>
          <span className="text-sm text-muted">·</span>
          <span className="text-sm text-muted">Sort by</span>
          <div className="flex gap-1 rounded-xl border border-border/50 bg-background/30 p-1">
            <Button
              variant="segment"
              active={sortBy === "price"}
              onClick={() => onSortChange("price")}
            >
              <DollarSign className="w-3.5 h-3.5" />
              Price
            </Button>
            <Button
              variant="segment"
              active={sortBy === "duration"}
              onClick={() => onSortChange("duration")}
            >
              <Clock className="w-3.5 h-3.5" />
              Duration
            </Button>
          </div>
        </div>

      </div>
    </Card>
  );
}
