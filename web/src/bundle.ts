/** contract between the page and a teavm-compiled flagon bundle. */

export interface HostBridge {
  log(level: string, text: string): void;
  status(phase: string, detail: string): void;
  ready(): void;
  exited(code: number): void;
  fatal(phase: string, message: string, stack: string): void;
}

declare global {
  interface Window {
    __flagonHost?: HostBridge;
  }
}

/** which backend a bundle was compiled by, and so how it is started. */
export type Backend = 'js' | 'wasm';

export const BACKENDS: readonly Backend[] = ['js', 'wasm'];

export function isBackend(name: string): name is Backend {
  return (BACKENDS as readonly string[]).includes(name);
}

/**
 * a module whose only content is a gc struct type: engines without WebAssembly GC reject it,
 * and validating costs nothing next to instantiating a bundle.
 */
const GC_PROBE = new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 80, 0, 95, 0]);

export function supportsWasmGc(): boolean {
  return typeof WebAssembly !== 'undefined' && WebAssembly.validate(GC_PROBE);
}

/** wasm gc where the engine has it, javascript everywhere else. */
export function defaultBackend(): Backend {
  return supportsWasmGc() ? 'wasm' : 'js';
}

interface JsBundle {
  main(args: string[]): void;
}

interface WasmBundle {
  exports: { main(args: string[]): void };
}

interface WasmLoader {
  load(path: string): Promise<WasmBundle>;
}

// teavm's own loader, unpacked beside the bundles by the wasm build
const WASM_LOADER = '/flagon/wasm-gc-module-runtime.js';

export async function runBundle(url: string, host: HostBridge, args: string[]): Promise<void> {
  window.__flagonHost = host;
  const absolute = new URL(url, location.origin).href;
  if (absolute.endsWith('.wasm')) {
    await runWasmBundle(absolute, args);
    return;
  }
  const module = (await import(/* @vite-ignore */ absolute)) as JsBundle;
  if (typeof module.main !== 'function') {
    throw new Error(`bundle at ${url} does not export main()`);
  }
  module.main(args);
}

/**
 * a wasm bundle is a module to instantiate rather than a script to import, so it is loaded and
 * then entered. both backends end up in the same main() with the same arguments.
 */
async function runWasmBundle(absolute: string, args: string[]): Promise<void> {
  const url = new URL(WASM_LOADER, location.origin).href;
  const loader = (await import(/* @vite-ignore */ url)) as WasmLoader;
  if (typeof loader.load !== 'function') {
    throw new Error(`the wasm loader at ${WASM_LOADER} does not export load()`);
  }
  const bundle = await loader.load(absolute);
  if (typeof bundle.exports?.main !== 'function') {
    throw new Error(`bundle at ${absolute} does not export main()`);
  }
  bundle.exports.main(args);
}
