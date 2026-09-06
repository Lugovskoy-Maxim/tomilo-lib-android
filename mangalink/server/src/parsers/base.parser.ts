export interface ParsedMangaData {
  title: string;
  alternativeTitles?: string[];
  description?: string;
  coverUrl?: string;
  genres?: string[];
  /** Author(s) - может быть несколько через запятую */
  author?: string;
  /** Artist(s) - может быть несколько через запятую */
  artist?: string;
  /** Теги (отдельно от жанров, если источник различает) */
  tags?: string[];
  /** Год выхода */
  releaseYear?: number;
  /** Тип издания: манга, манхва, комикс и т.д. */
  type?: string;
  /** Статус выпуска на источнике (ongoing/completed и локализованные варианты). */
  publicationStatus?: string;
  /** Возрастное ограничение источника (0–18). */
  ageLimit?: number;
  chapters: ChapterInfo[];
}

export interface ChapterInfo {
  name: string;
  url?: string;
  slug?: string;
  number?: number;
  /** Номер тома на источнике. */
  volumeNumber?: number;
  /** Количество страниц на источнике (если известно), для проверки после загрузки */
  pageCount?: number;
}

export interface MangaParser {
  parse(url: string): Promise<ParsedMangaData>;
}
