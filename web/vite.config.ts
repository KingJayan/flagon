import { defineConfig, type Plugin, type ViteDevServer } from 'vite';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { tcpProxy } from './tcp-proxy';
import { answerNetProbe } from './net-probes';

const here = dirname(fileURLToPath(import.meta.url));


type Serving = { middlewares: ViteDevServer['middlewares'] };

function attachProbes(server: Serving): void {
  server.middlewares.use((asked, answer, next) => {
    if (!answerNetProbe(asked, answer)) {
      next();
    }
  });
}

function netProbes(): Plugin {
  return {
    name: 'flagon-net-probes',
    configureServer: attachProbes,
    configurePreviewServer: attachProbes,
  };
}

export default defineConfig({
  plugins: [tcpProxy({ allow: ['localhost:*', '127.0.0.1:*'] }), netProbes()],
  server: {
    port: 5273,
    strictPort: true,
  },
  preview: {
    port: 4273,
    strictPort: true,
  },
  build: {
    target: 'es2022',
    sourcemap: true,
    rollupOptions: {
      input: {
        shell: resolve(here, 'index.html'),
        play: resolve(here, 'play/index.html'),
      },
    },
  },
});
