/** Static configuration for the popular route cards shown on the home page. */

import type { SearchFormValues } from "@/components/FlightSearchForm";

export interface PopularRoute {
  title: string;
  values: Omit<SearchFormValues, "departureDate">;
  /** Lat/lng for each airport code in this route, keyed by IATA code. */
  coords: Record<string, { lat: number; lng: number }>;
}

export const POPULAR_ROUTES: PopularRoute[] = [
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
    coords: {
      LGW: { lat: 51.1537, lng: -0.1821 },
      CDG: { lat: 49.0097, lng: 2.5479 },
      FCO: { lat: 41.7999, lng: 12.2462 },
      BUD: { lat: 47.4298, lng: 19.2611 },
      IBZ: { lat: 38.8729, lng: 1.3733 },
    },
  },
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
    coords: {
      YYZ: { lat: 43.6777, lng: -79.6248 },
      MIA: { lat: 25.7959, lng: -80.2870 },
      MEX: { lat: 19.4363, lng: -99.0721 },
      LAX: { lat: 33.9425, lng: -118.4081 },
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
    coords: {
      ICN: { lat: 37.4691, lng: 126.4510 },
      BKK: { lat: 13.6900, lng: 100.7501 },
      DPS: { lat: -8.7482, lng: 115.1670 },
      HKG: { lat: 22.3080, lng: 113.9185 },
      CEB: { lat: 10.3075, lng: 123.9791 },
    },
  },
];
