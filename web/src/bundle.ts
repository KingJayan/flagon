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

interface BundleModule {
  main(args: string[]): void;
}

export async function runBundle(url: string, host: HostBridge, args: string[]): Promise<void> {
  window.__flagonHost = host;
  const absolute = new URL(url, location.origin).href;
  const module = (await import(/* @vite-ignore */ absolute)) as BundleModule;
  if (typeof module.main !== 'function') {
    throw new Error(`bundle at ${url} does not export main()`);
  }
  module.main(args);
}