import dayjs from 'dayjs';

export function formatDateTime(value, pattern = 'YYYY-MM-DD HH:mm') {
  if (!value) return '';
  return dayjs(value).format(pattern);
}

export function showStatusText(status) {
  const map = { 0: '下架', 1: '售票中', 2: '进行中' };
  return map[status] ?? '—';
}

export function seckillUiState(session) {
  const now = dayjs();
  const start = dayjs(session.startTime);
  const end = dayjs(session.endTime);
  if (now.isBefore(start)) {
    return { key: 'upcoming', label: '即将开始' };
  }
  if (now.isAfter(end)) {
    return { key: 'ended', label: '已结束' };
  }
  return { key: 'active', label: '进行中' };
}

export function seckillCountdownParts(targetTime) {
  const diff = Math.max(0, dayjs(targetTime).diff(dayjs(), 'second'));
  const days = Math.floor(diff / 86400);
  const hours = Math.floor((diff % 86400) / 3600);
  const minutes = Math.floor((diff % 3600) / 60);
  const seconds = diff % 60;
  const pad = (n) => String(n).padStart(2, '0');
  return { days: pad(days), hours: pad(hours), minutes: pad(minutes), seconds: pad(seconds) };
}
