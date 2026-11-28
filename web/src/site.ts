export interface Demo {
  bundle: string;
  program: string;
  name: string;
  /** the program's main class, from the repository root */
  source: string;
}

const SANDBOX = 'modules/bundles/sandbox/src/main/java/dev/flagon/sandbox';
const EXAMPLES = 'examples/app/src/main/java/org/example';

export const SOURCE_ROOT = 'https://github.com/KingJayan/flagon/blob/main/';

export const DEMOS: readonly Demo[] = [
  {
    bundle: 'example',
    program: 'blocks',
    name: 'Blocks: a brick-breaker game',
    source: `${EXAMPLES}/blocks/BlocksGame.java`,
  },
  {
    bundle: 'example',
    program: 'spinner',
    name: 'Spinner: an LWJGL 2 application',
    source: `${EXAMPLES}/spinner/SpinnerApp.java`,
  },
  {
    bundle: 'example',
    program: 'hello-lwjgl3',
    name: "LWJGL 3's guide sample",
    source: 'examples/thirdparty/src/main/java/org/lwjgl/guide/HelloWorld.java',
  },
  {
    bundle: 'sandbox',
    program: 'threading',
    name: 'Worker threads behind a render loop',
    source: `${SANDBOX}/ThreadingDemoProgram.java`,
  },
  {
    bundle: 'sandbox',
    program: 'core-profile',
    name: 'Instanced drawing on a GL 3.2 core profile',
    source: `${SANDBOX}/CoreProfileDemoProgram.java`,
  },
  {
    bundle: 'sandbox',
    program: 'shaders',
    name: 'GLSL shader program',
    source: `${SANDBOX}/ShaderDemoProgram.java`,
  },
  {
    bundle: 'sandbox',
    program: 'texture',
    name: 'Texture upload and sampling',
    source: `${SANDBOX}/TextureDemoProgram.java`,
  },
  {
    bundle: 'sandbox',
    program: 'input',
    name: 'Keyboard and mouse input',
    source: `${SANDBOX}/InputDemoProgram.java`,
  },
  {
    bundle: 'sandbox',
    program: 'audio',
    name: 'OpenAL audio',
    source: `${SANDBOX}/AudioDemoProgram.java`,
  },
  {
    bundle: 'sandbox',
    program: 'files',
    name: 'java.io files on OPFS',
    source: `${SANDBOX}/FileSystemDemoProgram.java`,
  },
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
