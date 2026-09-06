import { DatabaseSync } from 'node:sqlite';
import { randomUUID } from 'node:crypto';
import type { ParsedMangaData } from './parsers/base.parser';
import { sourceUrl, sources, titleUrl } from './sources';

type Row = Record<string, string | number | null>;

export class Catalog {
  readonly db: DatabaseSync;
  constructor(path: string) {
    this.db = new DatabaseSync(path);
    this.db.exec(`PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL;
      CREATE TABLE IF NOT EXISTS titles (
        id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL,
        cover_url TEXT, search_text TEXT NOT NULL, alternatives TEXT NOT NULL, genres TEXT NOT NULL, updated_at TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS title_sources (
        id TEXT PRIMARY KEY, title_id TEXT NOT NULL REFERENCES titles(id),
        source_id TEXT NOT NULL, url TEXT NOT NULL UNIQUE, updated_at TEXT NOT NULL,
        last_error TEXT, UNIQUE(title_id, source_id)
      );
      CREATE TABLE IF NOT EXISTS chapters (
        id TEXT PRIMARY KEY, title_source_id TEXT NOT NULL REFERENCES title_sources(id),
        name TEXT NOT NULL, number REAL, volume REAL, url TEXT NOT NULL,
        UNIQUE(title_source_id, url)
      );
      CREATE INDEX IF NOT EXISTS chapters_source ON chapters(title_source_id);
      CREATE INDEX IF NOT EXISTS sources_title ON title_sources(title_id);`);
  }
  list(query = '', limit = 30, offset = 0) {
    return this.db.prepare(`SELECT id, name, cover_url AS coverUrl, updated_at AS updatedAt,
      (SELECT count(*) FROM title_sources s WHERE s.title_id=t.id) AS sourceCount
      FROM titles t WHERE instr(search_text, ?) > 0 ORDER BY updated_at DESC, id LIMIT ? OFFSET ?`)
      .all(query.toLocaleLowerCase(), limit, offset) as Row[];
  }

  detail(id: string) {
    const title = this.db.prepare('SELECT * FROM titles WHERE id=?').get(id) as Row | undefined;
    if (!title) return null;
    const rows = this.db.prepare(`SELECT id, source_id AS sourceId, url, updated_at AS updatedAt,
      last_error AS lastError FROM title_sources WHERE title_id=? ORDER BY source_id`).all(id) as Row[];
    return {
      id: title.id, name: title.name, description: title.description, coverUrl: title.cover_url,
      alternatives: JSON.parse(String(title.alternatives)), genres: JSON.parse(String(title.genres)),
      sources: rows.map(s => ({ ...s, name: sources.find(x => x.id === s.sourceId)?.name,
        chapters: this.db.prepare(`SELECT id, name, number, volume, url FROM chapters
          WHERE title_source_id=? ORDER BY COALESCE(volume, 0), COALESCE(number, 0), url`).all(s.id) })),
    };
  }
  import(rawUrl: string, parsed: ParsedMangaData, targetId?: string) {
    const { url, sourceId } = titleUrl(rawUrl);
    if (!parsed.title?.trim() || parsed.title === rawUrl) throw new Error('Парсер не вернул название тайтла');
    const existing = this.db.prepare('SELECT id, title_id FROM title_sources WHERE url=?').get(url) as Row | undefined;
    if (targetId && !this.detail(targetId)) throw new Error('Тайтл не найден');
    if (existing && targetId && existing.title_id !== targetId) throw new Error('Источник уже привязан к другому тайтлу');
    const titleId = String(existing?.title_id ?? targetId ?? randomUUID());
    const bindingId = String(existing?.id ?? randomUUID());
    const other = this.db.prepare('SELECT id FROM title_sources WHERE title_id=? AND source_id=?').get(titleId, sourceId) as Row | undefined;
    if (other && other.id !== bindingId) throw new Error('Этот источник уже есть у тайтла');
    // Validate the full result before modifying the last successful snapshot.
    const chapters = parsed.chapters.map(ch => {
      if (!ch.url) throw new Error('Парсер вернул главу без ссылки');
      const resolved = sourceUrl(new URL(ch.url, url).href, sourceId).url;
      return { ...ch, url: resolved };
    });
    if (!chapters.length) throw new Error('Источник не вернул главы; предыдущие данные сохранены');
    const now = new Date().toISOString();
    this.db.exec('BEGIN IMMEDIATE');
    try {
      if (!this.detail(titleId)) {
        this.db.prepare('INSERT INTO titles VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run(
          titleId, parsed.title.trim(), parsed.description ?? '', parsed.coverUrl ?? null,
          [parsed.title, ...(parsed.alternativeTitles ?? [])].join(' ').toLocaleLowerCase(),
          JSON.stringify(parsed.alternativeTitles ?? []), JSON.stringify(parsed.genres ?? []), now);
      }
      this.db.prepare(`INSERT INTO title_sources VALUES (?, ?, ?, ?, ?, NULL)
        ON CONFLICT(url) DO UPDATE SET updated_at=excluded.updated_at, last_error=NULL`)
        .run(bindingId, titleId, sourceId, url, now);
      const old = this.db.prepare('SELECT id, url FROM chapters WHERE title_source_id=?').all(bindingId) as Row[];
      const ids = new Map(old.map(c => [String(c.url), String(c.id)]));
      this.db.prepare('DELETE FROM chapters WHERE title_source_id=?').run(bindingId);
      const insert = this.db.prepare('INSERT OR IGNORE INTO chapters VALUES (?, ?, ?, ?, ?, ?)');
      for (const ch of chapters) insert.run(ids.get(ch.url) ?? randomUUID(), bindingId,
        ch.name || 'Глава', Number.isFinite(ch.number) ? ch.number! : null,
        Number.isFinite(ch.volumeNumber) ? ch.volumeNumber! : null, ch.url);
      this.db.prepare('UPDATE titles SET updated_at=? WHERE id=?').run(now, titleId);
      this.db.exec('COMMIT');
    } catch (e) { this.db.exec('ROLLBACK'); throw e; }
    return this.detail(titleId);
  }
  chapter(id: string) {
    return this.db.prepare(`SELECT c.url, s.source_id AS sourceId FROM chapters c
      JOIN title_sources s ON s.id=c.title_source_id WHERE c.id=?`).get(id) as Row | undefined;
  }
  bindings() { return this.db.prepare('SELECT id, url, title_id AS titleId FROM title_sources ORDER BY updated_at').all() as Row[]; }
  recordError(id: string, message: string) {
    this.db.prepare('UPDATE title_sources SET last_error=? WHERE id=?').run(message.slice(0, 500), id);
  }
}
