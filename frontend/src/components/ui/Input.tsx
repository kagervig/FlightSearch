"use client";

import { cn } from "@/lib/utils";
import { InputHTMLAttributes, ReactNode, forwardRef } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  leftIcon?: ReactNode;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, leftIcon, ...props }, ref) => {
    return (
      <div className="relative flex items-center">
        {leftIcon && (
          <span className="absolute left-3 text-muted pointer-events-none">
            {leftIcon}
          </span>
        )}
        <input
          ref={ref}
          className={cn(
            "w-full rounded-xl border border-border/50 bg-background/50 py-2.5 text-sm text-foreground placeholder:text-muted",
            "focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary/50",
            "transition-all duration-200",
            leftIcon ? "pl-9 pr-4" : "px-4",
            className
          )}
          {...props}
        />
      </div>
    );
  }
);
Input.displayName = "Input";

export { Input };
