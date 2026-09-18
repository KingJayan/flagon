<div align="center">
  <img src="docs/images/flagon-banner-nobg.png" alt="Flagon" width="350" />
<p>
    <a href="#quickstart">Quickstart</a> &nbsp;•&nbsp;
    <a href="docs/COMPATIBILITY.md">Compatibility</a> &nbsp;•&nbsp;
    <a href="docs/DOWNSTREAM.md">Packaging</a> &nbsp;•&nbsp;
    <a href="docs/ARCHITECTURE.md">Architecture</a>
  </p>

[![TeaVM](https://img.shields.io/badge/TeaVM-434c5e?style=flat-square&logo=webassembly&logoColor=eceff4)](https://teavm.org)
[![Bun](https://img.shields.io/badge/Bun-2e3440?style=flat-square&logo=bun&logoColor=eceff4)](https://bun.sh)
[![Java 25+](https://img.shields.io/badge/Java-25+-d08770?style=flat-square&logo=openjdk&logoColor=2e3440)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-4c566a?style=flat-square)](LICENSE)
</div>

##
Flagon compiles desktop Java bytecode into browser-native WebAssembly GC and JavaScript using TeaVM, reimplementing LWJGL 2 & 3, OpenGL, OpenAL, OPFS filesystem persistence, and socket networking over browser APIs.

## Quickstart

Requires **JDK 25+** and **[Bun](https://bun.sh)**.

```bash
make setup   # Install dependencies
make build   # Compile Java and emit WASM GC / JS bundles
make dev     # Start development server at http://localhost:5273
```

To test a standalone JAR without setup, drop it into `vendor/` and run `make app` (see [Running an Existing JAR](DOWNSTREAM.md#quickstart-no-setup)).

* **Dev Shell (`/`):** Console, runtime diagnostics, and program picker.
* **Fullscreen View (`/play/?bundle=app`):** Edge-to-edge canvas execution.
* **Networking (`make serve`):** Hosts the static build alongside the required WebSocket-to-TCP bridge (see [Networking Details](COMPATIBILITY.md)).

## Testing

```bash
make test      # JVM unit tests
make browser   # Playwright integration tests (Chromium & Firefox across both backends)
make check     # Full CI verification suite
```

## Comparison

| Project | Target Input | Graphics Pipeline | Sockets & Filesystem | License |
| --- | --- | --- | --- | --- |
| **Flagon** | Unmodified JAR | Reimplemented on WebGL2 (LWJGL 2 & 3) | OPFS persistence + Proxied TCP | MIT |
| **TeaVM** | Source / Bytecode | None (core classlib only) | In-memory only | Apache 2.0 |
| **CheerpJ** | Unmodified JAR | JVM virtualization (no desktop LWJGL) | Emulated virtual FS | Proprietary |
| **Eaglercraft** | Forked 1.8 Source | Custom WebGL port (Minecraft only) | IndexedDB / Local | Contested |

## License

Licensed under [MIT](https://github.com/KingJayan/flagon/blob/main/LICENSE).
