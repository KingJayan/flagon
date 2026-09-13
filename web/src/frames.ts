export interface Frames {
  stop(): void;
}

export function watchFrames(badge: HTMLElement, idle: string): Frames {
  let last = window.__flagonFrames ?? 0;
  badge.hidden = false;
  badge.textContent = idle;
  const ticker = window.setInterval(() => {
    const total = window.__flagonFrames ?? 0;
    const drawn = total - last;
    last = total;
    badge.textContent = drawn > 0 ? `${drawn} fps` : total > 0 ? 'not drawing' : idle;
  }, 1000);
  return {
    stop(): void {
      clearInterval(ticker);
      badge.hidden = true;
    },
  };
}
