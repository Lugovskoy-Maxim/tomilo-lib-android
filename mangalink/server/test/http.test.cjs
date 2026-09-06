const { test } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { mkdtempSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { once } = require('node:events');
const net = require('node:net');

test('HTTP API, admin authentication, invalid input, persistent catalog', async () => {
  const directory = mkdtempSync(join(tmpdir(), 'mangalink-test-'));
  const { Catalog } = require('../dist/catalog');
  const database = join(directory, 'catalog.sqlite');
  const c = new Catalog(database);
  const title = c.import('https://mangabuff.ru/manga/test', { title: 'Тест', chapters: [{ name: 'Глава 1', url: 'https://mangabuff.ru/manga/test/1' }] });
  c.db.close();
  const socket = net.createServer();
  socket.listen(0, '127.0.0.1');
  await once(socket, 'listening');
  const port = socket.address().port;
  await new Promise(resolve => socket.close(resolve));
  const token = 'local-test-token-1234567890';
  const child = spawn(process.execPath, ['dist/main.js'], { env: { ...process.env, PORT: String(port), HOST: '127.0.0.1', ADMIN_TOKEN: token, DATABASE_PATH: database }, stdio: ['ignore', 'pipe', 'pipe'] });
  try {
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('Server did not start')), 10000);
      child.stdout.on('data', chunk => { if (chunk.toString().includes('ready')) { clearTimeout(timer); resolve(); } });
      child.once('exit', code => { clearTimeout(timer); reject(new Error(`Server exited: ${code}`)); });
    });
    const request = (path, options) => fetch(`http://127.0.0.1:${port}${path}`, options);
    assert.equal((await request('/health')).status, 200);
    const list = await (await request('/api/titles?q=' + encodeURIComponent('ТЕСТ'))).json();
    assert.equal(list[0].id, title.id);
    const detail = await (await request('/api/titles/' + title.id)).json();
    assert.equal(detail.sources[0].chapters[0].url, 'https://mangabuff.ru/manga/test/1');
    assert.equal((await request('/api/titles?limit=NaN')).status, 400);
    assert.equal((await request('/api/admin/import', { method: 'POST', body: '{}' })).status, 401);
    assert.equal((await request('/api/admin/import', { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify({ url: 'http://localhost/private' }) })).status, 400);
    assert.equal((await request('/api/titles/00000000-0000-0000-0000-000000000000')).status, 404);
    assert.equal((await request('/api/sources')).status, 200);
  } finally {
    child.kill('SIGTERM');
    await once(child, 'exit');
    rmSync(directory, { recursive: true, force: true });
  }
});
