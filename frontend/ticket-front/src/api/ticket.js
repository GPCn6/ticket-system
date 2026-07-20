import http from './http';

export const ticketApi = {
  getTicketDetail(id) { return http.get(`/ticket/detail/${id}`); },
  getByShowId(showId) { return http.get(`/ticket/show/${showId}`); },
  createTicket(data) { return http.post('/ticket/create', data); },
  updateTicket(data) { return http.put('/ticket/update', data); },
  deleteTicket(id) { return http.delete(`/ticket/delete/${id}`); }
};
