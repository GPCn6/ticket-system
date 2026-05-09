import http from './http';

export const userApi = {
  login(data) {
    return http.post('/user/login', data);
  },
  register(data) {
    return http.post('/user/register', data);
  },
  getUserInfo() {
    return http.get('/user/info');
  },
  updateUser(data) {
    return http.put('/user/update', data);
  },
  resetPassword(data) {
    return http.put('/user/reset-password', data);
  },
  logout() {
    return http.post('/user/logout');
  }
};
