import http from './http';

export const showApi = {
  getShowList(params) {
    return http.get('/show/list', { params });
  },
  getShowDetail(id) {
    return http.get(`/show/detail/${id}`);
  },
  getByCategory(category) {
    return http.get(`/show/category/${encodeURIComponent(category)}`);
  },
  getHotShows(limit = 10) {
    return http.get('/show/hot', { params: { limit } });
  },
  searchShows(keyword) {
    return http.get('/show/search', { params: { keyword } });
  },
  createShow(data) {
    return http.post('/show/create', data);
  },
  updateShow(data) {
    return http.put('/show/update', data);
  },
  deleteShow(id) {
    return http.delete(`/show/delete/${id}`);
  }
};