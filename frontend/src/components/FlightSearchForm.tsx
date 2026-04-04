"use client";

/**
 * Flight search form with animated destination rows, days-per-destination spinners,
 * a departure date picker, and price/duration optimize toggle.
 */

import { useRef } from "react";
import { useForm, useFieldArray, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AnimatePresence } from "framer-motion";
import { Navigation, Plus, Calendar, Search, Loader2 } from "lucide-react";
import { AirportAutocomplete } from "@/components/AirportAutocomplete";
import { DestinationRow } from "@/components/DestinationRow";
import { OptimizeByToggle } from "@/components/OptimizeByToggle";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

const schema = z.object({
  homeAirport: z.object({
    code: z.string().min(2, "Enter a home airport"),
    city: z.string(),
  }),
  destinations: z
    .array(
      z.object({
        code: z.string().min(2, "Enter a destination airport"),
        city: z.string(),
        days: z.number().min(1).max(14),
      })
    )
    .min(1, "Add at least one destination"),
  departureDate: z.string().min(1, "Choose a departure date"),
  optimizeBy: z.enum(["price", "duration"]),
});

export type SearchFormValues = z.infer<typeof schema>;

interface FlightSearchFormProps {
  onSearch: (values: SearchFormValues) => void;
  isLoading: boolean;
}

export function FlightSearchForm({ onSearch, isLoading }: FlightSearchFormProps) {
  const today = new Date().toISOString().split("T")[0];
  const dateInputRef = useRef<HTMLInputElement>(null);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<SearchFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      homeAirport: { code: "", city: "" },
      destinations: [{ code: "", city: "", days: 3 }],
      departureDate: today,
      optimizeBy: "price",
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "destinations",
  });

  return (
    <Card className="p-6">
      <h2
        className="text-2xl font-semibold mb-6"
        style={{ fontFamily: "var(--font-display)" }}
      >
        Plan Your Trip
      </h2>

      <form onSubmit={handleSubmit(onSearch)} noValidate className="space-y-5">
        {/* Two-column layout: home/date on left (1/3), destinations on right (2/3) */}
        <div className="grid grid-cols-3 gap-x-6 items-start">

          {/* Left column */}
          <div className="col-span-1 space-y-5">
            {/* Home airport */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Home Airport</label>
              <Controller
                control={control}
                name="homeAirport"
                render={({ field }) => (
                  <AirportAutocomplete
                    value={field.value.code}
                    onChange={(code, city) => field.onChange({ code, city })}
                    placeholder="City or code (e.g. YVR)"
                    icon={<Navigation className="w-4 h-4" />}
                  />
                )}
              />
              {errors.homeAirport?.code && (
                <p className="text-xs text-destructive">{errors.homeAirport.code.message}</p>
              )}
            </div>

            {/* Departure date */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Departure Date</label>
              <Controller
                control={control}
                name="departureDate"
                render={({ field }) => (
                  /* Clicking anywhere in the row opens the date picker */
                  <div
                    className="relative flex items-center cursor-pointer"
                    onClick={() => {
                      try { dateInputRef.current?.showPicker(); } catch { /* unsupported browser */ }
                      dateInputRef.current?.focus();
                    }}
                  >
                    <span className="absolute left-3 text-muted pointer-events-none">
                      <Calendar className="w-4 h-4" />
                    </span>
                    <input
                      ref={dateInputRef}
                      type="date"
                      min={today}
                      value={field.value}
                      onChange={field.onChange}
                      className="w-full cursor-pointer rounded-xl border border-border/50 bg-background/50 py-2.5 pl-9 pr-4 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary/50 transition-all duration-200"
                    />
                  </div>
                )}
              />
              {errors.departureDate && (
                <p className="text-xs text-destructive">{errors.departureDate.message}</p>
              )}
            </div>

            {/* Optimize by toggle */}
            <Controller
              control={control}
              name="optimizeBy"
              render={({ field }) => (
                <OptimizeByToggle value={field.value} onChange={field.onChange} />
              )}
            />
          </div>

          {/* Right column */}
          <div className="col-span-2">
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Destinations</label>

              <div className="space-y-2">
                <AnimatePresence initial={false}>
                  {fields.map((field, index) => (
                    <DestinationRow
                      key={field.id}
                      control={control}
                      index={index}
                      canRemove={fields.length > 1}
                      onRemove={() => remove(index)}
                      errors={errors}
                    />
                  ))}
                </AnimatePresence>
              </div>

              {fields.length < 5 && (
                <button
                  type="button"
                  onClick={() => append({ code: "", city: "", days: 3 })}
                  className="mt-1 w-full flex items-center justify-center gap-2 rounded-xl border border-dashed border-border py-2.5 text-sm text-muted hover:text-foreground hover:border-primary/50 transition-colors"
                >
                  <Plus className="w-4 h-4" />
                  Add Destination
                </button>
              )}

              {errors.destinations?.root && (
                <p className="text-xs text-destructive">{errors.destinations.root.message}</p>
              )}
            </div>
          </div>

        </div>

        {/* Submit */}
        <div className="flex justify-end">
        <Button
          type="submit"
          variant="primary"
          disabled={isLoading}
          className="px-8 py-3 text-base mt-2"
        >
          {isLoading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Searching flights…
            </>
          ) : (
            <>
              <Search className="w-4 h-4" />
              Find Best Route
            </>
          )}
        </Button>
        </div>
      </form>
    </Card>
  );
}
