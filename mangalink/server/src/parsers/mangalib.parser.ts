import { Injectable, BadRequestException } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { MangaParser, ParsedMangaData, ChapterInfo } from './base.parser';

const API_BASE = 'https://api.cdnlibs.org/api';
// mangalib.me is geo-blocked by DDoS-Guard in some regions (error 1020).
// The public API accepts the working .org mirror as its web origin.
const SITE_ORIGIN = 'https://mangalib.org';
const DEFAULT_IMAGE_SERVER = 'https://img3.cdnlibs.org';

interface MangalibManga {
  id: number;
  name?: string;
  rus_name?: string;
  eng_name?: string;
  slug?: string;
  slug_url?: string;
  cover?: {
    default?: string;
    md?: string;
    thumbnail?: string;
  };
  type?: { label?: string };
  status?: { label?: string };
  releaseDateString?: string;
}

interface MangalibChapterListItem {
  id: number;
  volume?: string;
  number?: string;
  name?: string;
  index?: number;
}

interface MangalibChapterPage {
  slug?: number;
  url?: string;
  image?: string;
}

interface MangalibChapterDetail {
  id: number;
  volume?: string;
  number?: string;
  name?: string;
  pages?: MangalibChapterPage[];
}

@Injectable()
export class MangalibParser implements MangaParser {
  private session: AxiosInstance;

  constructor() {
    this.session = axios.create({
      maxRedirects: 0,
      maxContentLength: 10 * 1024 * 1024,
      timeout: 30000,
      headers: MangalibParser.apiHeaders(),
    });
  }

  static apiHeaders(): Record<string, string> {
    return {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
      Accept: 'application/json',
      'Accept-Language': 'ru-RU,ru;q=0.9,en;q=0.8',
      'site-id': '1',
      Origin: SITE_ORIGIN,
      Referer: `${SITE_ORIGIN}/`,
    };
  }

  /** slug_url, например 50100--ojaggy */
  static extractSlugFromUrl(url: string): string | null {
    const mangaMatch = url.match(/mangalib\.(?:me|org)\/ru\/manga\/([^/?#]+)/i);
    if (mangaMatch) return decodeURIComponent(mangaMatch[1]);

    const readMatch = url.match(/mangalib\.(?:me|org)\/ru\/([^/?#]+)\/read\//i);
    if (readMatch) return decodeURIComponent(readMatch[1]);

    return null;
  }

  /** Из URL читалки: /read/v4/c102 */
  static extractChapterRefFromUrl(
    url: string,
  ): { slugUrl: string; volume: string; number: string } | null {
    const match = url.match(
      /mangalib\.(?:me|org)\/ru\/([^/?#]+)\/read\/v([^/]+)\/c([^/?#]+)/i,
    );
    if (!match) return null;
    return {
      slugUrl: decodeURIComponent(match[1]),
      volume: match[2],
      number: match[3],
    };
  }

  static buildChapterUrl(
    slugUrl: string,
    volume: string,
    number: string,
  ): string {
    return `${SITE_ORIGIN}/ru/${slugUrl}/read/v${volume}/c${number}`;
  }

  static buildTitleUrl(slugUrl: string): string {
    return `${SITE_ORIGIN}/ru/manga/${slugUrl}?section=chapters`;
  }

  async parse(url: string): Promise<ParsedMangaData> {
    const slugUrl = MangalibParser.extractSlugFromUrl(url);
    if (!slugUrl) {
      throw new BadRequestException(
        'Invalid mangalib.me URL. Expected: https://mangalib.me/ru/manga/{slug} or .../read/v{volume}/c{number}',
      );
    }

    const manga = await this.fetchManga(slugUrl);
    const chapters = await this.fetchChapters(slugUrl);

    const alternativeTitles = [
      manga.rus_name,
      manga.eng_name,
      manga.name,
    ].filter((t): t is string => Boolean(t?.trim()));

    const title =
      manga.rus_name?.trim() ||
      manga.name?.trim() ||
      manga.eng_name?.trim() ||
      slugUrl;

    const uniqueAlts = [
      ...new Set(
        alternativeTitles.filter(
          (t) => t.trim().toLowerCase() !== title.toLowerCase(),
        ),
      ),
    ];

    return {
      title,
      alternativeTitles: uniqueAlts.length > 0 ? uniqueAlts : undefined,
      coverUrl: manga.cover?.default || manga.cover?.md || undefined,
      type: manga.type?.label || undefined,
      publicationStatus: manga.status?.label || undefined,
      releaseYear: MangalibParser.parseReleaseYear(manga.releaseDateString),
      chapters,
    };
  }

  private static parseReleaseYear(value?: string): number | undefined {
    if (!value) return undefined;
    const match = value.match(/(\d{4})/);
    if (!match) return undefined;
    const year = parseInt(match[1], 10);
    return Number.isFinite(year) ? year : undefined;
  }

  private async fetchManga(slugUrl: string): Promise<MangalibManga> {
    const res = await this.session.get<{ data?: MangalibManga }>(
      `${API_BASE}/manga/${encodeURIComponent(slugUrl)}`,
    );
    const manga = res.data?.data;
    if (!manga?.slug_url && !manga?.id) {
      throw new BadRequestException(
        `Manga not found on mangalib.me: ${slugUrl}`,
      );
    }
    return manga;
  }

  private async fetchChapters(slugUrl: string): Promise<ChapterInfo[]> {
    const res = await this.session.get<{ data?: MangalibChapterListItem[] }>(
      `${API_BASE}/manga/${encodeURIComponent(slugUrl)}/chapters`,
    );
    const list = Array.isArray(res.data?.data) ? res.data.data : [];

    return list
      .map((ch) => this.mapChapterItem(slugUrl, ch))
      .filter((ch): ch is ChapterInfo => ch !== null)
      .sort((a, b) => (b.number || 0) - (a.number || 0));
  }

  private mapChapterItem(
    slugUrl: string,
    ch: MangalibChapterListItem,
  ): ChapterInfo | null {
    const volume = (ch.volume ?? '1').toString().trim();
    const numberRaw = (ch.number ?? '').toString().trim();
    if (!numberRaw) return null;

    const number = parseFloat(numberRaw);
    if (!Number.isFinite(number)) return null;

    const name =
      ch.name?.trim() ||
      (volume !== '1'
        ? `Том ${volume} Глава ${numberRaw}`
        : `Глава ${numberRaw}`);

    return {
      name,
      number,
      volumeNumber: Number.isFinite(Number(volume))
        ? Number(volume)
        : undefined,
      slug: `${volume}/${numberRaw}`,
      url: MangalibParser.buildChapterUrl(slugUrl, volume, numberRaw),
    };
  }

  private static async fetchImageServerUrl(
    session: AxiosInstance,
  ): Promise<string> {
    try {
      const res = await session.get<{
        data?: { imageServers?: Array<{ id: string; url: string }> };
      }>(`${API_BASE}/constants`, {
        params: { 'fields[]': 'imageServers' },
      });
      const servers = res.data?.data?.imageServers ?? [];
      const download = servers.find((s) => s.id === 'download');
      const main = servers.find((s) => s.id === 'main');
      return (
        download?.url ||
        main?.url ||
        servers[0]?.url ||
        DEFAULT_IMAGE_SERVER
      ).replace(/\/$/, '');
    } catch {
      return DEFAULT_IMAGE_SERVER;
    }
  }

  private static buildPageUrl(imageServer: string, pagePath: string): string {
    const normalized = pagePath.startsWith('//')
      ? pagePath.slice(2)
      : pagePath.replace(/^\/+/, '');
    return `${imageServer}/${normalized}`;
  }

  /**
   * chapterSlug = "volume/number", например "4/102"
   */
  static async fetchChapterPages(
    session: AxiosInstance,
    slugUrl: string,
    chapterSlug: string,
  ): Promise<{ imageUrls: string[]; pageCount: number }> {
    const [volume, number] = chapterSlug.split('/');
    if (!volume || !number) {
      throw new BadRequestException(
        `Invalid mangalib chapter slug: ${chapterSlug}. Expected volume/number`,
      );
    }

    const res = await session.get<{ data?: MangalibChapterDetail }>(
      `${API_BASE}/manga/${encodeURIComponent(slugUrl)}/chapter`,
      {
        params: { volume, number },
        headers: MangalibParser.apiHeaders(),
      },
    );

    const chapter = res.data?.data;
    const pages = chapter?.pages ?? [];
    if (pages.length === 0) {
      throw new BadRequestException(
        `No pages in mangalib chapter ${slugUrl} v${volume} c${number}`,
      );
    }

    const imageServer = await MangalibParser.fetchImageServerUrl(session);
    const imageUrls = [...pages]
      .sort((a, b) => (a.slug ?? 0) - (b.slug ?? 0))
      .map((p) => p.url)
      .filter((u): u is string => Boolean(u))
      .map((u) => MangalibParser.buildPageUrl(imageServer, u));

    return { imageUrls, pageCount: imageUrls.length };
  }
}
