import {
  BUNDLES,
  bundleUrlOf,
  readRequest,
  runBundle,
  supportsWasmGc,
  whyNot,
  type HostBridge,
} from './bundle';
import { watchFrames, type Frames } from './frames';
import { kindOf, Log, type Line } from './log';
import { installStop } from './stopping';

const app = document.querySelector<HTMLElement>('#app')!;
const stateBadge = document.querySelector<HTMLElement>('#state')!;
const phaseList = document.querySelector<HTMLOListElement>('#phases')!;
const logView = document.querySelector<HTMLPreElement>('#log')!;
const bundleSelect = document.querySelector<HTMLSelectElement>('#bundle')!;
const backendSelect = document.querySelector<HTMLSelectElement>('#backend')!;
const programSelect = document.querySelector<HTMLSelectElement>('#program')!;
const argsInput = document.querySelector<HTMLInputElement>('#args')!;
const runButton = document.querySelector<HTMLButtonElement>('#run')!;
const stopButton = document.querySelector<HTMLButtonElement>('#stop')!;
const rateBadge = document.querySelector<HTMLElement>('#rate')!;
const progress = document.querySelector<HTMLElement>('#progress')!;

let frames: Frames | undefined;

type State = 'loading' | 'booting' | 'running' | 'exited' | 'failed';

function setState(state: State): void {
  app.dataset.state = state;
  stateBadge.textContent = state;
  stateBadge.setAttribute('aria-label', `run state: ${state}`);
  stopButton.disabled = state !== 'running';
  if (state !== 'running') {
    relent();
  }
  progress.hidden = state !== 'loading' && state !== 'booting';
  if (state === 'running') {
    frames ??= watchFrames(rateBadge, 'no frames yet');
  } else {
    frames?.stop();
    frames = undefined;
  }
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


const log = new Log(logView);

const append = log.append.bind(log);

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
    log.past(line);
  }
  log.past({ text: '— previous run above —' });
}

addEventListener('pagehide', () => {
  try {
    sessionStorage.setItem('flagon:log', JSON.stringify(log.recent(500)));
  } catch {

  }
});

function addPhase(phase: string, detail: string): void {
  const item = document.createElement('li');
  item.textContent = detail ? `${phase}: ${detail}` : phase;
  item.dataset.phase = phase;
  phaseList.append(item);
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

function splitArgs(line: string): string[] {
  const found = line.match(/"[^"]*"|'[^']*'|\S+/g) ?? [];
  return found.map((word) =>
    (word.startsWith('"') && word.endsWith('"')) || (word.startsWith("'") && word.endsWith("'"))
      ? word.slice(1, -1)
      : word,
  );
}

const request = readRequest('sandbox');

bundleSelect.value = request.bundle;
backendSelect.value = request.backend;
argsInput.value = request.args.join(' ');
offer(request.bundle, request.program);

const playing = new URLSearchParams(location.search);
playing.set('bundle', request.bundle);
playing.delete('preserve');
document.querySelector<HTMLAnchorElement>('#player')!.search = playing.toString();

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
  for (const argument of splitArgs(argsInput.value)) {
    next.append('arg', argument);
  }
  location.search = next.toString();
});

const { relent } = installStop({
  button: stopButton,
  running: () => app.dataset.state === 'running',
  say: (text, kind) => append(text, kind),
});

const host: HostBridge = {
  log(level, text) {
    append(text, kindOf(level));
  },
  catalog(ids, descriptions) {
    const entries = ids.map((id, index) => ({ id, description: descriptions[index] ?? id }));
    remember(request.bundle, entries);
    if (bundleSelect.value === request.bundle) {
      showPrograms(entries, request.program);
    }
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
  append('this browser has no WASM-GC support. Re-run this bundle with backend=js', 'error');
  runButton.disabled = false;
} else {
  if (!request.asked && request.backend !== 'wasm') {
    append('WASM-GC is unavailable - defaulting to JS', 'warn');
  }
  announceDownload(bundleUrl);
  if (request.args.length > 0 && !request.program) {
    append('args are handed to a named program; dropped because empty', 'warn');
  }
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
