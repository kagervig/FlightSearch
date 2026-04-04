import { cn } from "@/lib/utils";
import { HTMLAttributes } from "react";

function Card({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("glass p-6", className)} {...props}>
      {children}
    </div>
  );
}

export { Card };
