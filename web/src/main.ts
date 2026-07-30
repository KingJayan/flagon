import { runBundle, type HostBridge } from './bundle';

/** only bundles this shell ships may be imported; the name comes from the url. */
const BUNDLES: Record<string, string> = {
  sandbox: '/flagon/sandbox.js',
  example: '/flagon/example.js',
};

const app = document.querySelector<HTMLElement>('#app')!;
const stateBadge = document.querySelector<HTMLElement>('#state')!;
const phaseList = document.querySelector<HTMLOListElement>('#phases')!;
const logView = document.querySelector<HTMLPreElement>('#log')!;
const bundleSelect = document.querySelector<HTMLSelectElement>('#bundle')!;
const programSelect = document.querySelector<HTMLSelectElement>('#program')!;
const runButton = document.querySelector<HTMLButtonElement>('#run')!;

type State = 'loading' | 'booting' | 'running' | 'exited' | 'failed';

function setState(state: State): void {
  app.dataset.state = state;
  stateBadge.textContent = state;
}

function append(text: string, kind?: 'warn' | 'error'): void {
  const line = document.createElement('span');
  if (kind) {
    line.className = kind;
  }
  line.textContent = `${text}\n`;
  logView.append(line);
  logView.scrollTop = logView.scrollHeight;
}

function addPhase(phase: string, detail: string): void {
  const item = document.createElement('li');
  item.textContent = detail ? `${phase}: ${detail}` : phase;
  item.dataset.phase = phase;
  phaseList.append(item);
}

function readRequest(): { bundle: string; program: string; args: string[] } {
  const params = new URLSearchParams(location.search);
  const bundle = params.get('bundle') ?? 'sandbox';
  const program = params.get('program') ?? (bundle === 'sandbox' ? 'hello' : 'spinner');
  const args = params.getAll('arg');
  return { bundle, program, args };
}

const request = readRequest();
bundleSelect.value = request.bundle;
programSelect.value = request.program;
if (programSelect.value !== request.program) {
  // an unknown program stays selectable so the failure path can be exercised from a url
  const option = new Option(request.program, request.program, true, true);
  programSelect.add(option);
}

runButton.addEventListener('click', () => {
  const next = new URLSearchParams();
  next.set('bundle', bundleSelect.value);
  next.set('program', programSelect.value);
  location.search = next.toString();
});

const host: HostBridge = {
  log(level, text) {
    const kind = level === 'ERROR' ? 'error' : level === 'WARN' ? 'warn' : undefined;
    append(text, kind);
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

runButton.disabled = true;
setState('loading');

const bundleUrl = BUNDLES[request.bundle];
if (!bundleUrl) {
  setState('failed');
  append(`no bundle named '${request.bundle}'; try ${Object.keys(BUNDLES).join(', ')}`, 'error');
  runButton.disabled = false;
} else {
  runBundle(bundleUrl, host, [request.program, ...request.args])
    .catch((error: unknown) => {
      setState('failed');
      append(`could not start the runtime: ${String(error)}`, 'error');
    })
    .finally(() => {
      runButton.disabled = false;
    });
}
