/*
 * page.tsx - FlightSearch Frontend
 *
 * This is the main page of the FlightSearch app. It provides a form for users to:
 *   1. Enter their home city (airport code like JFK, LAX)
 *   2. Select how many cities they want to visit (1-5)
 *   3. Enter destination cities
 *   4. Choose to optimize by price or duration
 *
 * When "Find Best Route" is clicked, it calls the backend API:
 *   GET /api/flights/multicity?from=JFK&destinations=LAX,ORD
 *
 * The backend finds optimal multi-city routes and returns flight options per leg.
 *
 * Key concepts:
 *   - useState: React hook to store data that changes (form inputs, results)
 *   - fetch: JavaScript function to make HTTP requests to the backend
 *   - async/await: Way to handle asynchronous operations (API calls)
 */

"use client";  // This tells Next.js this component runs in the browser (client-side)

import { useState } from "react";

// API_URL comes from environment variable, with fallback for local development
// In production (Vercel), this is set to your Railway backend URL
const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// TypeScript interfaces define the shape of our data
// This helps catch errors and provides autocomplete
interface FlightOption {
  flightNumber: string;
  price: number;
  departureTime: string;
  arrivalTime: string;
  durationMinutes: number;
  cheapest: boolean;
}

interface Leg {
  from: string;
  to: string;
  fromCity: string;
  fromCountry: string;
  toCity: string;
  toCountry: string;
  flights: FlightOption[];
}

interface Route {
  airports: string[];
  cheapestTotalPrice: number;
  legs: Leg[];
}

interface SearchResult {
  from: string;
  routes: Route[];
}

export default function Home() {
  // useState creates "state variables" - data that can change and triggers re-render
  // Each useState returns [currentValue, setterFunction]

  const [numCities, setNumCities] = useState(1);           // How many cities to visit
  const [homeCity, setHomeCity] = useState("");             // Origin airport code
  const [destinations, setDestinations] = useState<string[]>([
    "",                                                      // Array of destination airport codes
  ]);
  const [optimizeBy, setOptimizeBy] = useState<"price" | "duration">("price");  // Optimization mode
  const [loading, setLoading] = useState(false);            // Is API call in progress?
  const [results, setResults] = useState<SearchResult | null>(null);  // API response
  const [error, setError] = useState<string | null>(null);  // Error message if any

  const handleNumCitiesChange = (num: number) => {
    setNumCities(num);
    const newDestinations = [...destinations];
    while (newDestinations.length < num) {
      newDestinations.push("");
    }
    while (newDestinations.length > num) {
      newDestinations.pop();
    }
    setDestinations(newDestinations);
  };

  const updateDestination = (index: number, value: string) => {
    const newDestinations = [...destinations];
    newDestinations[index] = value;
    setDestinations(newDestinations);
  };

  const formatDuration = (minutes: number) => {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  };

  // Backend sends LocalTime as HH:MM:SS — strip seconds for display
  const formatTime = (time: string) => time.slice(0, 5);

  // tracks which legs have their alternative flights expanded
  const [expandedLegs, setExpandedLegs] = useState<Record<string, boolean>>({});
  // tracks whether the "other routes" section is visible
  const [showOtherRoutes, setShowOtherRoutes] = useState(false);

  const toggleLeg = (key: string) => {
    setExpandedLegs(prev => ({ ...prev, [key]: !prev[key] }));
  };

  /**
   * handleSearch - Called when user clicks "Find Best Route"
   *
   * This function:
   *   1. Validates that home city and at least one destination are entered
   *   2. Calls the multi-city backend API
   *   3. Updates the UI with results or error
   */
  const handleSearch = async () => {
    if (!homeCity) {
      setError("Please enter your home city");
      return;
    }

    const selectedCities = destinations.map(d => d.toUpperCase()).filter(c => c);
    if (selectedCities.length === 0) {
      setError("Please enter at least one destination");
      return;
    }

    setLoading(true);
    setError(null);
    setResults(null);
    setExpandedLegs({});
    setShowOtherRoutes(false);

    try {
      const destinationsParam = selectedCities.join(",");
      const response = await fetch(
        `${API_URL}/api/flights/multicity?from=${homeCity.toUpperCase()}&destinations=${destinationsParam}`
      );

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || "Failed to search routes");
      }

      const data: SearchResult = await response.json();
      setResults(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen">
      {/* Navigation */}
      <nav className="fixed top-0 w-full bg-white/80 dark:bg-slate-900/80 backdrop-blur-md z-50 border-b border-slate-200 dark:border-slate-800">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <PlaneIcon className="w-8 h-8 text-sky-600 dark:text-sky-400" />
            <span className="text-xl font-bold text-slate-900 dark:text-white">FlightSearch</span>
          </div>
          <div className="flex items-center gap-6">
            <a href="#features" className="text-slate-600 dark:text-slate-300 hover:text-sky-600 dark:hover:text-sky-400 transition-colors">
              Features
            </a>
            <button className="bg-sky-600 hover:bg-sky-700 text-white px-4 py-2 rounded-lg font-medium transition-colors">
              Plan Trip
            </button>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-start">
            <div className="lg:sticky lg:top-32">
              <h1 className="text-5xl font-bold text-slate-900 dark:text-white leading-tight mb-6">
                Plan Your
                <span className="text-sky-600 dark:text-sky-400"> Multi-City </span>
                Adventure
              </h1>
              <p className="text-xl text-slate-600 dark:text-slate-300 mb-8">
                Visit up to 5 cities and let our algorithms find the optimal route based on price, duration, or number of stops.
              </p>
              <div className="space-y-4 text-slate-600 dark:text-slate-400">
                <div className="flex items-center gap-3">
                  <CheckIcon className="w-5 h-5 text-sky-600" />
                  <span>Optimized multi-city routing</span>
                </div>
                <div className="flex items-center gap-3">
                  <CheckIcon className="w-5 h-5 text-sky-600" />
                  <span>Compare prices across multiple routes</span>
                </div>
                <div className="flex items-center gap-3">
                  <CheckIcon className="w-5 h-5 text-sky-600" />
                  <span>Visit up to 5 cities in one trip</span>
                </div>
              </div>
            </div>

            {/* Trip Planner Card */}
            <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl p-8 border border-slate-200 dark:border-slate-700">
              <h2 className="text-xl font-semibold text-slate-900 dark:text-white mb-6">Plan Your Trip</h2>

              <div className="space-y-6">
                {/* Number of Cities */}
                <div>
                  <label className="block text-sm font-medium text-slate-600 dark:text-slate-400 mb-3">
                    How many cities do you want to visit?
                  </label>
                  <div className="flex gap-2">
                    {[1, 2, 3, 4, 5].map((num) => (
                      <button
                        key={num}
                        onClick={() => handleNumCitiesChange(num)}
                        className={`w-12 h-12 rounded-lg font-semibold transition-all ${
                          numCities === num
                            ? "bg-sky-600 text-white"
                            : "bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600"
                        }`}
                      >
                        {num}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Home City */}
                <div>
                  <label className="block text-sm font-medium text-slate-600 dark:text-slate-400 mb-2">
                    Home City (Starting Point)
                  </label>
                  <input
                    type="text"
                    value={homeCity}
                    onChange={(e) => setHomeCity(e.target.value.toUpperCase())}
                    placeholder="e.g. JFK, LAX, ORD"
                    className="w-full px-4 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-sky-500 focus:border-transparent outline-none"
                  />
                </div>

                {/* Destinations */}
                <div>
                  <label className="block text-sm font-medium text-slate-600 dark:text-slate-400 mb-3">
                    Destinations
                  </label>
                  <div className="space-y-3">
                    {destinations.map((dest, index) => (
                      <div key={index} className="flex gap-3 items-center">
                        <div className="w-8 h-8 rounded-full bg-sky-100 dark:bg-sky-900 text-sky-600 dark:text-sky-400 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                          {index + 1}
                        </div>
                        <input
                          type="text"
                          value={dest}
                          onChange={(e) => updateDestination(index, e.target.value.toUpperCase())}
                          placeholder="City code (e.g. LAX)"
                          className="flex-1 px-4 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-sky-500 focus:border-transparent outline-none"
                        />
                      </div>
                    ))}
                  </div>
                </div>

                {/* Optimize By */}
                <div>
                  <label className="block text-sm font-medium text-slate-600 dark:text-slate-400 mb-2">
                    Optimize By
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => setOptimizeBy("price")}
                      className={`px-4 py-2 rounded-lg font-medium text-sm border-2 transition-all ${
                        optimizeBy === "price"
                          ? "bg-sky-100 dark:bg-sky-900 text-sky-700 dark:text-sky-300 border-sky-500"
                          : "bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 border-transparent hover:border-slate-300 dark:hover:border-slate-500"
                      }`}
                    >
                      Price
                    </button>
                    <button
                      onClick={() => setOptimizeBy("duration")}
                      className={`px-4 py-2 rounded-lg font-medium text-sm border-2 transition-all ${
                        optimizeBy === "duration"
                          ? "bg-sky-100 dark:bg-sky-900 text-sky-700 dark:text-sky-300 border-sky-500"
                          : "bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 border-transparent hover:border-slate-300 dark:hover:border-slate-500"
                      }`}
                    >
                      Duration
                    </button>
                  </div>
                </div>

                {/* Error Message */}
                {error && (
                  <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
                    {error}
                  </div>
                )}

                <button
                  onClick={handleSearch}
                  disabled={loading}
                  className="w-full bg-sky-600 hover:bg-sky-700 disabled:bg-sky-400 text-white py-4 rounded-lg font-medium text-lg transition-colors flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <>
                      <SpinnerIcon className="w-5 h-5 animate-spin" />
                      Searching...
                    </>
                  ) : (
                    <>
                      <SearchIcon className="w-5 h-5" />
                      Find Best Route
                    </>
                  )}
                </button>

                {/* Results */}
                {results && (
                  <div className="mt-6 space-y-4">
                    {results.routes.length === 0 ? (
                      <div className="p-4 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700">
                        <p className="text-slate-500 dark:text-slate-400 text-sm">No valid routes found for these destinations</p>
                      </div>
                    ) : (
                      <>
                        {/* Cheapest route */}
                        <div className="p-4 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700">
                          <div className="flex justify-between items-center mb-4">
                            <h3 className="font-semibold text-slate-900 dark:text-white">
                              Best Route
                            </h3>
                            <span className="text-lg font-bold text-sky-600 dark:text-sky-400">
                              ${results.routes[0].cheapestTotalPrice}
                            </span>
                          </div>

                          <div className="text-sm text-slate-500 dark:text-slate-400 mb-3">
                            {results.routes[0].airports.join(" → ")}
                          </div>

                          <div className="space-y-2">
                            {results.routes[0].legs.map((leg, legIndex) => {
                              const cheapestFlight = leg.flights.find(f => f.cheapest) || leg.flights[0];
                              const legKey = `0-${legIndex}`;
                              const isExpanded = expandedLegs[legKey];

                              return (
                                <div key={legIndex} className="bg-white dark:bg-slate-800 rounded-lg p-3">
                                  <button
                                    onClick={() => toggleLeg(legKey)}
                                    className="w-full flex justify-between items-center cursor-pointer"
                                  >
                                    <span className="font-medium text-slate-900 dark:text-white">
                                      {leg.from} → {leg.to}
                                    </span>
                                    <span className="font-semibold text-sky-600 dark:text-sky-400">
                                      ${cheapestFlight.price}
                                    </span>
                                  </button>

                                  {isExpanded && (
                                    <div className="mt-3 space-y-2">
                                      {leg.flights.map((f, fi) => (
                                        <div
                                          key={fi}
                                          className={`p-3 rounded-lg border ${
                                            f.cheapest
                                              ? "border-sky-300 dark:border-sky-700 bg-sky-50 dark:bg-sky-900/30"
                                              : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900"
                                          }`}
                                        >
                                          <div className="flex justify-between items-center mb-1">
                                            <span className="font-medium text-slate-900 dark:text-white text-sm">
                                              {f.flightNumber}
                                              {f.cheapest && (
                                                <span className="ml-2 text-xs text-sky-600 dark:text-sky-400 font-semibold">
                                                  Cheapest
                                                </span>
                                              )}
                                            </span>
                                            <span className="font-semibold text-slate-900 dark:text-white text-sm">
                                              ${f.price}
                                            </span>
                                          </div>
                                          <div className="text-xs text-slate-500 dark:text-slate-400 space-y-0.5">
                                            <div>{formatTime(f.departureTime)} → {formatTime(f.arrivalTime)} · {formatDuration(f.durationMinutes)}</div>
                                            {leg.fromCity && leg.toCity && (
                                              <div>{leg.fromCity}, {leg.fromCountry} → {leg.toCity}, {leg.toCountry}</div>
                                            )}
                                          </div>
                                        </div>
                                      ))}
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        </div>

                        {/* Other routes toggle */}
                        {results.routes.length > 1 && (
                          <div>
                            <button
                              onClick={() => setShowOtherRoutes(!showOtherRoutes)}
                              className="w-full text-center text-sm text-sky-600 dark:text-sky-400 hover:underline py-2"
                            >
                              {showOtherRoutes ? "Hide" : "Show"} {results.routes.length - 1} other route{results.routes.length > 2 ? "s" : ""}
                            </button>

                            {showOtherRoutes && (
                              <div className="space-y-3 mt-2">
                                {results.routes.slice(1).map((route, routeIndex) => (
                                  <div key={routeIndex} className="p-3 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700">
                                    <div className="flex justify-between items-center mb-1">
                                      <span className="text-sm text-slate-600 dark:text-slate-400">
                                        {route.airports.join(" → ")}
                                      </span>
                                      <span className="font-semibold text-slate-700 dark:text-slate-300">
                                        ${route.cheapestTotalPrice}
                                      </span>
                                    </div>
                                    <div className="space-y-1">
                                      {route.legs.map((leg, legIndex) => {
                                        const cheapestFlight = leg.flights.find(f => f.cheapest) || leg.flights[0];
                                        return (
                                          <div key={legIndex} className="flex justify-between text-xs text-slate-500 dark:text-slate-400">
                                            <span>{leg.from} → {leg.to}: {cheapestFlight.flightNumber}</span>
                                            <span>${cheapestFlight.price}</span>
                                          </div>
                                        );
                                      })}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )}
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-12 px-6 bg-slate-100 dark:bg-slate-800/50">
        <div className="max-w-6xl mx-auto">
          <div className="grid grid-cols-3 gap-8 text-center">
            <div>
              <div className="text-4xl font-bold text-sky-600 dark:text-sky-400">100</div>
              <div className="text-slate-600 dark:text-slate-400 mt-1">Global Airports</div>
            </div>
            <div>
              <div className="text-4xl font-bold text-sky-600 dark:text-sky-400">5</div>
              <div className="text-slate-600 dark:text-slate-400 mt-1">Max Cities</div>
            </div>
            <div>
              <div className="text-4xl font-bold text-sky-600 dark:text-sky-400">2</div>
              <div className="text-slate-600 dark:text-slate-400 mt-1">Optimization Modes</div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-slate-900 dark:text-white mb-4">Powerful Route Finding</h2>
            <p className="text-lg text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
              Built on graph theory and pathfinding algorithms to find you the best routes across the network.
            </p>
          </div>
          <div className="grid md:grid-cols-2 gap-8 max-w-2xl mx-auto">
            <FeatureCard
              icon={<DollarIcon className="w-8 h-8" />}
              title="Cheapest Route"
              description="Finds the lowest total cost path across all your destinations."
            />
            <FeatureCard
              icon={<ClockIcon className="w-8 h-8" />}
              title="Fastest Route"
              description="Optimize for minimum travel time including flight duration."
            />
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-6 border-t border-slate-200 dark:border-slate-800">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2">
            <PlaneIcon className="w-6 h-6 text-sky-600 dark:text-sky-400" />
            <span className="font-semibold text-slate-900 dark:text-white">FlightSearch</span>
          </div>
          <div className="text-slate-500 dark:text-slate-400 text-sm">
            Built for multi-city travel
          </div>
        </div>
      </footer>
    </div>
  );
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode; title: string; description: string }) {
  return (
    <div className="bg-white dark:bg-slate-800 rounded-xl p-6 border border-slate-200 dark:border-slate-700 hover:shadow-lg transition-shadow">
      <div className="w-14 h-14 bg-sky-100 dark:bg-sky-900/50 rounded-lg flex items-center justify-center text-sky-600 dark:text-sky-400 mb-4">
        {icon}
      </div>
      <h3 className="text-xl font-semibold text-slate-900 dark:text-white mb-2">{title}</h3>
      <p className="text-slate-600 dark:text-slate-400">{description}</p>
    </div>
  );
}

function PlaneIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z" />
    </svg>
  );
}

function SearchIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.3-4.3" />
    </svg>
  );
}

function SpinnerIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
    </svg>
  );
}

function CheckIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}

function DollarIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="12" x2="12" y1="2" y2="22" />
      <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
    </svg>
  );
}

function ClockIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </svg>
  );
}
