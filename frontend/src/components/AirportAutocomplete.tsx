"use client";

/**
 * Airport search input with debounced autocomplete.
 * Queries the backend airport search endpoint and presents a dropdown of suggestions.
 * Allows direct IATA code entry as well as city-name search.
 */

import { useState, useEffect, useRef, ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { Input } from "@/components/ui/Input";
import { useDebounce } from "@/hooks/useDebounce";
import { cn } from "@/lib/utils";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface AirportSuggestion {
  code: string;
  name: string;
  city: string;
  country: string;
}

interface AirportAutocompleteProps {
  value: string;
  onChange: (code: string, city: string) => void;
  placeholder?: string;
  icon?: ReactNode;
}

export function AirportAutocomplete({
  value,
  onChange,
  placeholder,
  icon,
}: AirportAutocompleteProps) {
  const [inputText, setInputText] = useState(value);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const debouncedInput = useDebounce(inputText, 300);

  // Sync display text when value is cleared externally (e.g. form reset)
  useEffect(() => {
    if (!value) setInputText("");
  }, [value]);

  const { data: suggestions = [], isFetching } = useQuery<AirportSuggestion[]>({
    queryKey: ["airports", debouncedInput],
    queryFn: async () => {
      const res = await fetch(
        `${API_URL}/api/airports/search?city=${encodeURIComponent(debouncedInput)}`
      );
      if (!res.ok) throw new Error("Airport search failed");
      return res.json();
    },
    enabled: debouncedInput.length >= 2,
    staleTime: 60_000,
  });

  // Open dropdown whenever we have results for the current input
  useEffect(() => {
    if (suggestions.length > 0 && inputText.length >= 2) {
      setIsOpen(true);
    }
  }, [suggestions, inputText]);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const text = e.target.value;
    setInputText(text);
    // Allow direct IATA code entry — propagate on every keystroke
    onChange(text.toUpperCase(), "");
    if (text.length < 2) setIsOpen(false);
  }

  function handleSelect(suggestion: AirportSuggestion) {
    const display = `${suggestion.city} (${suggestion.code})`;
    setInputText(display);
    setIsOpen(false);
    onChange(suggestion.code, suggestion.city);
  }

  return (
    <div ref={containerRef} className="relative">
      <Input
        value={inputText}
        onChange={handleInputChange}
        onFocus={() => suggestions.length > 0 && setIsOpen(true)}
        placeholder={placeholder}
        leftIcon={
          isFetching ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            icon
          )
        }
        autoComplete="off"
      />

      {isOpen && suggestions.length > 0 && (
        <ul className={cn(
          "absolute z-50 mt-1 w-full overflow-hidden rounded-xl border border-border/50",
          "bg-card shadow-xl shadow-black/20",
        )}>
          {suggestions.map((s) => (
            <li key={s.code}>
              <button
                type="button"
                onMouseDown={(e) => {
                  // mousedown fires before blur — prevent input blur from closing dropdown first
                  e.preventDefault();
                  handleSelect(s);
                }}
                className="w-full px-4 py-2.5 text-left text-sm hover:bg-primary/10 transition-colors"
              >
                <span className="font-medium text-primary">{s.code}</span>
                <span className="text-foreground ml-2">{s.city}</span>
                <span className="text-muted ml-1">· {s.country}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
