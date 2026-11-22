import { marked } from 'marked';

const PAGES = [
  { id: 'start', title: 'Getting started' },
  { id: 'packaging', title: 'Packaging' },
  { id: 'compatibility', title: 'Compatibility' },
  { id: 'architecture', title: 'Architecture' },
  { id: 'api', title: 'API reference' },
] as const;

const SOURCES = import.meta.glob<string>('../docs/*.md', {
  query: '?raw',
  import: 'default',
  eager: true,
});

function slug(text: string): string {
  return text
    .toLowerCase()
    .replace(/<[^>]+>/g, '')
    .replace(/[^\p{L}\p{N}\s-]/gu, '')
    .trim()
    .replace(/\s+/g, '-');
}

function link(page: string, section = ''): string {
  return `?page=${page}${section ? `#${section}` : ''}`;
}

function render(): void {
  const asked = new URLSearchParams(location.search).get('page');
  const page = PAGES.find((each) => each.id === asked) ?? PAGES[0];
  const article = document.getElementById('doc')!;
  article.innerHTML = marked.parse(SOURCES[`../docs/${page.id}.md`] ?? '', { async: false });
  document.title = `${page.title} · Flagon docs`;

  const taken = new Map<string, number>();
  for (const heading of article.querySelectorAll<HTMLElement>('h1, h2, h3, h4')) {
    const base = slug(heading.textContent ?? '');
    const seen = taken.get(base) ?? 0;
    taken.set(base, seen + 1);
    heading.id = seen ? `${base}-${seen}` : base;
  }
  for (const anchor of article.querySelectorAll<HTMLAnchorElement>('a[href]')) {
    const target = anchor.getAttribute('href')!.match(/^([\w-]+)\.md(?:#(.*))?$/);
    if (target) {
      anchor.href = link(target[1], target[2]);
    }
  }
  for (const table of article.querySelectorAll('table')) {
    const frame = document.createElement('div');
    frame.className = 'table';
    table.replaceWith(frame);
    frame.append(table);
  }

  const list = document.getElementById('pages')!;
  for (const each of PAGES) {
    const item = document.createElement('li');
    const anchor = document.createElement('a');
    anchor.href = link(each.id);
    anchor.textContent = each.title;
    item.append(anchor);
    if (each === page) {
      anchor.setAttribute('aria-current', 'page');
      const sections = document.createElement('ul');
      for (const heading of article.querySelectorAll('h2')) {
        const entry = document.createElement('li');
        const jump = document.createElement('a');
        jump.href = `#${heading.id}`;
        jump.textContent = heading.textContent;
        entry.append(jump);
        sections.append(entry);
      }
      item.append(sections);
    }
    list.append(item);
  }

  if (location.hash) {
    document.getElementById(decodeURIComponent(location.hash.slice(1)))?.scrollIntoView();
  }
}

render();
