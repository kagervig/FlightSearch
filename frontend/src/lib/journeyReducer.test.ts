import { describe, it, expect } from "vitest";
import {
  journeyReducer,
  deriveJourney,
  initialState,
  type JourneyState,
} from "./journeyReducer";

const HOME = "LHR";

function makeState(overrides: Partial<JourneyState> = {}): JourneyState {
  return { ...initialState(HOME), ...overrides };
}

// ---------------------------------------------------------------------------
// initialState
// ---------------------------------------------------------------------------

describe("initialState", () => {
  it("starts with a single hop at home with zero price and nights", () => {
    const s = initialState(HOME);
    expect(s.journey).toHaveLength(1);
    expect(s.journey[0]).toEqual({ code: HOME, nights: 0, price: 0 });
  });

  it("starts open", () => {
    expect(initialState(HOME).closed).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// APPEND_HOP
// ---------------------------------------------------------------------------

describe("APPEND_HOP", () => {
  it("adds a hop to the journey", () => {
    const s = journeyReducer(makeState(), {
      type: "APPEND_HOP",
      code: "CDG",
      price: 120,
    });
    expect(s.journey).toHaveLength(2);
    expect(s.journey[1]).toEqual({ code: "CDG", nights: 1, price: 120 });
  });

  it("does nothing when the journey is closed", () => {
    const s = makeState({ closed: true });
    const next = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    expect(next).toEqual(s);
  });

  it("does nothing when the destination is already in the journey", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    const next = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 80 });
    expect(next.journey).toHaveLength(2);
  });

  it("does nothing when revisiting the home airport mid-journey", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    const next = journeyReducer(s, { type: "APPEND_HOP", code: HOME, price: 50 });
    expect(next.journey).toHaveLength(2);
  });

  it("does nothing when the 10-city cap is reached", () => {
    let s = makeState();
    const codes = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"];
    for (const code of codes) {
      s = journeyReducer(s, { type: "APPEND_HOP", code, price: 100 });
    }
    expect(s.journey).toHaveLength(11); // home + 10 hops
    const next = journeyReducer(s, { type: "APPEND_HOP", code: "K", price: 100 });
    expect(next.journey).toHaveLength(11);
  });

  it("defaults new hops to 1 night", () => {
    const s = journeyReducer(makeState(), {
      type: "APPEND_HOP",
      code: "CDG",
      price: 120,
    });
    expect(s.journey[1].nights).toBe(1);
  });
});

// ---------------------------------------------------------------------------
// FLY_HOME
// ---------------------------------------------------------------------------

describe("FLY_HOME", () => {
  it("appends the home airport and closes the journey", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "FLY_HOME", price: 200 });
    expect(s.journey.at(-1)).toEqual({ code: HOME, nights: 0, price: 200 });
    expect(s.closed).toBe(true);
  });

  it("does not count the return leg toward the 10-city cap", () => {
    let s = makeState();
    const codes = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"];
    for (const code of codes) {
      s = journeyReducer(s, { type: "APPEND_HOP", code, price: 100 });
    }
    s = journeyReducer(s, { type: "FLY_HOME", price: 300 });
    expect(s.closed).toBe(true);
    expect(s.journey.at(-1)?.code).toBe(HOME);
    expect(s.journey).toHaveLength(12); // home + 10 hops + return
  });

  it("closes without appending when already at home", () => {
    const s = journeyReducer(makeState(), { type: "FLY_HOME", price: 0 });
    expect(s.journey).toHaveLength(1);
    expect(s.closed).toBe(true);
  });
});

// ---------------------------------------------------------------------------
// ONE_WAY
// ---------------------------------------------------------------------------

describe("ONE_WAY", () => {
  it("closes the journey without adding a return leg", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "ONE_WAY" });
    expect(s.closed).toBe(true);
    expect(s.journey.at(-1)?.code).toBe("CDG");
  });
});

// ---------------------------------------------------------------------------
// RESET
// ---------------------------------------------------------------------------

describe("RESET", () => {
  it("returns journey to the single home hop", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "RESET" });
    expect(s.journey).toHaveLength(1);
    expect(s.journey[0].code).toBe(HOME);
    expect(s.closed).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// SET_HOME
// ---------------------------------------------------------------------------

describe("SET_HOME", () => {
  it("resets the journey with the new home airport", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "SET_HOME", code: "JFK" });
    expect(s.journey).toHaveLength(1);
    expect(s.journey[0].code).toBe("JFK");
    expect(s.closed).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// SET_NIGHTS
// ---------------------------------------------------------------------------

describe("SET_NIGHTS", () => {
  it("updates nights for the given hop index", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "SET_NIGHTS", index: 1, nights: 3 });
    expect(s.journey[1].nights).toBe(3);
  });

  it("does not update nights below 1 for non-home hops", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "SET_NIGHTS", index: 1, nights: 0 });
    expect(s.journey[1].nights).toBe(1);
  });

  it("ignores out-of-bounds index", () => {
    const s = makeState();
    const next = journeyReducer(s, { type: "SET_NIGHTS", index: 99, nights: 5 });
    expect(next).toEqual(s);
  });
});

// ---------------------------------------------------------------------------
// deriveJourney
// ---------------------------------------------------------------------------

describe("deriveJourney", () => {
  const departure = new Date("2026-10-01");

  it("hopCount is zero for a fresh journey", () => {
    const { hopCount } = deriveJourney(makeState(), departure);
    expect(hopCount).toBe(0);
  });

  it("hopCount reflects added hops", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "APPEND_HOP", code: "IST", price: 90 });
    expect(deriveJourney(s, departure).hopCount).toBe(2);
  });

  it("runningTotal sums hop prices", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "APPEND_HOP", code: "IST", price: 90 });
    expect(deriveJourney(s, departure).runningTotal).toBe(210);
  });

  it("visitedSet contains all visited airport codes", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    const { visitedSet } = deriveJourney(s, departure);
    expect(visitedSet.has(HOME)).toBe(true);
    expect(visitedSet.has("CDG")).toBe(true);
  });

  it("daysAway equals sum of nights plus number of legs", () => {
    // home(0n) → CDG(2n) → IST(3n): 2 legs, 5 nights → 7 days
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "SET_NIGHTS", index: 1, nights: 2 });
    s = journeyReducer(s, { type: "APPEND_HOP", code: "IST", price: 90 });
    s = journeyReducer(s, { type: "SET_NIGHTS", index: 2, nights: 3 });
    expect(deriveJourney(s, departure).daysAway).toBe(7);
  });

  it("backHomeDate is departure plus daysAway", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    s = journeyReducer(s, { type: "SET_NIGHTS", index: 1, nights: 6 });
    // 1 leg + 6 nights = 7 days → 2026-10-08
    const { backHomeDate } = deriveJourney(s, departure);
    expect(backHomeDate.toISOString().slice(0, 10)).toBe("2026-10-08");
  });

  it("atCap is true when 10 hops are added", () => {
    let s = makeState();
    for (const code of ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"]) {
      s = journeyReducer(s, { type: "APPEND_HOP", code, price: 100 });
    }
    expect(deriveJourney(s, departure).atCap).toBe(true);
  });

  it("atCap is false before 10 hops", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 100 });
    expect(deriveJourney(s, departure).atCap).toBe(false);
  });

  it("cumulativeDistanceKm sums distanceKm from all hops", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120, distanceKm: 340 });
    s = journeyReducer(s, { type: "APPEND_HOP", code: "IST", price: 90, distanceKm: 2200 });
    expect(deriveJourney(s, departure).cumulativeDistanceKm).toBe(2540);
  });

  it("cumulativeDistanceKm is 0 when no distanceKm provided", () => {
    let s = makeState();
    s = journeyReducer(s, { type: "APPEND_HOP", code: "CDG", price: 120 });
    expect(deriveJourney(s, departure).cumulativeDistanceKm).toBe(0);
  });
});
