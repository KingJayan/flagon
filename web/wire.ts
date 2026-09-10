/**
 * the words the page and the proxy say to each other. both sides import this, so renaming one
 * of them cannot rename it on only one side.
 */

export const TCP_PROXY_PATH = '/__flagon/tcp';

/** the proxy says this once the far socket is connected */
export const OPENED = 'open';

/** the page says these to pace the proxy */
export const PAUSE = 'pause';
export const RESUME = 'resume';
