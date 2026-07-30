# Flagon

Run unmodified desktop Java/LWJGL applications in a modern browser. The long-term target is
Minecraft Java Edition 1.8.9, running from an untouched `minecraft.jar`.

## Status

| # | Milestone | State |
|---|-----------|-------|
| 1 | Project builds | done |
| 2 | Browser launches runtime | done |
| 3 | Simple Java program executes | done |
| 4 | LWJGL demo renders | next |
| 5 | Minecraft classes load | |
| 6 | Minecraft splash screen | |
| 7 | Main menu | |
| 8 | World generation | |
| 9 | Player movement | |
| 10 | Stable gameplay | |

## Structure

Java is compiled AoT to JS by [TeaVM](https://teavm.org). See
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the layering rules and
[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for what does and does not work.

## Building

Requires JDK 17+ and [bun](https://bun.sh). Maven comes from the wrapper.

```sh
./mvnw package                  # compiles java and emits the browser bundle
cd web && bun install && bun run dev
```

Then open http://localhost:5273. Pick a program and run it; `?program=selftest` runs the
compilation-backend regression suite.

## Testing

```sh
./mvnw test                     # jvm unit tests
cd web && bunx playwright test  # browser tests against the real bundle
```

## Minecraft

The game jar is not committed nor is distributed here.
