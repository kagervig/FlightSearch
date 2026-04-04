"use client";

/**
 * Result count display and sort controls.
 * Time-of-day departure filters are rendered but disabled — the backend does not
 * currently expose departure time filtering on the multicity endpoint.
 */

import { Sunrise, Sun, Moon, DollarSign, Clock } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { cn } from "@/lib/utils";

interface FlightFilterSortProps {
  resultCount: number;
  sortBy: "price" | "duration";
  onSortChange: (sort: "price" | "duration") => void;
}

const TIME_FILTERS = [
  { label: "Morning", icon: Sunrise },
  { label: "Afternoon", icon: Sun },
  { label: "Evening", icon: Moon },
];

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

        {/* Time-of-day filters — disabled until backend supports them */}
        <div className="flex items-center gap-2 sm:flex-1">
          {TIME_FILTERS.map(({ label, icon: Icon }) => (
            <div
              key={label}
              title="Requires backend support"
              className={cn(
                "inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs border border-border/40",
                "opacity-40 cursor-not-allowed select-none text-muted"
              )}
            >
              <Icon className="w-3.5 h-3.5" />
              {label}
            </div>
          ))}
        </div>

        {/* Sort segmented control */}
        <div className="flex gap-1 rounded-xl border border-border/50 bg-background/30 p-1 shrink-0">
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
