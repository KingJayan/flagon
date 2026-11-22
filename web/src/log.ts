export type Kind = 'warn' | 'error';

export interface Line {
  text: string;
  kind?: Kind;
}

/** how far from the bottom still counts as reading the end */
const NEAR_BOTTOM = 8;

export function kindOf(level: string): Kind | undefined {
  return level === 'ERROR' ? 'error' : level === 'WARN' ? 'warn' : undefined;
}

function spanOf(line: Line, past = false): HTMLSpanElement {
  const span = document.createElement('span');
  span.className = [line.kind, past ? 'past' : ''].filter(Boolean).join(' ');
  span.textContent = `${line.text}\n`;
  return span;
}

export class Log {
  private readonly written: Line[] = [];

  private pending: DocumentFragment | undefined;
  private frame = 0;

  constructor(
    private readonly view: HTMLElement,
    private readonly kept = 2000,
  ) {}

  append(text: string, kind?: Kind): void {
    const line: Line = { text, kind };
    this.written.push(line);
    if (this.written.length > this.kept) {
      this.written.splice(0, this.written.length - this.kept);
    }
    this.hold(spanOf(line));
  }

  past(line: Line): void {
    this.hold(spanOf(line, true));
  }

  clear(): void {
    this.written.length = 0;
    this.pending = undefined;
    this.view.replaceChildren();
  }

  recent(count: number): Line[] {
    return this.written.slice(-count);
  }

  /** one layout per frame: reading scrollHeight after every line lays the page out again */
  private hold(span: HTMLSpanElement): void {
    this.pending ??= document.createDocumentFragment();
    this.pending.append(span);
    this.frame ||= requestAnimationFrame(() => this.flush());
  }

  private flush(): void {
    this.frame = 0;
    const pending = this.pending;
    this.pending = undefined;
    if (!pending) {
      return;
    }
    const following = this.isAtBottom();
    this.view.append(pending);
    while (this.view.childElementCount > this.kept) {
      this.view.firstElementChild?.remove();
    }
    if (following) {
      this.view.scrollTop = this.view.scrollHeight;
    }
  }

  /** a reader who scrolled up keeps their place */
  private isAtBottom(): boolean {
    const below = this.view.scrollHeight - this.view.scrollTop - this.view.clientHeight;
    return below <= NEAR_BOTTOM;
  }
}
