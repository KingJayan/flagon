import { installTunnel } from './tunnel';

export interface HostBridge {
  log(level: string, text: string): void;
  catalog(ids: string[], descriptions: string[]): void;
  status(phase: string, detail: string): void;
  ready(): void;
  exited(code: number): void;
  fatal(phase: string, message: string, stack: string): void;
}

declare global {
  interface Window {
    __flagonHost?: HostBridge;
    __flagonCloseRequested?: boolean;
    __flagonFrames?: number;
  }
}

/** only bundles this site ships may be imported; the name comes from the url. */
export const BUNDLES = ['sandbox', 'example', 'app'];

export type Backend = 'js' | 'wasm';

export const BACKENDS: readonly Backend[] = ['js', 'wasm'];

export function isBackend(name: string): name is Backend {
  return (BACKENDS as readonly string[]).includes(name);
}

const GC_PROBE = new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 80, 0, 95, 0]);

export function supportsWasmGc(): boolean {
  return typeof WebAssembly !== 'undefined' && WebAssembly.validate(GC_PROBE);
}

/** wasm gc where the engine has it, javascript everywhere else. */
export function defaultBackend(): Backend {
  return supportsWasmGc() ? 'wasm' : 'js';
}

/** what the url asked for. both pages read the same parameters; only the default bundle differs. */
export interface Request {
  bundle: string;
  backend: string;
  asked: boolean;
  program: string;
  args: string[];
}

export function readRequest(fallback: string): Request {
  const params = new URLSearchParams(location.search);
  const asked = params.get('backend');
  const token = params.get('proxytoken');
  if (token) {
    (window as unknown as { __flagonProxyToken: string }).__flagonProxyToken = token;
  }
  return {
    bundle: params.get('bundle') ?? fallback,
    backend: asked ?? defaultBackend(),
    asked: asked !== null,
    program: params.get('program') ?? '',
    args: params.getAll('arg'),
  };
}

/** the same bundle is emitted once per backend, under the extension that backend produces. */
export function bundleUrlOf(bundle: string, backend: string): string | undefined {
  if (!BUNDLES.includes(bundle) || !isBackend(backend)) {
    return undefined;
  }
  return `/flagon/${bundle}.${backend === 'wasm' ? 'wasm' : 'js'}`;
}

export function whyNot(bundle: string, backend: string): string {
  return BUNDLES.includes(bundle)
    ? `no backend named '${backend}'; try ${BACKENDS.join(', ')}`
    : `no bundle named '${bundle}'; try ${BUNDLES.join(', ')}`;
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
  installTunnel();
  const absolute = new URL(url, location.origin).href;
  try {
    if (absolute.endsWith('.wasm')) {
      await runWasmBundle(absolute, args);
      return;
    }
    const module = (await import(/* @vite-ignore */ absolute)) as JsBundle;
    if (typeof module.main !== 'function') {
      throw new Error(`bundle at ${url} does not export main()`);
    }
    module.main(args);
  } catch (failure) {
    throw (await whyItIsNotThere(absolute)) ?? failure;
  }
}

/**
 * a bundle nobody built fails as an opaque import error, so say what is actually missing. a dev
 * server answers an unknown path with the page itself, so the type matters as much as the status.
 */
async function whyItIsNotThere(absolute: string): Promise<Error | undefined> {
  try {
    const response = await fetch(absolute, { method: 'HEAD' });
    const type = response.headers.get('content-type') ?? '';
    if (response.ok && !type.includes('text/html')) {
      return undefined;
    }
  } catch {
    return undefined;
  }
  return new Error(
    absolute.includes('/flagon/app.')
      ? "there is no application bundle yet: put a jar in vendor/ and run 'make app'"
      : `there is no bundle at ${absolute}: run 'make build'`,
  );
}

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
