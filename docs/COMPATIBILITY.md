# Compatibility matrix

What the browser environment does and does not provide. Updated as milestones land.

## Platform services

| Area | State | Notes |
|------|-------|-------|
| Wall clock | works | `Date.now()` |
| Monotonic clock | works, degraded resolution | `performance.now()`, clamped non-decreasing |
| Console / logging | works | mapped onto `console.*` levels |
| Lifecycle reporting | works | `window.__flagonHost` |
| Filesystem | not started | OPFS-backed `.minecraft`, milestone 5 |
| Threading | not started | TeaVM green threads on the JS backend |
| Networking | not started | needs a proxy; browsers have no raw TCP |
| Input | not started | milestone 4 |
| Audio | not started | OpenAL → WebAudio |
| Graphics | not started | GL → WebGL2, milestone 4 |

## Java language and library

Verified in Chromium by the `selftest` program (66 checks), which runs in CI as a Playwright test.

| Area | State |
|------|-------|
| Integer and long arithmetic, overflow, shifts | works |
| IEEE-754 doubles and floats, NaN, signed zero | works |
| Strings, `String.format`, JDK-compatible `hashCode` | works |
| Arrays, multidimensional arrays, bounds checks | works |
| Collections, streams, `TreeMap` ordering | works |
| Exceptions, cause chains, `finally`, `ClassCastException` | works |
| `NullPointerException` on null dereference | works **only with `strict` mode** |
| Interfaces, default methods, records, virtual dispatch | works |
| Lambdas, method references, captured locals | works |
| Reflection | untested — likely a problem for the game |

## Known gaps and shims

### `Integer::sum` is missing from TeaVM's classlib
Linking fails with `Method java.lang.Integer.sum(II)I was not found`. Sibling methods
(`Integer.parseInt`, `Long.sum`) are unaffected. Worked around at the call site with a lambda.
If the game jar hits this, the fix is a classlib supplement rather than more call-site edits.

### Null dereference does not throw `NullPointerException` by default
Without `strict`, TeaVM's JS backend emits no null checks, so a null dereference becomes a
JavaScript `TypeError` that `catch (NullPointerException)` cannot catch. Enabled `strict` in the
root POM; costs about 9% bundle size. Non-negotiable — desktop Java catches NPEs.

### TeaVM's JS entry point marshals `main`'s arguments as `Object[]`
`$rt_mainStarter` builds the argument array with `$rt_createArray($rt_objcls(), n)`, so `main`
receives an `Object[]` where its signature says `String[]`. Invisible until `strict` mode checks
a cast, then it surfaces as a bare `ClassCastException` far from the cause.
`BrowserPlatform.copyOf` re-types the array at the entry point.

### Vite will not serve `public/` files through the module pipeline
The compiled bundle is a build artifact, correctly placed in `public/`, but a bare
`import('/flagon/sandbox.js')` makes Vite try to transform it and fail. `runBundle` resolves the
path to an absolute URL so the bundler treats it as external and the browser fetches it as-is.
