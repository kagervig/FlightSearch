/*
 * page.tsx - CityHopper home page entry point.
 *
 * Thin server component that wraps the client content in Suspense so that
 * useSearchParams() inside HomeContent can be resolved during static prerendering.
 */

import { Suspense } from "react";
import { HomeContent } from "./HomeContent";

export default function Page() {
  return (
    <Suspense>
      <HomeContent />
    </Suspense>
  );
}
