export type Kind = 'warn' | 'error';

export interface Line {
  text: string;
  kind?: Kind;
}

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

  constructor(
    private readonly view: HTMLElement,
    private readonly kept = 2000,
  ) {}

  append(text: string, kind?: Kind): void {
    const line: Line = { text, kind };
    this.written.push(line);
    this.view.append(spanOf(line));
    this.trim();
    this.view.scrollTop = this.view.scrollHeight;
  }

  past(line: Line): void {
    this.view.append(spanOf(line, true));
  }

  recent(count: number): Line[] {
    return this.written.slice(-count);
  }

  private trim(): void {
    if (this.written.length > this.kept) {
      this.written.splice(0, this.written.length - this.kept);
    }
    while (this.view.childElementCount > this.kept) {
      this.view.firstElementChild?.remove();
    }
  }
}
