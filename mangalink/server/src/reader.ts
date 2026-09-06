import axios from 'axios';
import * as cheerio from 'cheerio';
import { MangalibParser } from './parsers/mangalib.parser';
import { sourceUrl } from './sources';

export function extractMangabuffPages(html: string, chapterUrl: string): string[] {
  const $ = cheerio.load(html);
  const result: string[] = [];
  $('.reader__pages .reader__item img').each((_, el) => {
    const raw = $(el).attr('data-src') || $(el).attr('src');
    if (raw) result.push(new URL(raw, chapterUrl).href);
  });
  return [...new Set(result)];
}
export async function resolvePages(chapterUrl: string) {
  const ref = sourceUrl(chapterUrl);
  const session = axios.create({ timeout: 30000, maxRedirects: 0, maxContentLength: 10 * 1024 * 1024,
    headers: { 'User-Agent': MangalibParser.apiHeaders()['User-Agent'] } });
  let pages: string[];
  if (ref.sourceId === 'mangalib') {
    const chapter = MangalibParser.extractChapterRefFromUrl(ref.url);
    if (!chapter) throw new Error('Не удалось разобрать ссылку главы');
    pages = (await MangalibParser.fetchChapterPages(session, chapter.slugUrl, `${chapter.volume}/${chapter.number}`)).imageUrls;
  } else {
    const response = await session.get(ref.url);
    pages = extractMangabuffPages(String(response.data), ref.url);
  }
  if (!pages.length) throw new Error('Источник не вернул страницы для встроенной читалки');
  for (const page of pages) {
    const u = new URL(page);
    if (u.protocol !== 'https:' || u.username || u.password || u.port ||
        u.hostname === 'localhost' || u.hostname.endsWith('.local') || u.hostname.endsWith('.localhost') ||
        /^[\d.]+$/.test(u.hostname) || u.hostname.includes(':')) throw new Error('Недопустимый адрес страницы');
  }
  // Page URLs live only in this response, never in SQLite or a server disk cache.
  return { pages, headers: { Referer: new URL(ref.url).origin + '/', 'User-Agent': MangalibParser.apiHeaders()['User-Agent'] } };
}
