import { cn } from "@/lib/utils";
import { HTMLAttributes } from "react";

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: "default" | "amber" | "red" | "green";
}

function Badge({ className, variant = "default", children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium",
        variant === "default" && "bg-primary/10 text-primary",
        variant === "amber" && "bg-amber-500/10 text-amber-400",
        variant === "red" && "bg-red-500/10 text-red-400",
        variant === "green" && "bg-green-500/10 text-green-400",
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
}

export { Badge };
