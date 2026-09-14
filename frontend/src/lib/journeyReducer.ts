/** Maximum number of city hops after the home airport. */
const MAX_HOPS = 10;

export interface JourneyHop {
  code: string;
  nights: number;
  price: number;
}

export interface JourneyState {
  journey: JourneyHop[];
  closed: boolean;
}

export type JourneyAction =
  | { type: "APPEND_HOP"; code: string; price: number }
  | { type: "FLY_HOME"; price: number }
  | { type: "ONE_WAY" }
  | { type: "RESET" }
  | { type: "SET_HOME"; code: string }
  | { type: "SET_NIGHTS"; index: number; nights: number };

export function initialState(homeCode: string): JourneyState {
  return {
    journey: [{ code: homeCode, nights: 0, price: 0 }],
    closed: false,
  };
}

export function journeyReducer(
  state: JourneyState,
  action: JourneyAction
): JourneyState {
  switch (action.type) {
    case "APPEND_HOP": {
      if (state.closed) return state;
      const hops = state.journey.length - 1;
      if (hops >= MAX_HOPS) return state;
      const visited = new Set(state.journey.map((h) => h.code));
      if (visited.has(action.code)) return state;
      return {
        ...state,
        journey: [
          ...state.journey,
          { code: action.code, nights: 1, price: action.price },
        ],
      };
    }

    case "FLY_HOME": {
      const home = state.journey[0].code;
      const currentCode = state.journey.at(-1)?.code;
      if (currentCode === home) {
        return { ...state, closed: true };
      }
      return {
        ...state,
        journey: [
          ...state.journey,
          { code: home, nights: 0, price: action.price },
        ],
        closed: true,
      };
    }

    case "ONE_WAY":
      return { ...state, closed: true };

    case "RESET":
      return initialState(state.journey[0].code);

    case "SET_HOME":
      return initialState(action.code);

    case "SET_NIGHTS": {
      const { index, nights } = action;
      if (index < 0 || index >= state.journey.length) return state;
      const minNights = index === 0 ? 0 : 1;
      const clamped = Math.max(minNights, nights);
      const journey = state.journey.map((h, i) =>
        i === index ? { ...h, nights: clamped } : h
      );
      return { ...state, journey };
    }

    default:
      return state;
  }
}

export interface DerivedJourney {
  hopCount: number;
  visitedSet: Set<string>;
  runningTotal: number;
  daysAway: number;
  backHomeDate: Date;
  atCap: boolean;
}

export function deriveJourney(
  state: JourneyState,
  departureDate: Date
): DerivedJourney {
  const hopCount = state.journey.length - 1;
  const visitedSet = new Set(state.journey.map((h) => h.code));
  const runningTotal = state.journey.reduce((sum, h) => sum + h.price, 0);
  const totalNights = state.journey.reduce((sum, h) => sum + h.nights, 0);
  // each flight leg takes one day of travel
  const daysAway = totalNights + hopCount;
  const backHomeDate = new Date(departureDate);
  backHomeDate.setDate(backHomeDate.getDate() + daysAway);
  const atCap = hopCount >= MAX_HOPS;

  return { hopCount, visitedSet, runningTotal, daysAway, backHomeDate, atCap };
}
