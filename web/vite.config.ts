import { defineConfig } from 'vite';
import { tcpProxy } from './tcp-proxy';

export default defineConfig({
  // the sandbox talks to the dev server itself, and to nothing else
  plugins: [tcpProxy({ allow: ['localhost', '127.0.0.1'] })],
  server: {
    port: 5273,
    strictPort: true,
    headers: {
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'require-corp',
    },
  },
  preview: {
    port: 4273,
    strictPort: true,
    headers: {
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'require-corp',
    },
  },
  build: {
    target: 'es2022',
    sourcemap: true,
  },
});
