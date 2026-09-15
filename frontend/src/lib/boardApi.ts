/**
 * Types and fetch function for the /api/flights/board endpoint.
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export interface BoardFlight {
  flightNumber: string;
  airlineName: string;
  aircraftName: string;
  origin: string;
  originCity: string;
  originLat: number;
  originLon: number;
  destination: string;
  destinationCity: string;
  destinationLat: number;
  destinationLon: number;
  departureTime: string;
  arrivalTime: string;
  durationMinutes: number;
  price: number;
}

export interface BoardResponse {
  airport: string;
  hubLat: number;
  hubLon: number;
  departures: BoardFlight[];
  arrivals: BoardFlight[];
}

export async function fetchBoardFlights(airport: string): Promise<BoardResponse> {
  const res = await fetch(`${API_URL}/api/flights/board?airport=${airport}`);

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? "Failed to load board");
  }

  return res.json();
}
