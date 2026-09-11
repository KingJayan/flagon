import {
  bundleUrlOf,
  readRequest,
  runBundle,
  supportsWasmGc,
  whyNot,
  type HostBridge,
} from './bundle';

const canvas = document.querySelector<HTMLCanvasElement>('#flagon-canvas')!;
const veil = document.querySelector<HTMLElement>('#veil')!;
const headline = document.querySelector<HTMLElement>('#headline')!;
const detail = document.querySelector<HTMLElement>('#detail')!;

function show(kind: 'starting' | 'exited' | 'failed', text: string, why = ''): void {
  veil.hidden = false;
  veil.dataset.kind = kind;
  headline.textContent = text;
  detail.textContent = why;
}

function hide(): void {
  veil.hidden = true;
}

const { bundle, backend, asked, program, args } = readRequest('app');

const host: HostBridge = {
  log(level, text) {
    const write = level === 'ERROR' ? console.error : level === 'WARN' ? console.warn : console.log;
    write(text);
  },
  catalog() {},
  status(phase, note) {
    show('starting', 'Starting', note ? `${phase}: ${note}` : phase);
  },
  ready() {
    hide();
    canvas.focus();
  },
  exited(code) {
    show(
      code === 0 ? 'exited' : 'failed',
      code === 0 ? 'The program exited' : `The program exited with code ${code}`,
      'Reload to run it again.',
    );
  },
  fatal(phase, message, stack) {
    show('failed', `Stopped in ${phase}`, message);
    if (stack) {
      console.error(stack);
    }
  },
};

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
