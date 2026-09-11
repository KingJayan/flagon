/**
 * stop is a request: it sets the flag Display.isCloseRequested() reports, and a program that
 * never asks never stops. so the button stays live, and if the request goes untaken it says so
 * and offers the one thing that always works. both pages ask the same way.
 */
const GRACE = 4000;

export interface Stopping {
  button: HTMLButtonElement;
  running: () => boolean;
  say: (text: string, kind?: 'warn') => void;
}

export function installStop(it: Stopping): { relent: () => void } {
  let insisting: number | undefined;

  function relent(): void {
    if (insisting !== undefined) {
      clearTimeout(insisting);
      insisting = undefined;
    }
    delete it.button.dataset.force;
    it.button.textContent = 'Stop';
  }

  it.button.addEventListener('click', () => {
    if (it.button.dataset.force === 'true') {
      location.reload();
      return;
    }
    window.__flagonCloseRequested = true;
    it.say('close requested; a program that polls Display.isCloseRequested() stops at its next frame');
    insisting = window.setTimeout(() => {
      if (!it.running()) {
        return;
      }
      it.button.dataset.force = 'true';
      it.button.textContent = 'Reload';
      it.say('the program has not taken the close request; only reloading the page ends it', 'warn');
    }, GRACE);
  });

  return { relent };
}
