import { COMPILER } from './playground-paths';

interface Program {
  exports: { main(args: string[]): unknown };
}

interface Runtime {
  load(
    code: ArrayBuffer,
    options: {
      installImports(imports: Record<string, Record<string, unknown>>): void;
      onExit(code: number): void;
    },
  ): Promise<Program>;
}

export type Said =
  | { kind: 'lines'; lines: { text: string; error: boolean }[] }
  | { kind: 'exited'; code: number }
  | { kind: 'failed'; message: string };

const say = (message: Said): void => postMessage(message);

const pending: { text: string; error: boolean }[] = [];
let flushing = 0;

function flush(): void {
  flushing = 0;
  if (pending.length > 0) {
    say({ kind: 'lines', lines: pending.splice(0) });
  }
}

/** teavm writes one UTF-16 unit per call */
function stream(error: boolean): (unit: number) => void {
  let line = '';
  return (unit) => {
    if (unit === 0x0a) {
      pending.push({ text: line, error });
      line = '';
      flushing ||= self.setTimeout(flush, 30);
    } else {
      line += String.fromCharCode(unit);
    }
  };
}

addEventListener('message', async (event: MessageEvent<{ code: ArrayBuffer }>) => {
  try {
    const runtime = (await import(/* @vite-ignore */ `${COMPILER}compiler.wasm-runtime.js`)) as Runtime;
    const program = await runtime.load(event.data.code, {
      installImports(imports) {
        imports.teavmConsole!.putcharStdout = stream(false);
        imports.teavmConsole!.putcharStderr = stream(true);
      },
      onExit(code) {
        flush();
        say({ kind: 'exited', code });
      },
    });
    await program.exports.main([]);
  } catch (error) {
    flush();
    say({ kind: 'failed', message: error instanceof Error ? error.message : String(error) });
  }
});
