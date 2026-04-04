"use client";

/**
 * Result count display and sort controls.
 */

import { DollarSign, Clock } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

interface FlightFilterSortProps {
  resultCount: number;
  sortBy: "price" | "duration";
  onSortChange: (sort: "price" | "duration") => void;
}

export function FlightFilterSort({
  resultCount,
  sortBy,
  onSortChange,
}: FlightFilterSortProps) {
  return (
    <Card className="p-4">
      <div className="flex flex-col sm:flex-row sm:items-center gap-4">
        {/* Result count */}
        <span className="text-sm text-muted shrink-0">
          {resultCount} route{resultCount !== 1 ? "s" : ""} found
        </span>

        {/* Sort segmented control */}
        <div className="flex gap-1 rounded-xl border border-border/50 bg-background/30 p-1 shrink-0 sm:ml-auto">
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
    </Card>
  );
}
