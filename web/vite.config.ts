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

const FLOOD = '/flagon-net-flood.bin';
const FLOOD_SIZE = 32 * 1024 * 1024;

function flood(): Plugin {
  return {
    name: 'flagon-net-flood',
    configureServer(server) {
      server.middlewares.use(FLOOD, (asked, answer) => {
        if (asked.method === 'POST') {
          let taken = 0;
          asked.on('data', (chunk: Buffer) => {
            taken += chunk.length;
          });
          asked.on('end', () => {
            // a length, not chunked framing, the page reading this response by hand
            const body = 'taken ' + taken;
            answer.writeHead(200, {
              'content-type': 'text/plain',
              'content-length': String(body.length),
            });
            answer.end(body);
          });
          return;
        }
        answer.writeHead(200, {
          'content-type': 'application/octet-stream',
          'content-length': String(FLOOD_SIZE),
        });
        const chunk = Buffer.alloc(64 * 1024);
        let written = 0;
        const pump = (): void => {
          while (written < FLOOD_SIZE) {
            written += chunk.length;
            if (!answer.write(chunk)) {
              answer.once('drain', pump);
              return;
            }
          }
          answer.end();
        };
        pump();
      });
    },
  };
}

export default defineConfig({
  // the sandbox talks to the dev server itself, and to nothing else
  plugins: [tcpProxy({ allow: ['localhost:*', '127.0.0.1:*'] }), bulkProbe(), flood()],
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
  },
});
