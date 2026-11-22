/** the page half of java.net.http: one fetch per request, its timeout covering the headers */

interface Answer {
    status: number;
    url: string;
    headers: string;
    body: Int8Array;
}

type Settled = (error: string | null, answer: Answer | null) => void;

export interface Fetcher {
    send(
        method: string,
        url: string,
        headers: string,
        body: Int8Array<ArrayBuffer> | null,
        timeout: number,
        follow: boolean,
        done: Settled,
    ): void;
}

declare global {
    interface Window {
        __flagonFetch?: Fetcher;
    }
}

/** what a request reports when its timeout aborted it; BrowserFetch knows it by name */
export const TIMED_OUT = "timed out";

async function exchange(
    method: string,
    url: string,
    headers: string,
    body: Int8Array<ArrayBuffer> | null,
    timeout: number,
    follow: boolean,
): Promise<Answer> {
    const abort = new AbortController();
    const timer =
        timeout > 0 ? setTimeout(() => abort.abort(), timeout) : undefined;
    const sent = new Headers();
    const parts = headers.split("\n");
    for (let at = 0; at + 1 < parts.length; at += 2) {
        sent.append(parts[at], parts[at + 1]);
    }
    let response: Response;
    try {
        response = await fetch(url, {
            method,
            headers: sent,
            body: body ?? undefined,
            redirect: follow ? "follow" : "manual",
            signal: abort.signal,
        });
    } catch (error) {
        throw new Error(
            abort.signal.aborted
                ? TIMED_OUT
                : `fetch of ${url} failed: ${String(error)}`,
            {
                cause: error,
            },
        );
    } finally {
        clearTimeout(timer);
    }
    if (response.type === "opaqueredirect") {
        throw new Error(
            `the server at ${url} redirected, and a browser hides where to; follow redirects to reach it`,
        );
    }
    let lines = "";
    response.headers.forEach((value, name) => {
        lines += `${name}\n${value}\n`;
    });
    const bytes = await response.arrayBuffer();
    return {
        status: response.status,
        url: response.url || url,
        headers: lines,
        body: new Int8Array(bytes),
    };
}

export function fetcher(): Fetcher {
    return {
        send(method, url, headers, body, timeout, follow, done): void {
            exchange(method, url, headers, body, timeout, follow).then(
                (answer) => done(null, answer),
                (error: unknown) =>
                    done(
                        error instanceof Error ? error.message : String(error),
                        null,
                    ),
            );
        },
    };
}

export function installFetch(): void {
    window.__flagonFetch ??= fetcher();
}
