"use client";

/**
 * A single animated destination row: airport autocomplete, days spinner, and remove button.
 * Designed for use inside a react-hook-form useFieldArray context.
 */

import { motion } from "framer-motion";
import { Controller, Control, FieldErrors } from "react-hook-form";
import { MapPin, Trash2 } from "lucide-react";
import { AirportAutocomplete } from "@/components/AirportAutocomplete";
import { type SearchFormValues } from "@/components/FlightSearchForm";

interface DestinationRowProps {
  control: Control<SearchFormValues>;
  index: number;
  canRemove: boolean;
  onRemove: () => void;
  errors: FieldErrors<SearchFormValues>;
}

export function DestinationRow({ control, index, canRemove, onRemove, errors }: DestinationRowProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      transition={{ duration: 0.15 }}
    >
      <div className="flex items-center gap-2">
        {/* Single Controller for the whole destination object avoids field-path conflicts */}
        <div className="flex-1">
          <Controller
            control={control}
            name={`destinations.${index}`}
            render={({ field }) => (
              <AirportAutocomplete
                value={field.value.code}
                onChange={(code, city) =>
                  field.onChange({ ...field.value, code, city })
                }
                placeholder={`Destination ${index + 1}`}
                icon={<MapPin className="w-4 h-4" />}
              />
            )}
          />
        </div>

        {/* Days spinner */}
        <Controller
          control={control}
          name={`destinations.${index}.days`}
          render={({ field: daysField }) => (
            <div className="flex items-center gap-1 shrink-0">
              <button
                type="button"
                onClick={() => daysField.onChange(Math.max(1, daysField.value - 1))}
                className="w-7 h-7 rounded-lg border border-border/50 text-muted hover:text-foreground hover:border-primary/50 transition-colors flex items-center justify-center text-base leading-none"
              >
                −
              </button>
              <span className="w-8 text-center text-sm tabular-nums">
                {daysField.value}d
              </span>
              <button
                type="button"
                onClick={() => daysField.onChange(Math.min(14, daysField.value + 1))}
                className="w-7 h-7 rounded-lg border border-border/50 text-muted hover:text-foreground hover:border-primary/50 transition-colors flex items-center justify-center text-base leading-none"
              >
                +
              </button>
            </div>
          )}
        />

        {/* Remove button — always reserve space, hide when only 1 row */}
        <button
          type="button"
          onClick={onRemove}
          disabled={!canRemove}
          aria-label="Remove destination"
          className="w-7 h-7 rounded-lg text-muted hover:text-destructive hover:border hover:border-destructive/50 transition-colors flex items-center justify-center disabled:opacity-0 disabled:pointer-events-none shrink-0"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>

      {errors.destinations?.[index]?.code && (
        <p className="text-xs text-destructive mt-1 pl-1">
          {errors.destinations[index]?.code?.message}
        </p>
      )}
    </motion.div>
  );
}
