"use client";

/**
 * Toggles between light and dark mode by adding/removing the `.dark` class
 * on <html>. Persists the preference to localStorage.
 */

import { useEffect, useState } from "react";
import { Sun, Moon } from "lucide-react";
import { cn } from "@/lib/utils";

export function ThemeToggle() {
  const [isDark, setIsDark] = useState(true);

  useEffect(() => {
    // Read the class that the no-flash script already applied
    setIsDark(document.documentElement.classList.contains("dark"));
  }, []);

  function toggle() {
    const next = !isDark;
    setIsDark(next);
    localStorage.setItem("theme", next ? "dark" : "light");
    document.documentElement.classList.toggle("dark", next);
  }

  return (
    <button
      onClick={toggle}
      aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
      style={{ color: "var(--hero-nav-color)", borderColor: "color-mix(in srgb, var(--hero-nav-color) 30%, transparent)" }}
      className={cn(
        "w-9 h-9 rounded-xl flex items-center justify-center transition-colors border",
        "hover:bg-black/5 dark:hover:bg-white/10"
      )}
    >
      {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
    </button>
  );
}
