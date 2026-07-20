import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';
import test from 'node:test';
import { loadConfigFromFile } from 'vite';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const readSource = (relativePath) => readFile(path.join(projectRoot, relativePath), 'utf8');

async function loadTicketingState() {
  const moduleUrl = pathToFileURL(path.join(projectRoot, 'src/utils/ticketing-state.js')).href;
  try {
    return await import(`${moduleUrl}?test=${Date.now()}`);
  } catch (error) {
    assert.fail(`ticketing-state utilities must exist and load: ${error.message}`);
  }
}

test('development API proxies all use the configurable gateway', async () => {
  const previousTarget = process.env.VITE_DEV_GATEWAY_TARGET;
  process.env.VITE_DEV_GATEWAY_TARGET = 'http://127.0.0.1:19090';

  try {
    const loaded = await loadConfigFromFile(
      { command: 'serve', mode: 'test' },
      path.join(projectRoot, 'vite.config.js')
    );
    const proxy = loaded?.config?.server?.proxy || {};
    assert.ok(proxy['/api'], 'an /api proxy is required');
    assert.ok(Object.keys(proxy).length > 0, 'at least one development proxy is required');
    for (const [route, config] of Object.entries(proxy)) {
      assert.equal(config.target, 'http://127.0.0.1:19090', `${route} must use the gateway override`);
    }
  } finally {
    if (previousTarget === undefined) delete process.env.VITE_DEV_GATEWAY_TARGET;
    else process.env.VITE_DEV_GATEWAY_TARGET = previousTarget;
  }
});

test('order status contract maps refund and completion correctly', async () => {
  const { getOrderStatus } = await loadTicketingState();
  assert.deepEqual(getOrderStatus(3), { text: '已退款', type: 'danger' });
  assert.deepEqual(getOrderStatus(4), { text: '已完成', type: 'success' });
});

test('home time filters select today, the remaining week, and this weekend', async () => {
  const { filterShowsByTime } = await loadTicketingState();
  const now = new Date('2026-07-15T10:00:00+08:00');
  const shows = [
    { id: 'past', startTime: '2026-07-14T20:00:00+08:00' },
    { id: 'today', startTime: '2026-07-15T20:00:00+08:00' },
    { id: 'friday', startTime: '2026-07-17T20:00:00+08:00' },
    { id: 'saturday', startTime: '2026-07-18T20:00:00+08:00' },
    { id: 'sunday', startTime: '2026-07-19T20:00:00+08:00' },
    { id: 'next-week', startTime: '2026-07-20T20:00:00+08:00' },
    { id: 'invalid', startTime: 'not-a-date' }
  ];

  assert.deepEqual(filterShowsByTime(shows, 'today', now).map(({ id }) => id), ['today']);
  assert.deepEqual(filterShowsByTime(shows, 'week', now).map(({ id }) => id), ['today', 'friday', 'saturday', 'sunday']);
  assert.deepEqual(filterShowsByTime(shows, 'weekend', now).map(({ id }) => id), ['saturday', 'sunday']);
});

test('disabled seckill sessions remain closed regardless of time and stock', async () => {
  const { getSeckillSessionState } = await loadTicketingState();
  const now = new Date('2026-07-15T10:00:00+08:00');
  const session = {
    status: 0,
    stock: 20,
    startTime: '2026-07-15T09:00:00+08:00',
    endTime: '2026-07-15T11:00:00+08:00'
  };
  assert.equal(getSeckillSessionState(session, now), 'closed');
  assert.equal(getSeckillSessionState({ ...session, status: 1 }, now), 'active');
});

test('polling lifecycle cannot schedule again after cancellation', async () => {
  const { createPollingLifecycle } = await loadTicketingState();
  const lifecycle = createPollingLifecycle();
  let attempts = 0;
  let release;
  const gate = new Promise((resolve) => { release = resolve; });

  lifecycle.schedule(async () => {
    attempts += 1;
    await gate;
    lifecycle.schedule(() => { attempts += 1; }, 0);
  }, 0);

  await new Promise((resolve) => setTimeout(resolve, 10));
  lifecycle.cancel();
  release();
  await new Promise((resolve) => setTimeout(resolve, 20));

  assert.equal(lifecycle.active, false);
  assert.equal(attempts, 1);
});

test('mobile purchase and navigation contracts expose the missing actions', async () => {
  const [showDetail, header, category] = await Promise.all([
    readSource('src/views/show/ShowDetail.vue'),
    readSource('src/components/Header.vue'),
    readSource('src/views/home/Category.vue')
  ]);

  assert.match(showDetail, /class="mobile-ticket-trigger"/);
  assert.match(showDetail, /role="dialog"/);
  assert.match(showDetail, /class="ticket-sheet-backdrop"/);
  assert.match(header, /class="mobile-auth"/);
  assert.match(header, /v-if="!userStore\.isLoggedIn"/);
  assert.match(header, /to="\/login"/);
  assert.match(header, /to="\/register"/);
  assert.match(header, /to="\/order"/);
  assert.match(header, /to="\/user"/);
  assert.doesNotMatch(category, /tabindex="0"/);
});

test('home and admin pages expose partial failure states instead of masking them', async () => {
  const [home, dashboard] = await Promise.all([
    readSource('src/views/home/Home.vue'),
    readSource('src/views/admin/AdminDashboard.vue')
  ]);

  assert.match(home, /Promise\.allSettled/);
  assert.match(home, /filterShowsByTime/);
  assert.match(dashboard, /refreshError/);
  assert.match(dashboard, /apiState/);
  assert.doesNotMatch(dashboard, /<dd class="status-ok">正常<\/dd>/);
});

test('seckill detail uses lifecycle cancellation and closed-state logic', async () => {
  const source = await readSource('src/views/seckill/SeckillDetail.vue');
  assert.match(source, /getSeckillSessionState/);
  assert.match(source, /createPollingLifecycle/);
  assert.match(source, /pollingLifecycle\.cancel\(\)/);
  assert.match(source, /closed: '场次已关闭'/);
});
