/** Static configuration for the popular route cards shown on the home page. */

import type { SearchFormValues } from "@/components/FlightSearchForm";

export interface PopularRoute {
  title: string;
  values: Omit<SearchFormValues, "departureDate">;
}

export const POPULAR_ROUTES: PopularRoute[] = [
  {
    title: "Snowbird Sortie",
    values: {
      homeAirport: { code: "YYZ", city: "Toronto" },
      destinations: [
        { code: "MIA", city: "Miami", days: 3 },
        { code: "MEX", city: "Mexico City", days: 3 },
        { code: "LAX", city: "Los Angeles", days: 3 },
      ],
      optimizeBy: "price",
    },
  },
  {
    title: "East Asian Escape",
    values: {
      homeAirport: { code: "ICN", city: "Seoul" },
      destinations: [
        { code: "BKK", city: "Bangkok", days: 3 },
        { code: "DPS", city: "Bali", days: 3 },
        { code: "HKG", city: "Hong Kong", days: 3 },
        { code: "CEB", city: "Cebu", days: 3 },
      ],
      optimizeBy: "price",
    },
  },
  {
    title: "European Excursion",
    values: {
      homeAirport: { code: "LGW", city: "London" },
      destinations: [
        { code: "CDG", city: "Paris", days: 3 },
        { code: "FCO", city: "Rome", days: 3 },
        { code: "BUD", city: "Budapest", days: 3 },
        { code: "IBZ", city: "Ibiza", days: 3 },
      ],
      optimizeBy: "price",
    },
  },
];
