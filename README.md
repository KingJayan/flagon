# Flagon

Run unmodified desktop Java/LWJGL applications in a modern browser. The long-term target is
Minecraft Java Edition 1.8.9, running from an untouched `minecraft.jar`.

## Roadmap

| Stage | State |
|-------|-------|
| Build pipeline | done |
| Runtime boots in the browser | done |
| Java programs execute | done |
| LWJGL demo renders | in progress |
| Game classes load | |
| Splash screen | |
| Main menu | |
| World generation | |
| Player movement | |
| Stable gameplay | |

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
