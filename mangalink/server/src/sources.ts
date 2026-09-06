import { MangalibParser } from './parsers/mangalib.parser';
import { MangabuffParser } from './parsers/mangabuff.parser';
import type { MangaParser } from './parsers/base.parser';

export const sources = [
  { id: 'mangalib', name: 'MangaLib', hosts: ['mangalib.org', 'mangalib.me'] },
  { id: 'mangabuff', name: 'MangaBuff', hosts: ['mangabuff.ru'] },
];
export function sourceUrl(raw: string, expectedSource?: string): { url: string; sourceId: string } {
  const url = new URL(raw);
  const source = sources.find(s => s.hosts.includes(url.hostname));
  if (url.protocol !== 'https:' || url.username || url.password || url.port || !source ||
      (expectedSource && source.id !== expectedSource)) throw new Error('Недопустимый источник или URL');
  url.hash = '';
  return { url: url.href, sourceId: source.id };
}
export function parserFor(id: string): MangaParser {
  if (id === 'mangalib') return new MangalibParser();
  if (id === 'mangabuff') return new MangabuffParser();
  throw new Error('Источник не поддерживается');
}

export function titleUrl(raw: string): { url: string; sourceId: string } {
  const ref = sourceUrl(raw);
  const url = new URL(ref.url);
  if (ref.sourceId === 'mangalib') {
    const slug = MangalibParser.extractSlugFromUrl(ref.url);
    if (!slug || slug.includes('/')) throw new Error('Ожидается ссылка на тайтл MangaLib');
    return { url: `https://mangalib.org/ru/manga/${encodeURIComponent(slug)}`, sourceId: ref.sourceId };
  }
  if (!/^\/manga\/[^/]+\/?$/.test(url.pathname)) throw new Error('Ожидается ссылка на тайтл MangaBuff');
  url.search = '';
  url.pathname = url.pathname.replace(/\/$/, '');
  return { url: url.href, sourceId: ref.sourceId };
}
