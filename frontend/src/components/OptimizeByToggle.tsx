"use client";

/**
 * Segmented toggle for choosing whether to optimise search results by price or duration.
 */

import { DollarSign, Clock } from "lucide-react";
import { Button } from "@/components/ui/Button";

interface OptimizeByToggleProps {
  value: "price" | "duration";
  onChange: (value: "price" | "duration") => void;
}

export function OptimizeByToggle({ value, onChange }: OptimizeByToggleProps) {
  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium text-muted">Optimise By</label>
      <div className="flex gap-1 rounded-lg border border-border/50 bg-background/30 p-0.5">
        <Button
          type="button"
          variant="segment"
          active={value === "price"}
          onClick={() => onChange("price")}
          className="flex-1 text-xs py-1 gap-1"
        >
          <DollarSign className="w-3 h-3" />
          Price
        </Button>
        <Button
          type="button"
          variant="segment"
          active={value === "duration"}
          onClick={() => onChange("duration")}
          className="flex-1 text-xs py-1 gap-1"
        >
          <Clock className="w-3 h-3" />
          Duration
        </Button>
      </div>
    </div>
  );
}
