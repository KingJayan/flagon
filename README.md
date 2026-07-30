<div align="center">
  <img src="docs/images/flagon-banner.png" alt="Flagon Banner" width="100%" />

  <h1>Flagon</h1>

  <p><strong>Run unmodified desktop Java/LWJGL applications in a modern browser.</strong></p>

  <a href="https://teavm.org"><img src="https://img.shields.io/badge/Compiled_with-TeaVM-blue?style=flat-square" alt="TeaVM"></a>
  <a href="https://bun.sh"><img src="https://img.shields.io/badge/Powered_by-Bun-black?style=flat-square" alt="Bun"></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java 17+"></a>
</div>

<br />

Flagon is a compatibility layer, not a rewrite: it reimplements the interfaces a desktop
application already compiles against — `Display`, `GL11`, `Keyboard`, `Mouse`, `BufferUtils` —
on top of WebGL2, DOM events and the browser's own APIs. Application code is not modified.

Flagon itself is application-agnostic. Work on any particular application lives downstream;
[flagon-mc](https://github.com/KingJayan/flagon-mc) is one such target.

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
| Keyboard and mouse input | `Done` |
| NIO buffers (`BufferUtils`, `MemoryUtil`) | `Done` |
| Loading an application jar | `In Progress` |
| Vertex arrays and VBOs (`GL15`, `GL20`) | `Pending` |
| Virtual filesystem on OPFS | `Pending` |
| Audio (`OpenAL` → WebAudio) | `Pending` |
| Networking through a proxy | `Pending` |

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

*Notice*: No application jar is committed to or distributed from this repository.