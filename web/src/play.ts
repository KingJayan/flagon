import {
  bundleUrlOf,
  readRequest,
  runBundle,
  supportsWasmGc,
  whyNot,
  type HostBridge,
} from './bundle';
import { watchFrames, type Frames } from './frames';
import { kindOf, Log } from './log';
import { installStop } from './stopping';

const canvas = document.querySelector<HTMLCanvasElement>('#flagon-canvas')!;
const veil = document.querySelector<HTMLElement>('#veil')!;
const headline = document.querySelector<HTMLElement>('#headline')!;
const detail = document.querySelector<HTMLElement>('#detail')!;
const controls = document.querySelector<HTMLElement>('#controls')!;
const stopButton = document.querySelector<HTMLButtonElement>('#stop')!;
const fullscreenButton = document.querySelector<HTMLButtonElement>('#fullscreen')!;
const outputButton = document.querySelector<HTMLButtonElement>('#output')!;
const rateBadge = document.querySelector<HTMLElement>('#rate')!;
const logView = document.querySelector<HTMLPreElement>('#log')!;
const hint = document.querySelector<HTMLElement>('#hint')!;

let running = false;
let frames: Frames | undefined;

const log = new Log(logView);
const append = log.append.bind(log);

function show(kind: 'starting' | 'exited' | 'failed', text: string, why = ''): void {
  veil.hidden = false;
  veil.dataset.kind = kind;
  headline.textContent = text;
  detail.textContent = why;
}

function hide(): void {
  veil.hidden = true;
}


outputButton.addEventListener('click', () => {
  const showing = logView.hidden;
  logView.hidden = !showing;
  outputButton.setAttribute('aria-expanded', String(showing));
  reveal();
});

fullscreenButton.addEventListener('click', () => {
  if (document.fullscreenElement) {
    void document.exitFullscreen();
  } else {
    void document.documentElement.requestFullscreen().catch(() => {
      append('this browser would not go fullscreen', 'warn');
    });
  }
  canvas.focus();
});

document.addEventListener('fullscreenchange', () => {
  fullscreenButton.textContent = document.fullscreenElement ? 'Leave fullscreen' : 'Fullscreen';
});

const { relent } = installStop({
  button: stopButton,
  running: () => running,
  say: (text, kind) => append(text, kind),
});

const LINGER = 2500;

let hiding: number | undefined;

function reveal(): void {
  controls.dataset.shown = 'true';
  if (hiding !== undefined) {
    clearTimeout(hiding);
  }
  hiding = window.setTimeout(() => {
    if (running && !controls.matches(':hover, :focus-within')) {
      controls.dataset.shown = 'false';
    }
  }, LINGER);
}

for (const signal of ['pointermove', 'pointerdown', 'keydown'] as const) {
  window.addEventListener(signal, reveal, { passive: true });
}

document.addEventListener('pointerlockchange', () => {
  const locked = document.pointerLockElement === canvas;
  hint.hidden = !locked;
  hint.textContent = locked ? 'Press Esc to release the pointer' : '';
});


const { bundle, backend, asked, program, args } = readRequest('app');

const host: HostBridge = {
  log(level, text) {
    const write = level === 'ERROR' ? console.error : level === 'WARN' ? console.warn : console.log;
    write(text);
    append(text, kindOf(level));
  },
  catalog() {},
  status(phase, note) {
    show('starting', 'Starting', note ? `${phase}: ${note}` : phase);
  },
  ready() {
    hide();
    running = true;
    stopButton.disabled = false;
    canvas.focus();
    frames ??= watchFrames(rateBadge, '');
    reveal();
  },
  exited(code) {
    ended();
    show(
      code === 0 ? 'exited' : 'failed',
      code === 0 ? 'The program exited' : `The program exited with code ${code}`,
      'Reload to run it again.',
    );
  },
  fatal(phase, message, stack) {
    ended();
    show('failed', `Stopped in ${phase}`, message);
    append(`fatal in ${phase}: ${message}`, 'error');
    if (stack) {
      console.error(stack);
      append(stack, 'error');
    }
  },
};

function ended(): void {
  running = false;
  stopButton.disabled = true;
  frames?.stop();
  frames = undefined;
  relent();
  controls.dataset.shown = 'true';
}

canvas.tabIndex = 0;
canvas.addEventListener('contextmenu', (event) => event.preventDefault());

function missing(url: string): string {
  return bundle === 'app'
    ? 'No application has been built yet. Put a jar in vendor/ -- any jar -- and run make app. '
      + 'See docs/DOWNSTREAM.md.'
    : `The bundle at ${url} is not being served. Run make build.`;
}

async function start(): Promise<void> {
  const url = bundleUrlOf(bundle, backend);
  if (!url) {
    show('failed', 'Nothing to run', whyNot(bundle, backend));
    return;
  }
  if (backend === 'wasm' && !supportsWasmGc()) {
    show(
      'failed',
      'This browser cannot run the WebAssembly build',
      'Add ?backend=js to the address to run the JavaScript one instead.',
    );
    return;
  }
  if (!asked && backend !== 'wasm') {
    console.warn('WebAssembly GC is unavailable here, so the javascript bundle is running');
  }

  const found = await fetch(url, { method: 'HEAD' }).catch(() => undefined);
  const html = found?.headers.get('content-type')?.includes('text/html') ?? false;
  if (!found?.ok || html) {
    show('failed', 'Nothing to run', missing(url));
    return;
  }

  show('starting', 'Starting', 'loading the bundle');
  const argv = program ? [program, ...args] : [];
  await runBundle(url, host, argv);
}

start().catch((error: unknown) => {
  show('failed', 'The runtime could not start', String(error));
});
