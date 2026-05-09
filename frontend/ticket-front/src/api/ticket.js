import axios from 'axios';
import http from './http';

const bizBaseURL = import.meta.env.VITE_BIZ_BASE_URL || 'http://localhost:8082';

const buildHeaders = () => {
  const headers = {};
  const token = localStorage.getItem('token');
  if (token) headers.Authorization = `Bearer ${token}`;

  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null');
  if (userInfo?.id) headers['X-User-Id'] = userInfo.id;
  return headers;
};

const requestWithFallback = async (primaryRequest, fallbackConfig) => {
  try {
    return await primaryRequest();
  } catch (error) {
    const status = error.response?.status;
    if (status && status !== 404) throw error;

    const response = await axios({
      baseURL: bizBaseURL,
      timeout: 15000,
      headers: buildHeaders(),
      ...fallbackConfig
    });
    return response.data;
  }
};

export const ticketApi = {
  getTicketDetail(id) {
    return requestWithFallback(
      () => http.get(`/ticket/detail/${id}`),
      { method: 'get', url: `/ticket/detail/${id}` }
    );
  },
  getByShowId(showId) {
    return requestWithFallback(
      () => http.get(`/ticket/show/${showId}`),
      { method: 'get', url: `/ticket/show/${showId}` }
    );
  },
  createTicket(data) {
    return requestWithFallback(
      () => http.post('/ticket/create', data),
      { method: 'post', url: '/ticket/create', data }
    );
  },
  updateTicket(data) {
    return requestWithFallback(
      () => http.put('/ticket/update', data),
      { method: 'put', url: '/ticket/update', data }
    );
  },
  deleteTicket(id) {
    return requestWithFallback(
      () => http.delete(`/ticket/delete/${id}`),
      { method: 'delete', url: `/ticket/delete/${id}` }
    );
  }
};