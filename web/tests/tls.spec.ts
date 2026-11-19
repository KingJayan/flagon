import { test, expect } from '@playwright/test';
import http from 'node:http';
import https from 'node:https';
import net from 'node:net';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import type { AddressInfo } from 'node:net';
import WebSocket from 'ws';
import { attachTcpProxy, TCP_PROXY_PATH } from '../tcp-proxy';

function selfSigned(): { key: string; cert: string } {
  const at = mkdtempSync(path.join(tmpdir(), 'flagon-tls-'));
  const key = path.join(at, 'key.pem');
  const cert = path.join(at, 'cert.pem');
  execFileSync('openssl', [
    'req', '-x509', '-newkey', 'rsa:2048',
    '-keyout', key, '-out', cert,
    '-days', '1', '-nodes',
    '-subj', '/CN=localhost',
    '-addext', 'subjectAltName=DNS:localhost,IP:127.0.0.1',
  ], { stdio: 'ignore' });
  return { key: readFileSync(key, 'utf8'), cert: readFileSync(cert, 'utf8') };
}

async function proxyOn(tlsCa: string): Promise<http.Server> {
  const server = http.createServer();
  attachTcpProxy(server, { allow: ['127.0.0.1:*'], tlsCa });
  await new Promise<void>((up) => server.listen(0, '127.0.0.1', up));
  return server;
}

function portOf(server: { address(): AddressInfo | string | null }): number {
  return (server.address() as AddressInfo).port;
}

function through(proxy: number, host: string, port: number, tls: boolean): Promise<string> {
  const query = `host=${host}&port=${port}${tls ? '&tls=1' : ''}`;
  const socket = new WebSocket(`ws://127.0.0.1:${proxy}${TCP_PROXY_PATH}?${query}`, {
    headers: { origin: `http://127.0.0.1:${proxy}` },
  });
  return new Promise<string>((settle) => {
    let answer = '';
    socket.on('message', (data: Buffer, isBinary: boolean) => {
      if (!isBinary) {
        const said = data.toString();
        if (said === 'open') {
          socket.send(Buffer.from(`GET / HTTP/1.1\r\nHost: ${host}\r\nConnection: close\r\n\r\n`));
          return;
        }
        settle(said);
        return;
      }
      answer += data.toString();
    });
    socket.on('close', () => settle(answer.split('\r\n')[0]));
    socket.on('error', (failed: Error) => settle(failed.message));
  });
}

test.describe('tls the proxy speaks on the page\'s behalf', () => {
  test('carries an https server to a page that cannot do the handshake itself', async () => {
    const { key, cert } = selfSigned();
    const secure = https.createServer({ key, cert }, (_asked, answer) => {
      answer.writeHead(200, { 'content-length': '2' });
      answer.end('hi');
    });
    await new Promise<void>((up) => secure.listen(0, '127.0.0.1', up));
    const proxy = await proxyOn(cert);

    const said = await through(portOf(proxy), '127.0.0.1', portOf(secure), true);
    expect(said).toBe('HTTP/1.1 200 OK');

    secure.close();
    proxy.close();
  });

  test('refuses a certificate nobody told it to trust', async () => {
    const { key, cert } = selfSigned();
    const secure = https.createServer({ key, cert }, (_asked, answer) => answer.end('hi'));
    await new Promise<void>((up) => secure.listen(0, '127.0.0.1', up));
    const proxy = await proxyOn('');

    const said = await through(portOf(proxy), '127.0.0.1', portOf(secure), true);
    expect(said).toContain('self-signed certificate');

    secure.close();
    proxy.close();
  });

  test('leaves a plain socket plain when tls was not asked for', async () => {
    const plain = http.createServer((_asked, answer) => {
      answer.writeHead(200, { 'content-length': '2' });
      answer.end('hi');
    });
    await new Promise<void>((up) => plain.listen(0, '127.0.0.1', up));
    const proxy = await proxyOn('');

    const said = await through(portOf(proxy), '127.0.0.1', portOf(plain), false);
    expect(said).toBe('HTTP/1.1 200 OK');

    plain.close();
    proxy.close();
  });

  test('fails the handshake rather than sending plaintext to a server with no tls', async () => {
    const plain = net.createServer((socket) => socket.end('nothing here'));
    await new Promise<void>((up) => plain.listen(0, '127.0.0.1', up));
    const proxy = await proxyOn('');

    const said = await through(portOf(proxy), '127.0.0.1', portOf(plain), true);
    expect(said).not.toBe('HTTP/1.1 200 OK');

    plain.close();
    proxy.close();
  });
});
