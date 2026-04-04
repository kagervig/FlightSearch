"use client";

import { cn } from "@/lib/utils";
import { ButtonHTMLAttributes, forwardRef } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "ghost" | "segment";
  active?: boolean; // for segment variant active state
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", active, children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        type={props.type ?? "button"}
        className={cn(
          "inline-flex items-center justify-center gap-2 rounded-xl text-sm font-medium transition-all duration-200 disabled:pointer-events-none disabled:opacity-50 cursor-pointer",
          variant === "primary" && [
            "bg-primary text-white px-5 py-2.5",
            "hover:brightness-110 hover:shadow-lg hover:shadow-primary/30",
            "active:brightness-95",
          ],
          variant === "ghost" && [
            "text-muted px-4 py-2",
            "hover:bg-card hover:text-foreground",
          ],
          variant === "segment" && [
            "px-4 py-2 rounded-lg",
            active
              ? "bg-primary text-white shadow-sm"
              : "text-muted hover:bg-border/40 hover:text-foreground",
          ],
          className
        )}
        {...props}
      >
        {children}
      </button>
    );
  }
);
Button.displayName = "Button";

export { Button };
