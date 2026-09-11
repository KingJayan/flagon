import type http from 'node:http';


export const BULK_PROBE = '/flagon-net-bulk.bin';
export const BULK_PROBE_SIZE = 256 * 1024;

export const FLOOD = '/flagon-net-flood.bin';
export const FLOOD_SIZE = 32 * 1024 * 1024;


function bulkBody(): Buffer {
  const body = Buffer.alloc(BULK_PROBE_SIZE);
  for (let at = 0; at < body.length; at++) {
    body[at] = at % 251;
  }
  return body;
}

let bulk: Buffer | undefined;

function sendBulk(answer: http.ServerResponse): void {
  bulk ??= bulkBody();
  answer.writeHead(200, {
    'content-type': 'application/octet-stream',
    'content-length': String(bulk.length),
  });
  answer.end(bulk);
}

function sendFlood(asked: http.IncomingMessage, answer: http.ServerResponse): void {
  if (asked.method === 'POST') {
    let taken = 0;
    asked.on('data', (chunk: Buffer) => {
      taken += chunk.length;
    });
    asked.on('end', () => {

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

  answer.on('error', () => {});
  const chunk = Buffer.alloc(64 * 1024);
  let written = 0;
  const pump = (): void => {
    while (written < FLOOD_SIZE) {
      if (answer.destroyed) {
        return;
      }
      written += chunk.length;
      if (!answer.write(chunk)) {
        answer.once('drain', pump);
        return;
      }
    }
    answer.end();
  };
  pump();
}

export function answerNetProbe(asked: http.IncomingMessage, answer: http.ServerResponse): boolean {
  const path = new URL(asked.url ?? '/', 'http://site.invalid').pathname;
  if (path === BULK_PROBE) {
    sendBulk(answer);
    return true;
  }
  if (path === FLOOD) {
    sendFlood(asked, answer);
    return true;
  }
  return false;
}
