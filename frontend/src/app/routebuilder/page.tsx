/*
 * page.tsx - RouteBuilder page entry point.
 *
 * Thin server component wrapping the client content in Suspense so that
 * hook calls inside RouteBuilderContent resolve during static prerendering.
 */

import { Suspense } from "react";
import { RouteBuilderContent } from "@/components/RouteBuilderContent";

export default function RouteBuilderPage() {
  return (
    <Suspense>
      <RouteBuilderContent />
    </Suspense>
  );
}
