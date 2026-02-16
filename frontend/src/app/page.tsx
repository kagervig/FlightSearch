/*
 * page.tsx - FlightSearch Frontend
 *
 * This is the main page of the FlightSearch app. It provides a form for users to:
 *   1. Enter their home city (airport code like JFK, LAX)
 *   2. Select how many cities they want to visit (1-5)
 *   3. Enter destination cities and days per city
 *   4. Choose to optimize by price or duration
 *
 * When "Find Best Route" is clicked, it calls the backend API:
 *   GET /api/routes/cheapest?from=JFK
 *
 * The backend runs Dijkstra's algorithm and returns the cheapest price
 * to reach every airport from the origin.
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
interface Route {
  destination: string;       // Airport code (e.g., "LAX")
  destinationName: string;   // Full name (e.g., "Los Angeles International Airport")
  cheapestPrice: number;     // Price in dollars
}

interface SearchResult {
  from: string;              // Origin airport code
  routes: Route[];           // Array of destinations with prices
}

export default function Home() {
  // useState creates "state variables" - data that can change and triggers re-render
  // Each useState returns [currentValue, setterFunction]

  const [numCities, setNumCities] = useState(1);           // How many cities to visit
  const [homeCity, setHomeCity] = useState("");             // Origin airport code
  const [startDate, setStartDate] = useState("");           // Trip start date
  const [destinations, setDestinations] = useState<{ city: string; days: number }[]>([
    { city: "", days: 3 },                                  // Array of destination cities
  ]);
  const [optimizeBy, setOptimizeBy] = useState<"price" | "duration">("price");  // Optimization mode
  const [loading, setLoading] = useState(false);            // Is API call in progress?
  const [results, setResults] = useState<SearchResult | null>(null);  // API response
  const [error, setError] = useState<string | null>(null);  // Error message if any

  const handleNumCitiesChange = (num: number) => {
    setNumCities(num);
    const newDestinations = [...destinations];
    while (newDestinations.length < num) {
      newDestinations.push({ city: "", days: 3 });
    }
    while (newDestinations.length > num) {
      newDestinations.pop();
    }
    setDestinations(newDestinations);
  };

  const updateDestination = (index: number, field: "city" | "days", value: string | number) => {
    const newDestinations = [...destinations];
    if (field === "city") {
      newDestinations[index].city = value as string;
    } else {
      newDestinations[index].days = value as number;
    }
    setDestinations(newDestinations);
  };

  /**
   * handleSearch - Called when user clicks "Find Best Route"
   *
   * This function:
   *   1. Validates that home city is entered
   *   2. Calls the backend API with the origin airport
   *   3. Filters results to only show selected destinations
   *   4. Updates the UI with results or error
   */
  const handleSearch = async () => {
    // Validate input
    if (!homeCity) {
      setError("Please enter your home city");
      return;
    }

    // Set loading state (shows spinner, disables button)
    setLoading(true);
    setError(null);
    setResults(null);

    try {
      // Call the backend API
      // fetch() makes an HTTP request and returns a Promise
      // await pauses execution until the Promise resolves
      const response = await fetch(`${API_URL}/api/routes/cheapest?from=${homeCity.toUpperCase()}`);

      // Check if request was successful (status 200-299)
      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || "Failed to search routes");
      }

      // Parse JSON response into JavaScript object
      const data: SearchResult = await response.json();

      // Filter results to only show destinations the user selected
      // If no destinations entered, show all routes
      const selectedCities = destinations.map(d => d.city.toUpperCase()).filter(c => c);
      if (selectedCities.length > 0) {
        data.routes = data.routes.filter(r => selectedCities.includes(r.destination));
      }

      // Sort by price (cheapest first)
      data.routes.sort((a, b) => a.cheapestPrice - b.cheapestPrice);

      // Update state with results (triggers re-render to show results)
      setResults(data);
    } catch (err) {
      // Handle any errors (network issues, invalid response, etc.)
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      // Always runs, whether success or error
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
                  <span>Optimized routing with Dijkstra&apos;s algorithm</span>
                </div>
                <div className="flex items-center gap-3">
                  <CheckIcon className="w-5 h-5 text-sky-600" />
                  <span>Compare prices across multiple routes</span>
                </div>
                <div className="flex items-center gap-3">
                  <CheckIcon className="w-5 h-5 text-sky-600" />
                  <span>Flexible trip duration per city</span>
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
                    onChange={(e) => setHomeCity(e.target.value)}
                    placeholder="e.g. JFK, LAX, ORD"
                    className="w-full px-4 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-sky-500 focus:border-transparent outline-none"
                  />
                </div>

                {/* Start Date */}
                <div>
                  <label className="block text-sm font-medium text-slate-600 dark:text-slate-400 mb-2">
                    Start Date
                  </label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="w-full px-4 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-white focus:ring-2 focus:ring-sky-500 focus:border-transparent outline-none"
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
                          value={dest.city}
                          onChange={(e) => updateDestination(index, "city", e.target.value)}
                          placeholder="City code (e.g. LAX)"
                          className="flex-1 px-4 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-sky-500 focus:border-transparent outline-none"
                        />
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <input
                            type="number"
                            min="1"
                            max="30"
                            value={dest.days}
                            onChange={(e) => updateDestination(index, "days", parseInt(e.target.value) || 1)}
                            className="w-16 px-3 py-3 rounded-lg border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-white text-center focus:ring-2 focus:ring-sky-500 focus:border-transparent outline-none"
                          />
                          <span className="text-sm text-slate-500 dark:text-slate-400">days</span>
                        </div>
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
                  <div className="mt-6 p-4 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700">
                    <h3 className="font-semibold text-slate-900 dark:text-white mb-3">
                      Routes from {results.from}
                    </h3>
                    {results.routes.length === 0 ? (
                      <p className="text-slate-500 dark:text-slate-400 text-sm">No routes found for selected destinations</p>
                    ) : (
                      <div className="space-y-2">
                        {results.routes.map((route, i) => (
                          <div key={i} className="flex justify-between items-center p-3 bg-white dark:bg-slate-800 rounded-lg">
                            <div>
                              <span className="font-medium text-slate-900 dark:text-white">{route.destination}</span>
                              <span className="text-slate-500 dark:text-slate-400 text-sm ml-2">{route.destinationName}</span>
                            </div>
                            <span className="font-semibold text-sky-600 dark:text-sky-400">${route.cheapestPrice}</span>
                          </div>
                        ))}
                      </div>
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
              description="Dijkstra's algorithm finds the lowest total cost path, even with layovers."
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
            Built with graph algorithms
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
