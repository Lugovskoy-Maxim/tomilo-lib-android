const { test } = require('node:test');
const assert = require('node:assert/strict');
const { Catalog } = require('../dist/catalog');
const { sourceUrl } = require('../dist/sources');
const { MangalibParser } = require('../dist/parsers/mangalib.parser');
const { MangabuffParser } = require('../dist/parsers/mangabuff.parser');
const a = 'https://mangalib.org/ru/manga/1--example';
const b = 'https://mangabuff.ru/manga/example';
const parsed = (host = 'mangalib.org') => ({ title: 'Пример', chapters: [
  { name: 'Глава 1', number: 1, volumeNumber: 1, url: `https://${host}/chapter/1` },
  { name: 'Глава 1, том 2', number: 1, volumeNumber: 2, url: `https://${host}/chapter/2` },
] });
test('multiple sources, repeated sync, chapter IDs and volumes survive', () => {
  const c = new Catalog(':memory:');
  try {
    const first = c.import(a, parsed());
    const id = first.sources[0].chapters[0].id;
    c.import(b, parsed('mangabuff.ru'), first.id);
    const again = c.import(a, parsed());
    assert.equal(c.list().length, 1);
    assert.equal(again.sources.length, 2);
    assert.equal(again.sources.find(s => s.sourceId === 'mangalib').chapters[0].id, id);
    assert.equal(again.sources[0].chapters.length, 2);
    assert.equal(c.list('ПРИМ')[0].id, first.id);
    assert.deepEqual(c.list('', 30, 30), []);
  } finally { c.db.close(); }
});
test('failed/empty results do not destroy previous snapshot', () => {
  const c = new Catalog(':memory:');
  try {
    const first = c.import(a, parsed());
    assert.throws(() => c.import(a, { title: 'Пример', chapters: [] }));
    assert.throws(() => c.import(a, { title: 'Пример', chapters: [{ name: 'bad', url: 'http://127.0.0.1/private' }] }));
    assert.deepEqual(c.detail(first.id), first);
    c.recordError(first.sources[0].id, 'timeout');
    assert.equal(c.detail(first.id).sources[0].lastError, 'timeout');
    c.import(a, parsed());
    assert.equal(c.detail(first.id).sources[0].lastError, null);
  } finally { c.db.close(); }
});
test('explicit linking prevents accidental cross-title merging', () => {
  const c = new Catalog(':memory:');
  try {
    const first = c.import(a, parsed());
    const second = c.import(b, parsed('mangabuff.ru'));
    assert.equal(c.list().length, 2);
    assert.throws(() => c.import(b, parsed('mangabuff.ru'), first.id));
    assert.throws(() => c.import(a, parsed(), second.id));
    assert.equal(c.list().length, 2);
  } finally { c.db.close(); }
});
test('only HTTPS URLs on exact supported domains are accepted', () => {
  for (const url of ['http://mangabuff.ru/a', 'https://mangabuff.ru.evil.org/a', 'https://user:pass@mangabuff.ru/a', 'file:///etc/passwd', 'https://127.0.0.1/a']) {
    assert.throws(() => sourceUrl(url));
  }
  assert.throws(() => sourceUrl(b, 'mangalib'));
});
test('MangaLib parser requests metadata and chapter index only', async () => {
  const parser = new MangalibParser();
  const calls = [];
  parser.session = { get: async url => {
    calls.push(url);
    return { data: { data: url.endsWith('/chapters')
      ? [{ id: 2, volume: '2', number: '1.5', name: 'Bonus' }]
      : { id: 1, slug_url: '1--example', rus_name: 'Пример' } } };
  } };
  const result = await parser.parse(a);
  assert.equal(calls.length, 2);
  assert.equal(result.chapters[0].volumeNumber, 2);
  assert.match(result.chapters[0].url, /read\/v2\/c1.5$/);
  assert.equal(result.chapters[0].pages, undefined);
});
test('MangaBuff parser extracts a chapter URL from HTML', async () => {
  const parser = new MangabuffParser();
  parser.session = { get: async () => ({ headers: {}, data: `<h1 class="manga__name">Пример</h1>
    <a class="chapters__item" href="/manga/example/1"><span class="chapters__value">Глава 1</span></a>` }) };
  const result = await parser.parse(b);
  assert.equal(result.title, 'Пример');
  assert.equal(result.chapters[0].url, b + '/1');
});

test('reader selects chapter images and resolves relative links without downloading images', () => {
  const { extractMangabuffPages } = require('../dist/reader');
  assert.deepEqual(extractMangabuffPages(`<img src="/ad.jpg"><div class="reader__pages"><div class="reader__item">
    <img data-src="//cdn.example.org/1.jpg"><img src="/chapters/2.jpg"><img src="/chapters/2.jpg">
    </div></div>`, b), ['https://cdn.example.org/1.jpg', 'https://mangabuff.ru/chapters/2.jpg']);
});
test('page resolver rejects unknown hosts before making a request', async () => {
  const { resolvePages } = require('../dist/reader');
  await assert.rejects(resolvePages('https://localhost/private'));
});

test('source mirrors and query strings do not create duplicate titles', () => {
  const c = new Catalog(':memory:');
  try {
    const first = c.import(a, parsed());
    const second = c.import('https://mangalib.me/ru/manga/1--example?section=chapters', parsed());
    assert.equal(first.id, second.id);
    assert.equal(c.list().length, 1);
  } finally { c.db.close(); }
});
