import {
  bundleUrlOf,
  readRequest,
  runBundle,
  supportsWasmGc,
  whyNot,
  type HostBridge,
} from './bundle';
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

function show(kind: 'starting' | 'exited' | 'failed', text: string, why = ''): void {
  veil.hidden = false;
  veil.dataset.kind = kind;
  headline.textContent = text;
  detail.textContent = why;
}

function hide(): void {
  veil.hidden = true;
}

const KEPT = 2000;

/** the program's output belongs on the page, not only in a devtools panel nobody opened */
function append(text: string, kind?: 'warn' | 'error'): void {
  const line = document.createElement('span');
  line.className = kind ?? '';
  line.textContent = `${text}\n`;
  logView.append(line);
  while (logView.childElementCount > KEPT) {
    logView.firstElementChild?.remove();
  }
  logView.scrollTop = logView.scrollHeight;
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

/**
 * the controls sit over the program, so they show themselves on any sign of a reader and then
 * get out of the way again. a tap counts, because that is all a phone has to offer.
 */
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

/** a program that captures the pointer has taken the reader's only way out, so say how to get it */
document.addEventListener('pointerlockchange', () => {
  const locked = document.pointerLockElement === canvas;
  hint.hidden = !locked;
  hint.textContent = locked ? 'Press Esc to release the pointer' : '';
});

function watchFrames(): void {
  let last = window.__flagonFrames ?? 0;
  rateBadge.hidden = false;
  window.setInterval(() => {
    if (!running) {
      rateBadge.hidden = true;
      return;
    }
    const total = window.__flagonFrames ?? 0;
    const drawn = total - last;
    last = total;
    rateBadge.textContent = drawn > 0 ? `${drawn} fps` : total > 0 ? 'not drawing' : '';
  }, 1000);
}

const { bundle, backend, asked, program, args } = readRequest('app');

const host: HostBridge = {
  log(level, text) {
    const kind = level === 'ERROR' ? 'error' : level === 'WARN' ? 'warn' : undefined;
    const write = level === 'ERROR' ? console.error : level === 'WARN' ? console.warn : console.log;
    write(text);
    append(text, kind);
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
    watchFrames();
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
  rateBadge.hidden = true;
  relent();
  controls.dataset.shown = 'true';
}

/** the page is the program's window, so a keypress belongs to it and not to the browser */
canvas.tabIndex = 0;
canvas.addEventListener('contextmenu', (event) => event.preventDefault());

/** `app` is the one bundle that may legitimately be absent, so say what makes it appear */
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
