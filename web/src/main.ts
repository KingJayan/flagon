import { runBundle, type HostBridge } from './bundle';

const BUNDLE_URL = '/decanter/sandbox.js';

const app = document.querySelector<HTMLElement>('#app')!;
const stateBadge = document.querySelector<HTMLElement>('#state')!;
const phaseList = document.querySelector<HTMLOListElement>('#phases')!;
const logView = document.querySelector<HTMLPreElement>('#log')!;
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

function readRequest(): { program: string; args: string[] } {
  const params = new URLSearchParams(location.search);
  const program = params.get('program') ?? 'hello';
  const args = params.getAll('arg');
  return { program, args };
}

const request = readRequest();
programSelect.value = request.program;
if (programSelect.value !== request.program) {
  // an unknown program stays selectable so the failure path can be exercised from a url
  const option = new Option(request.program, request.program, true, true);
  programSelect.add(option);
}

runButton.addEventListener('click', () => {
  const next = new URLSearchParams();
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

runBundle(BUNDLE_URL, host, [request.program, ...request.args])
  .catch((error: unknown) => {
    setState('failed');
    append(`could not start the runtime: ${String(error)}`, 'error');
  })
  .finally(() => {
    runButton.disabled = false;
  });
