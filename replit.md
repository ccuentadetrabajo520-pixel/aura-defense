# AuraDefensa

Suite de ciberdefensa nativa para Android con interfaz Sci-Fi HUD estilo Cyberpunk. Analiza permisos de aplicaciones, detecta amenazas de red, audita la integridad del sistema y permite purgar aplicaciones maliciosas.

## Run & Operate

- `pnpm --filter @workspace/mobile run dev` — Expo dev server (port dynamic)
- `pnpm --filter @workspace/api-server run dev` — API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- Mobile: Expo SDK + React Native + Expo Router (file-based routing)
- Animations: react-native-reanimated
- State: React Context (SecurityContext) + AsyncStorage
- Icons: @expo/vector-icons (MaterialCommunityIcons)
- API: Express 5 (shared api-server artifact)
- DB: PostgreSQL + Drizzle ORM (available, not yet used by mobile)

## Where things live

- `artifacts/mobile/` — Expo mobile app
  - `app/(tabs)/` — 4 screens: index (Shield), scanner, telemetry, purge
  - `components/` — RadarHUD, LogTerminal, TelemetryGraph, ThreatCard
  - `contexts/SecurityContext.tsx` — all security state + scan engine
  - `constants/colors.ts` — cyberpunk dark theme tokens
- `artifacts/api-server/` — Express API server
- `lib/api-spec/openapi.yaml` — OpenAPI contract

## Architecture decisions

- AuraDefensa is frontend-only (AsyncStorage + simulated scan engine). No backend needed for security scanning logic.
- `SecurityContext` holds all state: firewall toggle, scan phases, threats array, log entries.
- The scan engine uses async/await with timed delays to simulate realistic sequential audit phases.
- Each `AnimatedBar` in TelemetryGraph is its own component to avoid calling `useAnimatedStyle` inside loops (React hooks rules).
- App is forced to dark mode via `"userInterfaceStyle": "dark"` in app.json.

## Product

- **Shield (Tab 1):** Holographic radar HUD that rotates faster during scanning. VPN Firewall toggle. 4-cell status grid.
- **Scanner (Tab 2):** Threat Hunt button triggers a 6-phase scan. Cascading monospace log terminal. Network IDS + system integrity panels.
- **Telemetry (Tab 3):** 8 animated CPU thread bars. Metric cards. Analysis engine load breakdown. Threat severity matrix.
- **Purge Console (Tab 4):** Glassmorphism threat cards with neon severity borders. Per-threat and global purge buttons. Invokes Android uninstaller via `Linking.openURL('package:...')`.

## User preferences

_Cyberpunk / Sci-Fi HUD aesthetic. Dark background (#080B10), neon cyan-green for safe, crimson for threats, electric purple for UI. Monospace fonts in terminal. No emojis._

## Gotchas

- Restart workflow only when changing dependencies (Metro HMR handles code changes).
- `useAnimatedStyle` must never be called inside `.map()` — always extract to a separate component.
- The `userInterfaceStyle: "dark"` in app.json forces `useColorScheme()` to return `"dark"` so `useColors()` always returns the dark palette.

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
- See the `expo` skill for Expo-specific patterns and pitfalls
