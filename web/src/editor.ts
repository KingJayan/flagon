import { CodeJar } from 'codejar';

const KEYWORDS = new Set(
  (
    'abstract assert boolean break byte case catch char class const continue default do double ' +
    'else enum extends false final finally float for goto if implements import instanceof int ' +
    'interface long native new null package permits private protected public record return ' +
    'sealed short static strictfp super switch synchronized this throw throws transient true try ' +
    'var void volatile while yield'
  ).split(' '),
);

const NAMES = (
  'System out err println print printf format String StringBuilder append toString length ' +
  'charAt substring equals hashCode Math abs max min sqrt pow random Integer Long Double Float ' +
  'Boolean Character Object Arrays asList sort stream Collections List ArrayList Map HashMap ' +
  'Set HashSet Optional Iterator Comparator Thread Runnable sleep start join currentTimeMillis ' +
  'nanoTime Exception RuntimeException IllegalArgumentException IllegalStateException ' +
  'InterruptedException Override FunctionalInterface main args size add get put remove ' +
  'contains isEmpty forEach filter map collect toList'
).split(' ');

const TOKEN =
  /(\/\/[^\n]*|\/\*[\s\S]*?(?:\*\/|$))|("""[\s\S]*?(?:"""|$)|"(?:\\.|[^"\\\n])*"?|'(?:\\.|[^'\\\n])*'?)|(@[A-Za-z_]\w*)|\b(0[xX][\da-fA-F_]+[lL]?|0[bB][01_]+[lL]?|\d[\d_]*(?:\.[\d_]*)?(?:[eE][+-]?\d+)?[lLfFdD]?)|([A-Za-z_$][\w$]*)/g;

const escape = (text: string): string =>
  text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const span = (kind: string, text: string): string => `<span class="${kind}">${escape(text)}</span>`;

/** Java source as HTML with a span around each comment, string, keyword, type and number */
export function highlightJava(source: string): string {
  let html = '';
  let at = 0;
  for (const match of source.matchAll(TOKEN)) {
    html += escape(source.slice(at, match.index));
    const [text, comment, string, annotation, number, word] = match;
    if (comment) {
      html += span('comment', text);
    } else if (string) {
      html += span('string', text);
    } else if (annotation || number) {
      html += span('literal', text);
    } else if (word && KEYWORDS.has(word)) {
      html += span('keyword', text);
    } else if (word && /^[A-Z]/.test(word)) {
      html += span('type', text);
    } else {
      html += escape(text);
    }
    at = match.index + text.length;
  }
  return html + escape(source.slice(at));
}

/** words that complete prefix: keywords, common JDK names and the source's own identifiers */
export function completions(source: string, prefix: string, limit = 8): string[] {
  const words = new Set([...KEYWORDS, ...NAMES]);
  for (const [word] of source.matchAll(/[A-Za-z_$][\w$]{2,}/g)) {
    words.add(word);
  }
  const lower = prefix.toLowerCase();
  const found = [...words].filter(
    (word) => word !== prefix && word.toLowerCase().startsWith(lower),
  );
  found.sort(
    (a, b) =>
      Number(!b.startsWith(prefix)) - Number(!a.startsWith(prefix)) ||
      a.length - b.length ||
      a.localeCompare(b),
  );
  return found.slice(0, limit);
}

export interface Editor {
  value: string;
  selectLine(line: number): void;
}

export function createEditor(
  element: HTMLElement,
  list: HTMLUListElement,
  onChange: () => void,
  onRun: () => void,
): Editor {
  let shown: string[] = [];
  let chosen = 0;
  let prefixStart = 0;

  function close(): void {
    shown = [];
    list.hidden = true;
    element.removeAttribute('aria-activedescendant');
  }

  function choose(index: number): void {
    chosen = index;
    for (const [at, item] of [...list.children].entries()) {
      item.setAttribute('aria-selected', String(at === index));
    }
    element.setAttribute('aria-activedescendant', `completion-${index}`);
  }

  function open(force: boolean): void {
    const caret = jar.save();
    const source = jar.toString();
    const prefix = /[A-Za-z_$][\w$]*$/.exec(source.slice(0, caret.start))?.[0] ?? '';
    shown = caret.start === caret.end && (force || prefix.length >= 2) ? completions(source, prefix) : [];
    if (shown.length === 0) {
      close();
      return;
    }
    prefixStart = caret.start - prefix.length;
    list.replaceChildren(
      ...shown.map((word, at) => {
        const item = document.createElement('li');
        item.id = `completion-${at}`;
        item.role = 'option';
        item.textContent = word;
        item.addEventListener('mousedown', (event) => {
          event.preventDefault();
          accept(at);
        });
        return item;
      }),
    );
    const range = getSelection()!.getRangeAt(0).getClientRects()[0];
    const box = element.parentElement!.getBoundingClientRect();
    if (range) {
      list.style.left = `${range.left - box.left}px`;
      list.style.top = `${range.bottom - box.top + 2}px`;
    }
    list.hidden = false;
    choose(0);
  }

  function accept(index: number): void {
    const word = shown[index]!;
    const caret = jar.save();
    const source = jar.toString();
    jar.recordHistory();
    jar.updateCode(source.slice(0, prefixStart) + word + source.slice(caret.end));
    const end = prefixStart + word.length;
    jar.restore({ start: end, end });
    jar.recordHistory();
    close();
  }

  // runs before CodeJar's own handler, which skips an event already handled here
  element.addEventListener('keydown', (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      close();
      onRun();
    } else if (event.ctrlKey && event.key === ' ') {
      event.preventDefault();
      open(true);
    } else if (shown.length > 0 && (event.key === 'ArrowDown' || event.key === 'ArrowUp')) {
      event.preventDefault();
      choose((chosen + (event.key === 'ArrowDown' ? 1 : shown.length - 1)) % shown.length);
    } else if (shown.length > 0 && (event.key === 'Enter' || event.key === 'Tab')) {
      event.preventDefault();
      accept(chosen);
    } else if (event.key === 'Escape') {
      if (shown.length > 0) {
        close();
      } else {
        jar.updateOptions({ catchTab: false });
      }
    }
  });
  element.addEventListener('keyup', (event) => {
    if (event.key !== 'Escape' && event.key !== 'Tab') {
      jar.updateOptions({ catchTab: true });
    }
  });
  element.addEventListener('blur', () => {
    jar.updateOptions({ catchTab: true });
    close();
  });
  element.addEventListener('input', (event) => {
    if (event instanceof InputEvent && event.inputType === 'insertText') {
      open(false);
    } else {
      close();
    }
  });

  const jar = CodeJar(
    element,
    (editor: HTMLElement) => {
      editor.innerHTML = highlightJava(editor.textContent ?? '');
    },
    { tab: '    ', indentOn: /[({[]$/, moveToNewLine: /^[)}\]]/ },
  );
  jar.onUpdate(onChange);
  element.style.whiteSpace = 'pre';
  element.style.overflowWrap = 'normal';

  return {
    get value() {
      return jar.toString();
    },
    set value(source: string) {
      jar.updateCode(source);
    },
    selectLine(line: number): void {
      const lines = jar.toString().split('\n');
      const start = lines.slice(0, line - 1).reduce((sum, text) => sum + text.length + 1, 0);
      element.focus();
      jar.restore({ start, end: start + (lines[line - 1]?.length ?? 0) });
    },
  };
}
