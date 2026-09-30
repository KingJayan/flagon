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
    __flagonResources?: ArrayBuffer;
    __flagonCloseRequested?: boolean;
    __flagonFrames?: number;
  }

  // lib.dom has no jspi yet
  namespace WebAssembly {
    const Suspending: unknown;
    const promising: unknown;
  }
}

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

/** jspi parks the wasm stack, so its build skips the coroutine rewrite */
export function supportsJspi(): boolean {
  return (
    typeof WebAssembly !== 'undefined' &&
    typeof WebAssembly.Suspending === 'function' &&
    typeof WebAssembly.promising === 'function'
  );
}

export function defaultBackend(): Backend {
  return supportsWasmGc() ? 'wasm' : 'js';
}

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

export function bundleUrlOf(bundle: string, backend: string): string | undefined {
  if (!BUNDLES.includes(bundle) || !isBackend(backend)) {
    return undefined;
  }
  if (backend === 'js') {
    return `/flagon/js/${bundle}.js`;
  }
  return supportsJspi() ? `/flagon/jspi/${bundle}.wasm` : `/flagon/wasm/${bundle}.wasm`;
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

const WASM_LOADER = '/flagon/wasm/wasm-gc-module-runtime.js';

export function packUrlOf(bundleUrl: string): string {
  return bundleUrl.replace(/\/(?:js|wasm|jspi)\/([^/]+)\.(?:js|wasm)$/, '/$1.pack');
}

async function fetchPack(bundleUrl: string): Promise<void> {
  delete window.__flagonResources;
  try {
    const response = await fetch(packUrlOf(bundleUrl));
    const type = response.headers.get('content-type') ?? '';
    if (!response.ok || type.includes('text/html')) {
      return;
    }
    const pack = await response.arrayBuffer();
    if (pack.byteLength > 0) {
      window.__flagonResources = pack;
    }
  } catch {}
}

export async function runBundle(url: string, host: HostBridge, args: string[]): Promise<void> {
  window.__flagonHost = host;
  installTunnel();
  const absolute = new URL(url, location.origin).href;
  await fetchPack(absolute);
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
    absolute.includes('/app.')
      ? "there is no application bundle yet: put a jar in vendor/ and run 'make app'"
      : `there is no bundle at ${absolute}: run 'make bundles'`,
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
