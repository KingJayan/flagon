import { supportsWasmGc } from './bundle';
import { EXAMPLES } from './examples';
import { Log } from './log';
import { COMPILER } from './playground-paths';
import type { Said } from './program.worker';

const MAX_SOURCE = 64 * 1024;
const RUN_LIMIT_MS = 10_000;
const LINE_LIMIT = 2000;
const SAVED = 'flagon:playground:source';
const LAUNCHER = '__FlagonRun';

/** runs Main like a jvm would: report an uncaught exception, then wait for the other threads */
const LAUNCHER_SOURCE = `final class ${LAUNCHER} {
    public static void main(String[] args) throws InterruptedException {
        int code = 0;
        try {
            Main.main(args);
        } catch (Throwable thrown) {
            System.err.print("Exception in thread \\"main\\" ");
            thrown.printStackTrace();
            code = 1;
        }
        // a started thread counts once it first runs, so wait a turn before counting
        do {
            Thread.sleep(10);
        } while (Thread.activeCount() > 1);
        System.exit(code);
    }
}
`;

type State = 'loading' | 'ready' | 'compiling' | 'running' | 'exited' | 'failed';

type FromCompiler =
  | { command: 'initialized' }
  | { command: 'ok'; id: string }
  | { command: 'error'; id: string; text: string }
  | { command: 'phase'; id: string; phase: string }
  | {
      command: 'compiler-diagnostic';
      id: string;
      severity: string;
      fileName: string;
      lineNumber: number;
      columnNumber: number;
      message: string;
    }
  | { command: 'diagnostic'; id: string; severity: string; lineNumber: number; message: string }
  | { command: 'compilation-complete'; id: string; status: string; script?: Int8Array };

const app = document.querySelector<HTMLElement>('#app')!;
const stateBadge = document.querySelector<HTMLElement>('#state')!;
const code = document.querySelector<HTMLTextAreaElement>('#code')!;
const gutter = document.querySelector<HTMLElement>('#gutter')!;
const runButton = document.querySelector<HTMLButtonElement>('#run')!;
const stopButton = document.querySelector<HTMLButtonElement>('#stop')!;
const exampleSelect = document.querySelector<HTMLSelectElement>('#example')!;
const problems = document.querySelector<HTMLOListElement>('#problems')!;
const log = new Log(document.querySelector<HTMLPreElement>('#log')!, LINE_LIMIT);

let state: State = 'loading';
let compiler: Worker | undefined;
let program: Worker | undefined;
let deadline = 0;
let printed = 0;
let asked = 0;
let numbered = 0;

function setState(next: State): void {
  state = next;
  app.dataset.state = next;
  stateBadge.textContent = next;
  stateBadge.setAttribute('aria-label', `state: ${next}`);
  runButton.disabled = next === 'loading' || next === 'compiling';
  stopButton.disabled = next !== 'running';
}

function fail(text: string): void {
  log.append(text, 'error');
  setState('failed');
}

function load(): string {
  try {
    return localStorage.getItem(SAVED) ?? EXAMPLES[0]!.code;
  } catch {
    return EXAMPLES[0]!.code;
  }
}

function save(): void {
  try {
    localStorage.setItem(SAVED, code.value);
  } catch {
  }
}

function number(): void {
  const lines = code.value.split('\n').length;
  if (numbered !== lines) {
    numbered = lines;
    gutter.textContent = Array.from({ length: lines }, (_, at) => at + 1).join('\n');
  }
  gutter.scrollTop = code.scrollTop;
}

function show(source: string): void {
  code.value = source;
  number();
  save();
}

function selectLine(line: number): void {
  const lines = code.value.split('\n');
  const start = lines.slice(0, line - 1).reduce((sum, text) => sum + text.length + 1, 0);
  code.focus();
  code.setSelectionRange(start, start + (lines[line - 1]?.length ?? 0));
}

function problem(line: number, column: number, message: string, error: boolean): void {
  const item = document.createElement('li');
  const button = document.createElement('button');
  button.type = 'button';
  button.className = error ? 'error' : 'warn';
  button.textContent = `${line}:${column} ${message}`;
  button.addEventListener('click', () => selectLine(line));
  item.append(button);
  problems.append(item);
}

function stopProgram(why?: string): void {
  if (!program) {
    return;
  }
  program.terminate();
  program = undefined;
  if (why) {
    log.append(why, 'warn');
  }
  setState('exited');
}

function runProgram(script: Int8Array): void {
  setState('running');
  printed = 0;
  const worker = new Worker(new URL('./program.worker.ts', import.meta.url), { type: 'module' });
  program = worker;
  const run = (deadline = window.setTimeout(
    () => stopProgram(`stopped after ${RUN_LIMIT_MS / 1000} s`),
    RUN_LIMIT_MS,
  ));
  worker.addEventListener('message', (event: MessageEvent<Said>) => {
    if (program !== worker) {
      return;
    }
    const said = event.data;
    if (said.kind === 'lines') {
      for (const line of said.lines) {
        log.append(line.text, line.error ? 'error' : undefined);
      }
      printed += said.lines.length;
      if (printed >= LINE_LIMIT) {
        window.clearTimeout(run);
        stopProgram(`stopped after ${LINE_LIMIT} lines`);
      }
    } else if (said.kind === 'exited') {
      window.clearTimeout(run);
      stopProgram(said.code === 0 ? undefined : `exit code ${said.code}`);
    } else {
      window.clearTimeout(run);
      program = undefined;
      worker.terminate();
      fail(said.message);
    }
  });
  worker.addEventListener('error', (event) => {
    window.clearTimeout(run);
    program = undefined;
    fail(event.message || 'the program could not start');
  });
  worker.postMessage({ code: script.slice().buffer }, []);
}

function compile(): void {
  if (!compiler || state === 'loading' || state === 'compiling') {
    return;
  }
  if (code.value.length > MAX_SOURCE) {
    fail(`the source is over ${MAX_SOURCE / 1024} KB`);
    return;
  }
  window.clearTimeout(deadline);
  stopProgram();
  log.clear();
  problems.replaceChildren();
  setState('compiling');
  asked += 1;
  compiler.postMessage({
    id: String(asked),
    command: 'compile',
    files: [
      { path: 'Main.java', content: code.value },
      { path: `${LAUNCHER}.java`, content: LAUNCHER_SOURCE },
    ],
    options: { outputName: 'app', mainClass: LAUNCHER },
  });
}

function hear(message: FromCompiler): void {
  switch (message.command) {
    case 'initialized':
      compiler!.postMessage({
        id: 'classlib',
        command: 'load-classlib',
        url: `${COMPILER}compile-classlib-teavm.bin`,
        runtimeUrl: `${COMPILER}runtime-classlib-teavm.bin`,
      });
      return;
    case 'ok':
      setState('ready');
      return;
    case 'error':
      fail(message.text);
      return;
    case 'compiler-diagnostic':
    case 'diagnostic': {
      const error = message.severity.toLowerCase() === 'error';
      if (message.command === 'compiler-diagnostic' && message.fileName.includes(LAUNCHER)) {
        log.append('Main.java needs a class named Main with a main method', 'error');
        return;
      }
      const column = message.command === 'compiler-diagnostic' ? message.columnNumber : 0;
      if (message.lineNumber > 0) {
        problem(message.lineNumber, column, message.message, error);
      }
      log.append(
        message.lineNumber > 0 ? `Main.java:${message.lineNumber}: ${message.message}` : message.message,
        error ? 'error' : 'warn',
      );
      return;
    }
    case 'compilation-complete':
      if (message.status === 'successful' && message.script) {
        runProgram(message.script);
      } else {
        setState('failed');
      }
      return;
    default:
  }
}

function startCompiler(): void {
  if (!supportsWasmGc()) {
    fail('this browser has no WASM-GC support, which the compiler needs');
    return;
  }
  log.append('loading javac and TeaVM');
  compiler = new Worker(new URL('./compiler.worker.ts', import.meta.url), { type: 'module' });
  compiler.addEventListener('message', (event: MessageEvent<FromCompiler>) => hear(event.data));
  compiler.addEventListener('error', (event) => fail(event.message || 'the compiler could not start'));
}

for (const [index, example] of EXAMPLES.entries()) {
  exampleSelect.add(new Option(example.name, String(index)));
}
exampleSelect.addEventListener('change', () => {
  const example = EXAMPLES[Number(exampleSelect.value)];
  if (example) {
    show(example.code);
  }
  exampleSelect.value = '';
});

code.addEventListener('input', () => {
  number();
  save();
});
code.addEventListener('scroll', () => {
  gutter.scrollTop = code.scrollTop;
});
code.addEventListener('keydown', (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault();
    compile();
  } else if (event.key === 'Tab' && !event.shiftKey && !code.dataset.leaving) {
    event.preventDefault();
    code.setRangeText('    ', code.selectionStart, code.selectionEnd, 'end');
    number();
    save();
  } else if (event.key === 'Escape') {
    code.dataset.leaving = 'true';
  }
});
code.addEventListener('blur', () => delete code.dataset.leaving);
code.addEventListener('keyup', (event) => {
  if (event.key !== 'Escape' && event.key !== 'Tab') {
    delete code.dataset.leaving;
  }
});

runButton.addEventListener('click', compile);
stopButton.addEventListener('click', () => {
  window.clearTimeout(deadline);
  stopProgram('stopped');
});

show(load());
setState('loading');
startCompiler();
