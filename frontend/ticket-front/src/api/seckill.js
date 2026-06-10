import http from './http';

export const seckillApi = {
  executeSeckill(data) {
    return http.post('/seckill/execute', data);
  },
  warmUpStock(sessionId) {
    return http.post(`/seckill/warmup/${sessionId}`);
  },
  batchWarmUp() {
    return http.post('/seckill/warmup/batch');
  },
  getActiveSessions() {
    return http.get('/seckill/active');
  },
  getUpcomingSessions() {
    return http.get('/seckill/upcoming');
  },
  getAllSessions() {
    return http.get('/seckill/all');
  },
  getSeckillDetail(id) {
    return http.get(`/seckill/detail/${id}`);
  },
  createSeckill(data) {
    return http.post('/seckill/create', data);
  },
  updateSeckill(data) {
    return http.put('/seckill/update', data);
  },
  deleteSeckill(id) {
    return http.delete(`/seckill/delete/${id}`);
  }
};
