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
    __flagonApart?: {
      want(hashes: string): void;
      take(hash: string): ArrayBuffer | null;
    };
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

const PACK_CACHE = 'flagon-pack';

function isServed(response: Response): boolean {
  return response.ok && !(response.headers.get('content-type') ?? '').includes('text/html');
}

async function openPackCache(): Promise<Cache | undefined> {
  try {
    return typeof caches === 'undefined' ? undefined : await caches.open(PACK_CACHE);
  } catch {
    // cache storage needs a secure context, and a private window may refuse it
    return undefined;
  }
}

/** the cached pack while the server says it still holds, else the server's */
async function revalidated(url: string, cache: Cache | undefined): Promise<Response> {
  const kept = await cache?.match(url);
  const headers = new Headers();
  const tag = kept?.headers.get('etag');
  const modified = kept?.headers.get('last-modified');
  if (tag) {
    headers.set('if-none-match', tag);
  } else if (modified) {
    headers.set('if-modified-since', modified);
  }
  const response = await fetch(url, { headers, cache: 'no-store' });
  if (response.status === 304 && kept) {
    return kept;
  }
  const validated = response.headers.has('etag') || response.headers.has('last-modified');
  if (cache && validated && isServed(response)) {
    cache.put(url, response.clone()).catch(() => {});
  }
  return response;
}

const APART_FETCHES = 4;

/**
 * fetches the resources kept beside the pack in the background, from the cache where it can, and
 * hands each to the program once it is in; a read cannot wait, since it may hold a lock
 */
function serveApart(packUrl: string, cache: Cache | undefined): void {
  const base = new URL(packUrl.replace(/\.pack$/, '.files/'), location.origin).href;
  const arrived = new Map<string, ArrayBuffer>();
  const queue: string[] = [];

  async function load(hash: string): Promise<void> {
    const url = base + hash;
    try {
      const kept = await cache?.match(url);
      if (kept) {
        arrived.set(hash, await kept.arrayBuffer());
        return;
      }
      const response = await fetch(url);
      if (!isServed(response)) {
        console.warn(`flagon: ${url} answered ${response.status}`);
        return;
      }
      cache?.put(url, response.clone()).catch(() => {});
      arrived.set(hash, await response.arrayBuffer());
    } catch (failure) {
      console.warn(`flagon: could not fetch ${url}: ${String(failure)}`);
    }
  }

  // each worker takes the next file once its last is in, so a file asked for early goes next
  async function work(): Promise<void> {
    const hash = queue.shift();
    if (hash !== undefined) {
      await load(hash);
      await work();
    }
  }

  window.__flagonApart = {
    want(hashes: string): void {
      const wanted = hashes.split(',').filter((hash) => hash.length > 0);
      queue.push(...wanted);
      for (let worker = 0; worker < APART_FETCHES; worker++) {
        void work();
      }
      const urls = new Set(wanted.map((hash) => base + hash));
      cache
        ?.keys()
        .then((keys) => {
          const stale = keys.filter((key) => key.url.startsWith(base) && !urls.has(key.url));
          return Promise.all(stale.map((key) => cache.delete(key)));
        })
        .catch(() => {});
    },
    take(hash: string): ArrayBuffer | null {
      const file = arrived.get(hash);
      if (file) {
        return file;
      }
      // asked for before its turn, so it goes next
      const waiting = queue.indexOf(hash);
      if (waiting > 0) {
        queue.splice(waiting, 1);
        queue.unshift(hash);
      }
      return null;
    },
  };
}

async function fetchPack(bundleUrl: string): Promise<void> {
  delete window.__flagonResources;
  const url = packUrlOf(bundleUrl);
  try {
    const cache = await openPackCache();
    serveApart(url, cache);
    const response = await revalidated(url, cache);
    if (!isServed(response) || !response.body) {
      return;
    }
    // the pack is gzipped whole, and unzips here as it downloads
    const unzipped = response.body.pipeThrough(new DecompressionStream('gzip'));
    const pack = await new Response(unzipped).arrayBuffer();
    if (pack.byteLength > 0) {
      window.__flagonResources = pack;
    }
  } catch {}
}

/** the bundle is not being served */
export class MissingBundle extends Error {}

export async function runBundle(url: string, host: HostBridge, args: string[]): Promise<void> {
  window.__flagonHost = host;
  installTunnel();
  const absolute = new URL(url, location.origin).href;
  // the pack and the bundle come down together; the program starts once both are in
  const pack = fetchPack(absolute);
  try {
    if (absolute.endsWith('.wasm')) {
      await runWasmBundle(absolute, args, pack);
      return;
    }
    const [module] = await Promise.all([
      import(/* @vite-ignore */ absolute) as Promise<JsBundle>,
      pack,
    ]);
    if (typeof module.main !== 'function') {
      throw new Error(`bundle at ${url} does not export main()`);
    }
    module.main(args);
  } catch (failure) {
    throw (await whyItIsNotThere(absolute)) ?? failure;
  }
}

async function whyItIsNotThere(absolute: string): Promise<MissingBundle | undefined> {
  try {
    const response = await fetch(absolute, { method: 'HEAD' });
    const type = response.headers.get('content-type') ?? '';
    if (response.ok && !type.includes('text/html')) {
      return undefined;
    }
  } catch {
    return undefined;
  }
  return new MissingBundle(
    absolute.includes('/app.')
      ? "there is no application bundle yet: put a jar in vendor/ and run 'make app'"
      : `there is no bundle at ${absolute}: run 'make bundles'`,
  );
}

async function runWasmBundle(
  absolute: string,
  args: string[],
  pack: Promise<void>,
): Promise<void> {
  const url = new URL(WASM_LOADER, location.origin).href;
  const loader = (await import(/* @vite-ignore */ url)) as WasmLoader;
  if (typeof loader.load !== 'function') {
    throw new Error(`the wasm loader at ${WASM_LOADER} does not export load()`);
  }
  const [bundle] = await Promise.all([loader.load(absolute), pack]);
  if (typeof bundle.exports?.main !== 'function') {
    throw new Error(`bundle at ${absolute} does not export main()`);
  }
  bundle.exports.main(args);
}
