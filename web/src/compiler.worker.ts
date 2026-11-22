import { COMPILER } from './playground-paths';

interface TeaVM {
  exports: { installWorker(): void };
}

const runtime = (await import(/* @vite-ignore */ `${COMPILER}compiler.wasm-runtime.js`)) as {
  load(path: string): Promise<TeaVM>;
};
const teavm = await runtime.load(`${COMPILER}compiler.wasm`);
teavm.exports.installWorker();
