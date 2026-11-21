export interface Demo {
  bundle: string;
  program: string;
  name: string;
}

export const DEMOS: readonly Demo[] = [
  { bundle: 'example', program: 'blocks', name: 'Blocks: a brick-breaker game' },
  { bundle: 'example', program: 'spinner', name: 'Spinner: an LWJGL 2 application' },
  { bundle: 'example', program: 'hello-lwjgl3', name: "LWJGL 3's guide sample" },
  { bundle: 'sandbox', program: 'threading', name: 'Worker threads behind a render loop' },
  { bundle: 'sandbox', program: 'core-profile', name: 'Instanced drawing on a GL 3.2 core profile' },
  { bundle: 'sandbox', program: 'shaders', name: 'GLSL shader program' },
  { bundle: 'sandbox', program: 'texture', name: 'Texture upload and sampling' },
  { bundle: 'sandbox', program: 'input', name: 'Keyboard and mouse input' },
  { bundle: 'sandbox', program: 'audio', name: 'OpenAL audio' },
  { bundle: 'sandbox', program: 'files', name: 'java.io files on OPFS' },
];

export const keyOf = (demo: { bundle: string; program: string }): string =>
  `${demo.bundle}/${demo.program}`;

const LOOPBACK = new Set(['localhost', '127.0.0.1', '[::1]']);

/** the full workbench lists every bundle and self-test; the public site lists demos only. */
export async function isWorkbench(): Promise<boolean> {
  const view = new URLSearchParams(location.search).get('view');
  if (view === 'all' || view === 'demos') {
    return view === 'all';
  }
  if (LOOPBACK.has(location.hostname)) {
    return true;
  }
  try {
    return (await fetch('/flagon/app.catalog.json', { method: 'HEAD' })).ok;
  } catch {
    return false;
  }
}
