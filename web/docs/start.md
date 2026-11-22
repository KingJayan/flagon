# Getting started

Flagon compiles desktop Java bytecode ahead of time into WebAssembly GC and JavaScript. It runs unmodified LWJGL 2 and LWJGL 3 programs in a browser tab. There is no JVM in the page.

## Requirements

- JDK 25 or later.
- [Bun](https://bun.sh).
- `ffmpeg`, only for a JAR that ships Ogg Vorbis audio.
- A browser with WebAssembly GC: Chrome 119+, Firefox 120+ or Safari 18.2+. Other browsers use the JavaScript bundle.

## Build and run

```bash
make setup   # install web dependencies and Playwright browsers
make bundles # compile the bundles
make dev     # serve the site at http://127.0.0.1:5273
```

The site has three pages:

| Page | Path | Use |
| --- | --- | --- |
| Demos | `/` | Runs the sample bundles. On a local server it also lists every test program. |
| Playground | `/playground/` | Compiles and runs Java source in the browser. |
| Docs | `/docs/` | This documentation. |

## Run your own JAR

1. Put the application JAR and its runtime dependencies in `vendor/`.
2. Run `make app`.
3. Run `make dev` and open `/play/`.

See [Packaging](packaging.md) for the options.

## Page parameters

The Demos page and `/play/` read these query parameters:

| Parameter | Value | Default |
| --- | --- | --- |
| `bundle` | `sandbox`, `example` or `app` | `app` on `/play/`; on Demos, `sandbox` on a local host, else the first demo |
| `backend` | `wasm` or `js` | `wasm` when the browser has WebAssembly GC |
| `program` | a program id from the bundle's catalog | the first program |
| `arg` | one program argument; repeat for more | none |
| `proxytoken` | the token of a [socket proxy](packaging.md#socket-proxy) | none |
| `view` | `all` shows every test program; `demos` shows only the demos (Demos page only) | `all` on a local or private host |

The `wasm` backend loads the JSPI build when the browser has `WebAssembly.Suspending`, and the coroutine build otherwise.

## Make targets

| Target | Does |
| --- | --- |
| `make setup` | Installs web dependencies and Playwright browsers. |
| `make bundles` | Compiles every bundle. |
| `make fast` | Compiles only the WebAssembly bundles, for quicker development builds. |
| `make bundles-release` | Compiles release bundles and checks their size budgets. |
| `make app` | Compiles the JARs in `vendor/` into the `app` bundle. |
| `make minecraft` | Fills `vendor/` from a local launcher install. Set `MINECRAFT` and `LAUNCHER` to change the version or install. |
| `make playground` | Compiles javac and TeaVM to WebAssembly for the playground. |
| `make dev` | Starts the development server on port 5273. |
| `make serve` | Serves the built site with the socket proxy. |
| `make build-site` | Builds the site for deployment. |
| `make test` | Runs the JVM unit tests. |
| `make browser` | Runs the Playwright tests in Chromium and Firefox. |
| `make lint`, `make typecheck` | Runs the Java and web linters and the type checker. |
| `make check` | Runs the CI checks. |
| `make clean` | Removes build and test output. |

Run `make help` for the full list.

## Playground

The playground runs javac and TeaVM 0.17 in a Web Worker. No server compiles code. Limits:

- 64 KB of source.
- 10 seconds per run.
- 2,000 lines of output.

The program runs in a second worker, and **Stop** ends it. The playground uses the stock TeaVM class library, not Flagon's, so LWJGL is not available there.

## Next

- [Packaging](packaging.md): build a bundle and ship it.
- [Compatibility](compatibility.md): what works in a browser.
- [Architecture](architecture.md): how the build and runtime fit together.
- [API reference](api.md): the public classes.
