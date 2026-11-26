/** the page half of dev.flagon.platform.workers: bundle workers and their message channels */

type Received = (text: string | null, data: Int8Array | null, ended: boolean) => void;

interface Mail {
  text: string | null;
  data: Int8Array | null;
}

/** what crosses between a bundle and its worker */
export type Letter =
  | { kind: 'start'; bundle: string; args: string[]; token: string | null }
  | { kind: 'message'; text: string | null; data: ArrayBuffer | null }
  | { kind: 'closed' }
  | { kind: 'log'; level: string; text: string }
  | { kind: 'exited'; code: number }
  | { kind: 'fatal'; phase: string; message: string; stack: string };

interface Target {
  postMessage(letter: Letter, transfer: Transferable[]): void;
}

export interface WorkerApi {
  available(): boolean;
  spawn(program: string, args: string[]): number;
  parent(): number;
  send(id: number, text: string | null, data: Int8Array | null): void;
  receive(id: number, done: Received): void;
  take(id: number): Mail | null;
  isOpen(id: number): boolean;
  close(id: number): void;
}

declare global {
  var __flagonWorkers: WorkerApi | undefined;
  var __flagonBundle: string | undefined;
}

class Port {
  readonly queue: Mail[] = [];
  waiting: Received | null = null;
  open = true;

  constructor(
    readonly target: Target,
    readonly stop: () => void,
  ) {}

  deliver(arrived: Mail): void {
    const waiting = this.waiting;
    if (waiting === null) {
      this.queue.push(arrived);
    } else {
      this.waiting = null;
      waiting(arrived.text, arrived.data, false);
    }
  }

  end(): void {
    this.open = false;
    const waiting = this.waiting;
    this.waiting = null;
    waiting?.(null, null, true);
  }
}

const ports = new Map<number, Port>();
let nextPort = 1;
/** the port to whoever spawned this realm, when it is a worker */
export const PARENT = 0;

function port(id: number): Port {
  const found = ports.get(id);
  if (found === undefined) {
    throw new Error(`no worker channel ${id}`);
  }
  return found;
}

/** sends a copy of the bytes, so the java array behind them stays whole */
function letter(text: string | null, data: Int8Array | null): [Letter, Transferable[]] {
  const buffer = data === null ? null : data.slice().buffer;
  return [{ kind: 'message', text, data: buffer }, buffer === null ? [] : [buffer]];
}

function mail(text: string | null, data: ArrayBuffer | null): Mail {
  return { text, data: data === null ? null : new Int8Array(data) };
}

function report(program: string, level: string, text: string): void {
  globalThis.__flagonHost?.log(level, `[${program}] ${text}`);
  if (globalThis.__flagonHost === undefined) {
    console.info(`[${program}] ${text}`);
  }
}

function spawn(program: string, args: string[]): number {
  const bundle = globalThis.__flagonBundle;
  if (bundle === undefined) {
    throw new Error('no bundle is running here to start a worker from');
  }
  const worker = new Worker(new URL('./bundle.worker.ts', import.meta.url), { type: 'module' });
  const id = nextPort++;
  const ending = new Port(worker, () => worker.terminate());
  ports.set(id, ending);
  worker.addEventListener('message', (event: MessageEvent<Letter>) => {
    const said = event.data;
    switch (said.kind) {
      case 'message':
        ending.deliver(mail(said.text, said.data));
        break;
      case 'log':
        report(program, said.level, said.text);
        break;
      case 'closed':
        ending.end();
        break;
      case 'exited':
        ending.end();
        worker.terminate();
        break;
      case 'fatal':
        report(program, 'error', `${said.phase}: ${said.message}\n${said.stack}`);
        ending.end();
        worker.terminate();
        break;
      default:
        break;
    }
  });
  worker.addEventListener('error', (event) => {
    report(program, 'error', event.message);
    ending.end();
  });
  const token = (globalThis as { __flagonProxyToken?: string }).__flagonProxyToken ?? null;
  worker.postMessage({ kind: 'start', bundle, args: [program, ...args], token } satisfies Letter, []);
  return id;
}

function api(): WorkerApi {
  return {
    available: () => typeof Worker !== 'undefined' && globalThis.__flagonBundle !== undefined,
    spawn,
    parent: () => (ports.has(PARENT) ? PARENT : -1),
    send(id, text, data): void {
      const ending = port(id);
      if (ending.open) {
        ending.target.postMessage(...letter(text, data));
      }
    },
    receive(id, done): void {
      const ending = port(id);
      const next = ending.queue.shift();
      if (next !== undefined) {
        done(next.text, next.data, false);
      } else if (!ending.open) {
        done(null, null, true);
      } else {
        ending.waiting = done;
      }
    },
    take: (id) => port(id).queue.shift() ?? null,
    isOpen: (id) => port(id).open,
    close(id): void {
      const ending = port(id);
      if (ending.open) {
        ending.stop();
        ending.end();
      }
    },
  };
}

export function installWorkers(): void {
  globalThis.__flagonWorkers ??= api();
}

/** inside a worker: the channel to the spawning bundle, fed by its letters */
export function adoptParent(scope: Target & EventTarget): void {
  const parent = new Port(scope, () => scope.postMessage({ kind: 'closed' }, []));
  ports.set(PARENT, parent);
  scope.addEventListener('message', (event) => {
    const said = (event as MessageEvent<Letter>).data;
    if (said.kind === 'message') {
      parent.deliver(mail(said.text, said.data));
    }
  });
}
