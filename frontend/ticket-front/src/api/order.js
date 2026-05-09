import http from './http';

export const orderApi = {
  createOrder(data) {
    return http.post('/order/create', data);
  },
  getOrderDetail(id) {
    return http.get(`/order/detail/${id}`);
  },
  getByOrderNo(orderNo) {
    return http.get(`/order/detail/orderNo/${encodeURIComponent(orderNo)}`);
  },
  getOrderList(params) {
    return http.get('/order/list', { params });
  },
  getAdminOrderList(params) {
    return http.get('/order/admin/list', { params });
  },
  cancelOrder(id) {
    return http.post(`/order/cancel/${id}`);
  },
  payOrder(orderNo) {
    return http.post('/order/pay', { orderNo });
  },
  resetAllData() {
    return http.post('/admin/reset');
  }
};