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
- Ahead-of-time Java → WebAssembly GC/JavaScript compilation with TeaVM
- `GL11` through `GL15`, `GL20`, `GLContext` and `GLU` are reimplemented on WebGL2. 
See the [compatibility table](docs/COMPATIBILITY.md) for exactly what is emulated and what is refused
- Browser-native implementations of input, filesystem, audio, networking, and platform APIs
- Application-agnostic architecture for downstream projects

## How does it compare?

| Repo | Input | Native GL calls | License |
|---------|-------|-----------------|---------|
| Flagon | jar | reimplemented on WebGL2 | MIT |
| TeaVM | jar | not addressed; Flagon is built on it | Apache 2.0 |
| CheerpJ | jar | cannot load natives, LWJGL never starts | proprietary |
| Eaglercraft | forked source | reimplemented, Minecraft only | contested |


## Documentation

* **[Architecture Guidelines (for contributing)](docs/ARCHITECTURE.md):** Layering rules and system design.
* **[Compatibility Table](docs/COMPATIBILITY.md):** Detailed breakdown of supported and unsupported features.
* **[Running Existing Applications](docs/DOWNSTREAM.md):** Compiling an application jar to a bundle.

## Quickstart

Requires **JDK 25+** and **[bun](https://bun.sh)**. Maven is provided via the wrapper.

```sh
make # list tasks on the repo -- see Makefile
make setup   # installs the shell's pckgs and the browser the tests drive
make build   # compiles java and emits both browser bundles
make dev     # runs the dev server
```

Once running, open <http://localhost:5273>. Select a bundle, a backend, and a program, and run it.

Bundles compile to WebAssembly GC by default; `make bundles-js` emits the same bundles compiled to
JavaScript, which the shell falls back to on browsers without WebAssembly GC.
`make build-release` emits both as a hosted build should ship them: optimized and minified, with
no source maps or debug information beside them.
The `example` bundle contains [unmodified LWJGL applications](examples/app/): a spinning square, and
`blocks`, a whole small game with input, textures, sound, a hud and a high score it saves.
The `sandbox` bundle contains diagnostics and regression programs.

To try your own jar, put it in `vendor/` (any filename) and build:

```sh
make app
```

Nothing else to configure: the jar is asked what it runs, through its manifest or by finding the
classes with a `main`, and each one becomes a program. Go to
<http://localhost:5273/play/> to run it fullscreen, any bundle can be run there: `/play/?bundle=example&program=blocks`.


> [!NOTE]
> Appending ?program=selftest to the URL runs the compilation-backend regression suite.

Networking goes through `dev.flagon.platform.net`, `java.net.Socket` is supplied on top of it
for code that opens its own. Requires a proxy served alongside the page, because a browser cannot
open a socket itself.
The development server runs one; a hosted build runs `make serve`, which serves
the site and the socket endpoint together. See [COMPATIBILITY.md](docs/COMPATIBILITY.md#networking).

## Testing

Flagon includes both JVM unit tests and browser-level integration tests.

```sh
make test      # jvm unit tests
make browser   # browser tests against the bundles (per backend)
make check     # both of the above plus the typecheck (ci)
```

## Reporting a problem

Bugs, unsupported calls and questions belong in the
[issue tracker](https://github.com/KingJayan/flagon/issues). Please include the backend it occurred on, the program or jar, and whatever the shell console output.

## License

Licensed under the MIT. See [LICENSE](LICENSE) for details.