<div align="center">
  <img src="docs/images/flagon-banner-nobg.png" alt="Flagon Banner" width="75%" height="auto" />

  <h1>Flagon</h1>

  <p><strong>Run unmodified desktop Java/LWJGL applications directly in the browser.</strong></p>

  <a href="https://teavm.org"><img src="https://img.shields.io/badge/Compiled_with-TeaVM-blue?style=flat-square" alt="TeaVM"></a>
  <a href="https://bun.sh"><img src="https://img.shields.io/badge/Powered_by-Bun-black?style=flat-square" alt="Bun"></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-25+-orange?style=flat-square" alt="Java 25+"></a>
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
- `GL11`, `GL15` and `GL20` reimplemented on WebGL2, including immediate mode, the matrix stacks
  and GLSL 1.10/1.20 translation. `GL12`/`GL13`/`GL14`, `GLU` and fixed-function lighting are not
  there; the [compatibility matrix](docs/COMPATIBILITY.md) lists call by call what is
- Browser-native implementations of input, filesystem, audio, networking, and platform APIs
- Application-agnostic architecture for downstream projects

## How does it compare?

| Project | Input | Native GL calls | License |
|---------|-------|-----------------|---------|
| Flagon | jar | reimplemented on WebGL2 | MIT |
| TeaVM | jar | not addressed; Flagon is built on it | Apache 2.0 |
| CheerpJ | jar | cannot load natives, LWJGL never starts | proprietary |
| Eaglercraft | forked source | reimplemented, Minecraft only | contested |


## Documentation

* **[Architecture Guidelines (for contributing)](docs/ARCHITECTURE.md):** Layering rules and system design.
* **[Compatibility Matrix](docs/COMPATIBILITY.md):** Detailed breakdown of supported and unsupported features.
* **[Running Existing Applications](docs/DOWNSTREAM.md):** Compiling an application jar to a bundle.

## Quickstart

Requires **JDK 25+** and **[bun](https://bun.sh)**. Maven is provided via the wrapper.

```sh
make setup   # installs the shell's pckgs and the browser the tests drive
make build   # compiles java and emits both browser bundles
make dev     # runs the dev server
```
`make` on its own lists every task in the repo.

Once running, open <http://localhost:5273>. Select a bundle, a backend, and a program, and run it.

Bundles compile to WebAssembly GC by default; `make bundles-js` emits the same bundles compiled to
JavaScript, which the shell falls back to on browsers without WebAssembly GC.
`make build-release` emits both as a hosted build should ship them: optimized and minified, with
no source maps or debug information beside them.
The `example` bundle contains [unmodified LWJGL applications](example-app/): a spinning square, and
`blocks`, a whole small game with input, textures, sound, a hud and a high score it saves.
The `sandbox` bundle contains diagnostics and regression programs.


> [!NOTE]
> Appending ?program=selftest to the URL runs the compilation-backend regression suite.

Networking goes through `dev.flagon.platform.net`, not `java.net.Socket`, which the browser
classlib does not have. It needs a proxy served alongside the page, because a browser cannot open a
socket itself.
The development server runs one; a hosted build runs `make serve`, which serves
the site and the socket endpoint together. See [COMPATIBILITY.md](docs/COMPATIBILITY.md#networking).

## Testing

Flagon includes both JVM unit tests and browser-level integration tests.

```sh
make test      # jvm unit tests
make browser   # browser tests against the real bundles, once per backend
make check     # both of the above plus the typecheck, as ci runs them
```

## Reporting a problem

Bugs, unsupported calls and questions belong in the
[issue tracker](https://github.com/KingJayan/flagon/issues). A report is most useful with the
backend it happened on, the program or jar that hit it, and whatever the shell's console printed.

## License

Licensed under the MIT License. See [LICENSE](LICENSE) for details.