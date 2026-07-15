# CLAUDE.md

## Project

Flight search tool to help users find the cheapest or fastest combination of flights that takes them up to 5 destinations and back home again.

### Stack

Frontend:

- Next.js 16, React 19, TypeScript
- Tailwind CSS 4, Framer Motion
- TanStack React Query, React Hook Form + Zod
- Vercel hosting

Backend:

- Java 17, Javalin 6, Maven
- PostgreSQL, HikariCP
- Railway hosting

### Structure

Monorepo with `frontend/` and `backend/` directories.

```
frontend/src/
  app/              # Next.js app router (layout, page, providers)
  components/       # React components
    ui/             # Reusable primitives (Card, Button, Badge, Input, Skeleton)
    FlightSearchForm.tsx
    FlightCombinationCard.tsx
    AirportAutocomplete.tsx
    DestinationRow.tsx
    FlightFilterSort.tsx
    OptimizeByToggle.tsx
    ThemeToggle.tsx
  hooks/            # Custom hooks (useDebounce)
  lib/              # Utilities (utils.ts)

backend/src/main/java/com/kristian/flightsearch/
  Server.java           # Entry point
  models/               # Flight, Airport, Route, Airline
  db/                   # DatabaseManager, FlightStore, AirportStore
  flightgraph/          # Graph + Dijkstra (FlightGraph, AirportVertex, Edge, Dijkstra)
  multicitysearch/      # MultiCitySearch
  datagenerator/        # FlightGenerator, distance/duration calculators
  utils/                # AirportPrinter, FlightPrinter
backend/src/main/resources/db/
  001_create_flights.sql
backend/scripts/        # seed_database.sh, drop_tables.sh, count_flights.sh
```

### Commands

backend:

- `mvn compile exec:java -Dexec.mainClass="com.kristian.flightsearch.Server"`

frontend:

- `npm run dev`

### Available tools:

- psql
- railway
- vercel

### Decisions & learnings:

<!-- Append dated bullets when something bites us. Prevents recurring mistakes. -->

---

## Working together

We're coworkers. I am Kristian. Push back when you think you're right, but cite evidence. Ask rather than assume. Say "I don't know" when you don't.

## Code

- Simplicity and readability over cleverness
- Smallest reasonable change to reach the goal; never rewrite from scratch without permission
- Match surrounding style; consistency within a file beats external standards
- Reduce duplication; preserve comments unless actively false
- Comments explain _why_, not _what_; no temporal references ("recently refactored...")
- Evergreen names only — never "new", "improved", "enhanced"
- Every new file gets a `/** */` comment describing its purpose
- No unrelated changes — file issues instead

## Tests

- Write tests before implementation (TDD)
- Never mock what you're testing; never write tests that only test mocks
- Test output must be pristine; assert expected errors, don't ignore them
- Unit tests on all projects; integration/e2e only if a framework already exists

## Debugging

Find the root cause — never patch a symptom. One hypothesis at a time; smallest possible change to test it. If the fix doesn't work, stop and re-analyse before trying anything else.

## Style

- Canadian spelling in docs/commits; American in code
- Never use "robust" or "thorough"

## Git

- Never `--no-verify`; fix hooks or ask for help
- Semantic commits (`fix:`, `feat:`, `chore:`), first line ≤ 80 chars
- Never add AI as coauthor; create a WIP branch if none exists
- Double quotes `"` not single `'`

## Planning

- Never create `todo.md` files — use the TodoWrite tool for progress tracking instead
- Store plan documents in `.claude/claude-plan/` with descriptive names (e.g., `auth-plan.md`, not `plan.md`)
- Plans should include implementation steps, prompts for LLMs, and context for future reference
