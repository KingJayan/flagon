<div align="center">
  <img src="docs/images/flagon-banner.png" alt="Flagon Banner" width="100%" />

  <h1>Flagon</h1>

  <p><strong>Run unmodified desktop Java/LWJGL applications in a modern browser.</strong></p>

  <a href="https://teavm.org"><img src="https://img.shields.io/badge/Compiled_with-TeaVM-blue?style=flat-square" alt="TeaVM"></a>
  <a href="https://bun.sh"><img src="https://img.shields.io/badge/Powered_by-Bun-black?style=flat-square" alt="Bun"></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java 17+"></a>
</div>

<br />

The long-term target of this project is **Minecraft Java Edition 1.8.9**, running directly from an untouched `minecraft.jar`.

## Docs

Java is compiled ahead-of-time (AoT) to JavaScript utilizing [TeaVM](https://teavm.org). 

* **[Architecture Guidelines](docs/ARCHITECTURE.md):** Layering rules and system design.
* **[Compatibility Matrix](docs/COMPATIBILITY.md):** Detailed breakdown of supported and unsupported features.

## Goals

| Stage | Status |
| :--- | :--- |
| Build pipeline | `Done` |
| Runtime boots in browser | `Done` |
| Java programs execute | `Done` |
| LWJGL demo renders | `Done` |
| Game classes load | `In Progress` |
| Splash screen | `Pending` |
| Main menu | `Pending` |
| World generation | `Pending` |
| Player movement | `Pending` |
| Stable gameplay | `Pending` |

## Quickstart

Requires **JDK 17+** and **[bun](https://bun.sh)**. Maven is provided via the wrapper.

```sh
# compiles java and emits the browser bundle
./mvnw package

# initialize and run the development server
cd web && bun install && bun run dev
```
Once running, open `http://localhost:5273`. Select a program and run it.
> [!NOTE]
> Appending ?program=selftest to the URL runs the compilation-backend regression suite.

## Testing

```sh
# jvm unit tests
./mvnw test                     

# browser tests against the real bundle
cd web && bunx playwright test
```

*Notice*: The Minecraft game .jar is not committed, nor is it distributed within this repository.