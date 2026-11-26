/** a bundle worker: the spawning page's bundle again, running one catalog program */
import { runBundle, type HostBridge } from './bundle';
import { adoptParent, type Letter } from './workers';

const scope = self as unknown as EventTarget & {
  postMessage(letter: Letter, transfer: Transferable[]): void;
  close(): void;
};

adoptParent(scope);

const say = (letter: Letter): void => scope.postMessage(letter, []);

const host: HostBridge = {
  log: (level, text) => say({ kind: 'log', level, text }),
  catalog: () => {},
  status: () => {},
  ready: () => {},
  exited(code): void {
    say({ kind: 'exited', code });
    scope.close();
  },
  fatal: (phase, message, stack) => say({ kind: 'fatal', phase, message, stack }),
};

function start(event: Event): void {
  const said = (event as MessageEvent<Letter>).data;
  if (said.kind !== 'start') {
    return;
  }
  scope.removeEventListener('message', start);
  if (said.token !== null) {
    (globalThis as { __flagonProxyToken?: string }).__flagonProxyToken = said.token;
  }
  runBundle(said.bundle, host, said.args).catch((error: unknown) => {
    const failure = error instanceof Error ? error : new Error(String(error));
    host.fatal('boot', failure.message, failure.stack ?? '');
  });
}

scope.addEventListener('message', start);
