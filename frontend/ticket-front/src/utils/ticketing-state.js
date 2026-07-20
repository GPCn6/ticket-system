const ORDER_STATUS = Object.freeze({
  0: Object.freeze({ text: '待支付', type: 'warning' }),
  1: Object.freeze({ text: '已支付', type: 'success' }),
  2: Object.freeze({ text: '已取消', type: 'info' }),
  3: Object.freeze({ text: '已退款', type: 'danger' }),
  4: Object.freeze({ text: '已完成', type: 'success' })
});

export function getOrderStatus(status) {
  return ORDER_STATUS[Number(status)] || { text: '未知', type: 'info' };
}

function validDate(value) {
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function endOfDay(date) {
  const result = new Date(date);
  result.setHours(23, 59, 59, 999);
  return result;
}

export function filterShowsByTime(shows, range, currentTime = new Date()) {
  const items = Array.isArray(shows) ? shows : [];
  const now = validDate(currentTime);
  if (!now || !['today', 'week', 'weekend'].includes(range)) return items.slice();

  let rangeStart = new Date(now);
  let rangeEnd = endOfDay(now);

  if (range === 'week') {
    const daysUntilSunday = (7 - now.getDay()) % 7;
    rangeEnd.setDate(rangeEnd.getDate() + daysUntilSunday);
  } else if (range === 'weekend') {
    const day = now.getDay();
    if (day !== 0) {
      const daysUntilSaturday = Math.max(0, 6 - day);
      rangeStart = new Date(now);
      rangeStart.setDate(rangeStart.getDate() + daysUntilSaturday);
      rangeStart.setHours(0, 0, 0, 0);
      rangeEnd = endOfDay(rangeStart);
      rangeEnd.setDate(rangeEnd.getDate() + 1);
    }
  }

  return items.filter((show) => {
    const startTime = validDate(show?.startTime);
    return startTime && startTime >= rangeStart && startTime <= rangeEnd;
  });
}

export function getSeckillSessionState(session, currentTime = new Date()) {
  if (Number(session?.status) !== 1) return 'closed';

  const now = validDate(currentTime);
  const start = validDate(session?.startTime);
  const end = validDate(session?.endTime);
  if (!now || !start || !end || end < start) return 'unavailable';
  if (now < start) return 'upcoming';
  if (now > end) return 'ended';
  return Number(session?.stock || 0) > 0 ? 'active' : 'soldout';
}

export function createPollingLifecycle() {
  let active = true;
  let timer = null;

  return {
    get active() {
      return active;
    },
    schedule(task, delay) {
      if (!active) return null;
      if (timer !== null) clearTimeout(timer);
      timer = setTimeout(async () => {
        timer = null;
        if (!active) return;
        await task();
      }, delay);
      return timer;
    },
    cancel() {
      active = false;
      if (timer !== null) {
        clearTimeout(timer);
        timer = null;
      }
    }
  };
}
