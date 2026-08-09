import {
  BACKENDS,
  defaultBackend,
  isBackend,
  runBundle,
  supportsWasmGc,
  type HostBridge,
} from './bundle';

/** only bundles this shell ships may be imported; the name comes from the url. */
const BUNDLES = ['sandbox', 'example'];

/** the same bundle is emitted once per backend, under the extension that backend produces. */
function bundleUrlOf(bundle: string, backend: string): string | undefined {
  if (!BUNDLES.includes(bundle) || !isBackend(backend)) {
    return undefined;
  }
  return `/flagon/${bundle}.${backend === 'wasm' ? 'wasm' : 'js'}`;
}

function whyNot(bundle: string, backend: string): string {
  return BUNDLES.includes(bundle)
    ? `no backend named '${backend}'; try ${BACKENDS.join(', ')}`
    : `no bundle named '${bundle}'; try ${BUNDLES.join(', ')}`;
}

const app = document.querySelector<HTMLElement>('#app')!;
const stateBadge = document.querySelector<HTMLElement>('#state')!;
const phaseList = document.querySelector<HTMLOListElement>('#phases')!;
const logView = document.querySelector<HTMLPreElement>('#log')!;
const bundleSelect = document.querySelector<HTMLSelectElement>('#bundle')!;
const backendSelect = document.querySelector<HTMLSelectElement>('#backend')!;
const programSelect = document.querySelector<HTMLSelectElement>('#program')!;
const runButton = document.querySelector<HTMLButtonElement>('#run')!;
const stopButton = document.querySelector<HTMLButtonElement>('#stop')!;
const rateBadge = document.querySelector<HTMLElement>('#rate')!;
const progress = document.querySelector<HTMLElement>('#progress')!;

type State = 'loading' | 'booting' | 'running' | 'exited' | 'failed';

function setState(state: State): void {
  app.dataset.state = state;
  stateBadge.textContent = state;
  stateBadge.setAttribute('aria-label', `run state: ${state}`);
  stopButton.disabled = state !== 'running';
  progress.hidden = state !== 'loading' && state !== 'booting';
  if (state === 'running') {
    watchFrames();
  } else {
    stopWatchingFrames();
  }
}

let ticker: number | undefined;

/**
 * 'running' alone cannot tell a program that is drawing from one that is wedged, so while a
 * program runs the shell reports the rate the runtime is actually presenting frames at.
 */
function watchFrames(): void {
  if (ticker !== undefined) {
    return;
  }
  let last = window.__flagonFrames ?? 0;
  rateBadge.hidden = false;
  rateBadge.textContent = 'no frames yet';
  ticker = window.setInterval(() => {
    const total = window.__flagonFrames ?? 0;
    const drawn = total - last;
    last = total;
    rateBadge.textContent = drawn > 0 ? `${drawn} fps` : total > 0 ? 'not drawing' : 'no frames yet';
  }, 1000);
}

function stopWatchingFrames(): void {
  if (ticker !== undefined) {
    clearInterval(ticker);
    ticker = undefined;
  }
  rateBadge.hidden = true;
}

function announceDownload(url: string): void {
  const name = url.slice(url.lastIndexOf('/') + 1);
  progress.textContent = `fetching ${name}`;
  fetch(url, { method: 'HEAD' })
    .then((response) => {
      const bytes = Number(response.headers.get('content-length'));
      if (bytes > 0) {
        progress.textContent = `fetching ${name} — ${(bytes / 1e6).toFixed(1)} MB`;
      }
    })
    .catch(() => {});
}

interface Line {
  text: string;
  kind?: 'warn' | 'error';
}

const written: Line[] = [];

function append(text: string, kind?: 'warn' | 'error'): void {
  written.push({ text, kind });
  logView.append(render({ text, kind }));
  logView.scrollTop = logView.scrollHeight;
}

function render(line: Line, past = false): HTMLSpanElement {
  const span = document.createElement('span');
  span.className = [line.kind, past ? 'past' : ''].filter(Boolean).join(' ');
  span.textContent = `${line.text}\n`;
  return span;
}

function restorePreviousRun(): void {
  let lines: Line[] = [];
  try {
    const stored: unknown = JSON.parse(sessionStorage.getItem('flagon:log') ?? '[]');
    lines = Array.isArray(stored) ? (stored as Line[]) : [];
  } catch {
    return;
  }
  if (lines.length === 0) {
    return;
  }
  for (const line of lines) {
    logView.append(render(line, true));
  }
  logView.append(render({ text: '— previous run above —' }, true));
}

addEventListener('pagehide', () => {
  try {
    sessionStorage.setItem('flagon:log', JSON.stringify(written.slice(-500)));
  } catch {

  }
});

function addPhase(phase: string, detail: string): void {
  const item = document.createElement('li');
  item.textContent = detail ? `${phase}: ${detail}` : phase;
  item.dataset.phase = phase;
  phaseList.append(item);
}

interface Request {
  bundle: string;
  backend: string;
  asked: boolean;
  program: string;
  args: string[];
}

function readRequest(): Request {
  const params = new URLSearchParams(location.search);
  const asked = params.get('backend');
  return {
    bundle: params.get('bundle') ?? 'sandbox',
    backend: asked ?? defaultBackend(),
    asked: asked !== null,
    program: params.get('program') ?? '',
    args: params.getAll('arg'),
  };
}

interface Entry {
  id: string;
  description: string;
}

function remembered(bundle: string): Entry[] {
  try {
    const stored: unknown = JSON.parse(sessionStorage.getItem(`flagon:programs:${bundle}`) ?? '[]');
    return Array.isArray(stored) ? (stored as Entry[]) : [];
  } catch {
    return [];
  }
}

function remember(bundle: string, entries: Entry[]): void {
  try {
    sessionStorage.setItem(`flagon:programs:${bundle}`, JSON.stringify(entries));
  } catch {
  }
}

async function catalogOf(bundle: string): Promise<Entry[]> {
  if (!BUNDLES.includes(bundle)) {
    return [];
  }
  try {
    const response = await fetch(`/flagon/${bundle}.catalog.json`);
    if (!response.ok) {
      return [];
    }
    const read: unknown = await response.json();
    return Array.isArray(read) ? (read as Entry[]).filter((entry) => Boolean(entry?.id)) : [];
  } catch {
    return [];
  }
}

function offer(bundle: string, selected: string): void {
  const known = remembered(bundle);
  showPrograms(known, selected);
  void catalogOf(bundle).then((entries) => {
    if (entries.length === 0 || bundleSelect.value !== bundle) {
      return;
    }
    remember(bundle, entries);
    showPrograms(entries, programSelect.value || selected);
  });
}

const ANY = "the bundle's first program";

function showPrograms(entries: Entry[], selected: string): void {
  programSelect.replaceChildren(new Option(ANY, ''));
  for (const entry of entries) {
    programSelect.add(new Option(`${entry.id} — ${entry.description}`, entry.id));
  }
  if (selected && !entries.some((entry) => entry.id === selected)) {

    programSelect.add(new Option(`${selected} (not in this bundle)`, selected));
  }
  programSelect.value = selected;
}

const request = readRequest();

const proxyToken = new URLSearchParams(location.search).get('proxytoken');
if (proxyToken) {
  (window as unknown as { __flagonProxyToken: string }).__flagonProxyToken = proxyToken;
}
bundleSelect.value = request.bundle;
backendSelect.value = request.backend;
offer(request.bundle, request.program);

bundleSelect.addEventListener('change', () => {
  offer(bundleSelect.value, '');
});

runButton.addEventListener('click', () => {
  const next = new URLSearchParams();
  next.set('bundle', bundleSelect.value);
  next.set('backend', backendSelect.value);
  if (programSelect.value) {
    next.set('program', programSelect.value);
  }
  location.search = next.toString();
});

stopButton.addEventListener('click', () => {
  stopButton.disabled = true;
  window.__flagonCloseRequested = true;
  append('close requested; the program stops at its next frame');
});

const host: HostBridge = {
  log(level, text) {
    const kind = level === 'ERROR' ? 'error' : level === 'WARN' ? 'warn' : undefined;
    append(text, kind);
  },
  catalog(ids, descriptions) {
    const entries = ids.map((id, index) => ({ id, description: descriptions[index] ?? id }));
    remember(request.bundle, entries);
    showPrograms(entries, request.program);
  },
  status(phase, detail) {
    addPhase(phase, detail);
    setState('booting');
  },
  ready() {
    setState('running');
  },
  exited(code) {
    setState(code === 0 ? 'exited' : 'failed');
    append(`exit code ${code}`);
  },
  fatal(phase, message, stack) {
    setState('failed');
    append(`fatal in ${phase}: ${message}`, 'error');
    if (stack) {
      append(stack, 'error');
    }
  },
};

restorePreviousRun();
runButton.disabled = true;
setState('loading');

const bundleUrl = bundleUrlOf(request.bundle, request.backend);
if (!bundleUrl) {
  setState('failed');
  append(whyNot(request.bundle, request.backend), 'error');
  runButton.disabled = false;
} else if (request.backend === 'wasm' && !supportsWasmGc()) {
  setState('failed');
  append('this browser has no WebAssembly GC support; run this bundle with backend=js', 'error');
  runButton.disabled = false;
} else {
  if (!request.asked && request.backend !== 'wasm') {
    append('WebAssembly GC is unavailable here, so the javascript bundle is running', 'warn');
  }
  announceDownload(bundleUrl);
  const argv = request.program ? [request.program, ...request.args] : [];
  runBundle(bundleUrl, host, argv)
    .catch((error: unknown) => {
      setState('failed');
      append(`could not start the runtime: ${String(error)}`, 'error');
    })
    .finally(() => {
      runButton.disabled = false;
    });
}
