import { defineConfig, type Plugin } from 'vite';
import { tcpProxy } from './tcp-proxy';

const BULK_PROBE = '/flagon-net-bulk.bin';
const BULK_PROBE_SIZE = 256 * 1024;

// a body far larger than one websocket frame, so the net demo can prove that a socket read
// reassembles every byte in order. generated rather than committed, being a quarter megabyte
function bulkProbe(): Plugin {
  const body = Buffer.alloc(BULK_PROBE_SIZE);
  for (let at = 0; at < body.length; at++) {
    body[at] = at % 251;
  }
  return {
    name: 'flagon-bulk-probe',
    configureServer(server) {
      server.middlewares.use(BULK_PROBE, (_asked, answer) => {
        answer.writeHead(200, {
          'content-type': 'application/octet-stream',
          'content-length': String(body.length),
        });
        answer.end(body);
      });
    },
  };
}

export default defineConfig({
  // the sandbox talks to the dev server itself, and to nothing else
  plugins: [tcpProxy({ allow: ['localhost', '127.0.0.1'] }), bulkProbe()],
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
