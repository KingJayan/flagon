<div align="center">
  <img src="docs/images/flagon-banner.png" alt="Flagon Banner" width="100%" />

  <h1>Flagon</h1>

  <p><strong>Run unmodified desktop Java/LWJGL applications directly in the browser.</strong></p>

  <a href="https://teavm.org"><img src="https://img.shields.io/badge/Compiled_with-TeaVM-blue?style=flat-square" alt="TeaVM"></a>
  <a href="https://bun.sh"><img src="https://img.shields.io/badge/Powered_by-Bun-black?style=flat-square" alt="Bun"></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java 17+"></a>
</div>

<br />


## What is Flagon?

Flagon lets existing desktop Java/LWJGL applications run directly in the browser,
without modifying application code.

It achieves this by reimplementing the desktop APIs that applications already compile
against (`Display`, `GL11`, `Keyboard`, `Mouse`, `BufferUtils`, etc.) on top of
WebGL2, DOM events, and browser APIs.

Flagon itself is application-agnostic. Work on any particular application lives downstream;
[flagon-mc](https://github.com/KingJayan/flagon-mc) is one such target.

## Features

- Run existing desktop Java/LWJGL applications directly in the browser
- No application source modifications required
- Ahead-of-time Java → WebAssembly GC compilation with TeaVM, with a JavaScript fallback
- WebGL2 implementation of core LWJGL APIs
- Browser-native implementations of input, filesystem, audio, networking, and platform APIs
- Application-agnostic architecture for downstream projects

## Documentation

* **[Architecture Guidelines (for contributing)](docs/ARCHITECTURE.md):** Layering rules and system design.
* **[Compatibility Matrix](docs/COMPATIBILITY.md):** Detailed breakdown of supported and unsupported features.
* **[Running Existing Applications](docs/DOWNSTREAM.md):** Compiling an application jar to a bundle.

## Roadmap for WASM-GC Support

Flagon runs on WebAssembly GC, which keeps Java objects in the WebAssembly heap and hands garbage
collection to the engine. JavaScript remains a fully supported backend and is what browsers without
WASM GC are served. These were the stages to getting every bundle onto it.

| Stage | Status |
| :--- | :--- |
| Toolchain able to emit WASM GC | `✔` |
| Bundles compile and boot in the browser | `✔` |
| Graphics and input | `✔` |
| Files, audio, and networking | `✔` |
| Browser suite green on both backends | `✔` |
| WASM GC as the default backend | `✔` |

## Quickstart

Requires **JDK 17+** and **[bun](https://bun.sh)**. Maven is provided via the wrapper.

```sh
# compiles java and emits the browser bundles
./mvnw package
./mvnw -Pjs package

# initialize and run the development server
cd web && bun install && bun run dev
```
Once running, open <http://localhost:5273>. Select a bundle, a backend, and a program, and run it.
Bundles compile to WebAssembly GC by default; `-Pjs` emits the same bundles compiled to JavaScript,
which the shell falls back to on browsers without WebAssembly GC.
The `example` bundle contains [an unmodified LWJGL application](example-app/).
The `sandbox` bundle contains diagnostics and regression programs.


> [!NOTE]
> Appending ?program=selftest to the URL runs the compilation-backend regression suite.

## Testing

Flagon includes both JVM unit tests and browser-level integration tests.

```sh
# jvm unit tests
./mvnw test                     

# browser tests against the real bundles, once per backend
cd web && bunx playwright test
```

## License

Licensed under the MIT License. See [LICENSE](LICENSE) for details.