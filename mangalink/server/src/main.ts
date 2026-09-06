import 'reflect-metadata';
import { createServer, IncomingMessage } from 'node:http';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { timingSafeEqual } from 'node:crypto';
import { resolvePages } from './reader';
import { Catalog } from './catalog';
import { parserFor, sources, titleUrl } from './sources';

const path = process.env.DATABASE_PATH || './data/catalog.sqlite';
mkdirSync(dirname(path), { recursive: true });
const catalog = new Catalog(path);
const adminToken = process.env.ADMIN_TOKEN;
if (!adminToken || adminToken.length < 24) throw new Error('Задайте ADMIN_TOKEN длиной не менее 24 символов');
let busy = false;
let activeReaders = 0;
function authorized(header: string | undefined) {
  const supplied = Buffer.from(header ?? '');
  const expected = Buffer.from(`Bearer ${adminToken}`);
  return supplied.length === expected.length && timingSafeEqual(supplied, expected);
}
async function body(req: IncomingMessage) {
  let value = '';
  for await (const chunk of req) {
    value += chunk;
    if (Buffer.byteLength(value) > 8192) throw new Error('Слишком большой запрос');
  }
  return JSON.parse(value) as { url?: unknown; titleId?: unknown };
}
async function syncAll() {
  if (busy) return;
  busy = true;
  try {
    for (const row of catalog.bindings()) {
      try {
        const ref = titleUrl(String(row.url));
        catalog.import(ref.url, await parserFor(ref.sourceId).parse(ref.url), String(row.titleId));
      } catch (e) { catalog.recordError(String(row.id), e instanceof Error ? e.message : 'Ошибка обновления'); }
    }
  } finally { busy = false; }
}
const server = createServer(async (req, res) => {
  const send = (code: number, data: unknown) => {
    res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'X-Content-Type-Options': 'nosniff' });
    res.end(JSON.stringify(data));
  };
  try {
    const url = new URL(req.url ?? '/', 'http://localhost');
    if (req.method === 'GET' && url.pathname === '/health') return send(200, { ok: true });
    if (req.method === 'GET' && url.pathname === '/api/sources') return send(200, sources);
    if (req.method === 'GET' && url.pathname === '/api/titles') {
      const limit = Number(url.searchParams.get('limit') ?? 30);
      const offset = Number(url.searchParams.get('offset') ?? 0);
      if (!Number.isInteger(limit) || limit < 1 || limit > 100 || !Number.isInteger(offset) || offset < 0)
        return send(400, { error: 'Некорректная пагинация' });
      return send(200, catalog.list((url.searchParams.get('q') ?? '').slice(0, 200), limit, offset));
    }
    const readerMatch = url.pathname.match(/^\/api\/chapters\/([a-f0-9-]{36})\/pages$/);
    if (req.method === 'GET' && readerMatch) {
      const chapter = catalog.chapter(readerMatch[1]);
      if (!chapter) return send(404, { error: 'Глава не найдена' });
      if (activeReaders >= 8) return send(429, { error: 'Слишком много запросов, повторите позже' });
      activeReaders++;
      try {
        res.setHeader('Cache-Control', 'no-store');
        return send(200, await resolvePages(String(chapter.url)));
      } catch (e) {
        return send(502, { error: e instanceof Error ? e.message : 'Источник недоступен' });
      } finally { activeReaders--; }
    }
    const match = url.pathname.match(/^\/api\/titles\/([a-f0-9-]{36})$/);
    if (req.method === 'GET' && match) {
      const detail = catalog.detail(match[1]);
      return send(detail ? 200 : 404, detail ?? { error: 'Тайтл не найден' });
    }
    if (req.method === 'POST' && ['/api/admin/import', '/api/admin/sync'].includes(url.pathname)) {
      if (!authorized(req.headers.authorization)) return send(401, { error: 'Требуется токен администратора' });
      if (busy) return send(409, { error: 'Обновление уже выполняется' });
      if (url.pathname.endsWith('/sync')) {
        void syncAll();
        return send(202, { status: 'started' });
      }
      const data = await body(req);
      if (typeof data.url !== 'string' || (data.titleId !== undefined && typeof data.titleId !== 'string'))
        return send(400, { error: 'Нужны url и необязательный titleId' });
      const ref = titleUrl(data.url);
      if (data.titleId && !catalog.detail(data.titleId)) return send(404, { error: 'Тайтл не найден' });
      if (busy) return send(409, { error: 'Обновление уже выполняется' });
      busy = true;
      try {
        const result = await parserFor(ref.sourceId).parse(ref.url);
        return send(200, catalog.import(ref.url, result, data.titleId));
      } finally { busy = false; }
    }
    send(404, { error: 'Маршрут не найден' });
  } catch (e) {
    send(400, { error: e instanceof Error ? e.message : 'Ошибка запроса' });
  }
});
const interval = Number(process.env.SYNC_INTERVAL_MINUTES ?? 60);
if (!Number.isFinite(interval) || interval < 1) throw new Error('SYNC_INTERVAL_MINUTES должен быть >= 1');
const timer = setInterval(() => void syncAll(), interval * 60000);
server.listen(Number(process.env.PORT ?? 3100), process.env.HOST ?? '127.0.0.1', () => console.log('MangaLink API ready'));
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => {
  clearInterval(timer);
  server.close(() => process.exit(0));
});
