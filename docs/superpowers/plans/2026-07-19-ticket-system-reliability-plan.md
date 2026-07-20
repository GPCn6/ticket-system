# Ticket System Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将当前购票系统从“网关 + Redis + RabbitMQ 的原型”提升为具备安全边界、订单/库存一致性、秒杀可靠性和可验证前端契约的方案 B 基线。

**Architecture:** 保留现有 Spring Boot 多模块、Gateway、MySQL、Redis、RabbitMQ 和 Vue 3 技术栈。通过服务端 JWT 校验、白名单 DTO、订单状态 CAS、库存事件幂等、Outbox 可靠发布和统一前端 API 层建立正确性边界；前端先做契约和状态修复，再逐步重构用户端与管理端视觉。

**Tech Stack:** Java 21, Spring Boot 3.2, Spring Cloud Gateway, MyBatis-Plus, MySQL 8, Redis, RabbitMQ, Vue 3, Vite, Pinia, Element Plus.

## Global Constraints

- 不覆盖或回滚用户已有未提交改动。
- 所有生产代码行为变更先补回归测试；配置文件和纯仓库元数据变更可直接验证。
- 客户端不得提交或决定 `userId`、`role`、`status`、订单金额和订单状态。
- 秒杀成功只代表请求进入可靠处理链路，订单状态必须可查询。
- 消息发布和消费必须可重试、可观测、可幂等。
- 网关是外部入口，业务服务仍需校验内部身份，不信任客户端身份头。
- 前端不得直连 8081/8082，不得通过 localStorage 中的用户资料构造身份头。

---

### Task 1: 安全边界与请求 DTO

**Files:**
- Create: `backend/ticket-user/src/main/java/com/jsu/dto/RegisterRequest.java`
- Create: `backend/ticket-user/src/main/java/com/jsu/dto/UpdateUserRequest.java`
- Modify: `backend/ticket-user/src/main/java/com/jsu/controller/UserController.java`
- Modify: `backend/ticket-user/src/main/java/com/jsu/service/Impl/UserServiceImpl.java`
- Modify: `backend/ticket-user/src/main/java/com/jsu/config/UserWebSecurityConfig.java`
- Modify: `backend/ticket-gateway/src/main/java/com/jsu/filter/AuthFilter.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/controller/OrderController.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/controller/SeckillController.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/controller/ShowController.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/controller/TicketController.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/controller/DataResetController.java`
- Test: `backend/ticket-user/src/test/java/com/jsu/service/UserServiceSecurityTest.java`

**Deliverable:** 删除调试接口；注册强制普通用户；普通用户更新使用白名单字段；登录校验启用状态；业务服务校验由网关签名/内部身份注入的用户上下文；所有管理接口服务端再次检查 admin。

### Task 2: 订单金额、状态机与库存释放

**Files:**
- Create: `backend/ticket-biz/src/main/java/com/jsu/dto/CreateOrderRequest.java`
- Create: `backend/ticket-biz/src/main/java/com/jsu/common/OrderStateTransition.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/controller/OrderController.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/service/Impl/OrderServiceImpl.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/mapper/OrderMapper.java`
- Modify: `backend/ticket-biz/src/main/resources/mapper/OrderMapper.xml`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/service/Impl/TicketServiceImpl.java`
- Test: `backend/ticket-biz/src/test/java/com/jsu/service/OrderServiceConsistencyTest.java`

**Deliverable:** 服务端计算价格快照；创建/支付/取消/超时采用条件更新；取消只触发一次库存释放；支付与取消并发下只有一个状态迁移成功；分页和详情只返回授权订单。

### Task 3: 秒杀可靠链路与消息幂等

**Files:**
- Create: `backend/ticket-biz/src/main/java/com/jsu/mq/MessageDeduplicationService.java`
- Create: `backend/ticket-biz/src/main/java/com/jsu/mq/OutboxEvent.java`
- Create: `backend/ticket-biz/src/main/java/com/jsu/mq/OutboxEventMapper.java`
- Create: `backend/ticket-biz/src/main/resources/mapper/OutboxEventMapper.xml`
- Modify: `backend/sql/init.sql`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/service/Impl/SeckillServiceImpl.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/controller/SeckillController.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/mq/producer/MessageProducer.java`
- Modify: `backend/ticket-biz/src/main/java/com/jsu/mq/consumer/MessageConsumer.java`
- Modify: `backend/ticket-biz/src/main/resources/application.yaml`
- Test: `backend/ticket-biz/src/test/java/com/jsu/service/SeckillValidationTest.java`

**Deliverable:** 数量和场次校验在服务端执行；Redis 预扣与可靠事件记录绑定；生产者使用 confirm；消费者按事件 ID 幂等；失败消息进入重试/DLQ；返回 requestId 供前端查询。

### Task 4: 网关、配置与运行边界

**Files:**
- Modify: `backend/ticket-gateway/src/main/resources/application.yml`
- Modify: `backend/ticket-gateway/src/main/java/com/jsu/config/GatewayConfig.java`
- Modify: `backend/ticket-gateway/src/main/java/com/jsu/filter/RateLimitFilter.java`
- Modify: `backend/ticket-common/src/main/java/com/jsu/common/filter/CorsFilter.java`
- Modify: `backend/ticket-common/src/main/java/com/jsu/common/config/WebConfig.java`
- Modify: `backend/ticket-biz/src/main/resources/application.yaml`
- Modify: `backend/ticket-user/src/main/resources/application.yml`
- Create: `.gitignore`

**Deliverable:** 路由单一来源、服务地址环境化、限流 Lua 原子执行、CORS 收紧、默认密钥移除、构建产物和 IDE 文件不再进入版本控制。

### Task 5: 前端 API 契约与主流程修复

**Files:**
- Modify: `frontend/ticket-front/src/api/http.js`
- Modify: `frontend/ticket-front/src/api/ticket.js`
- Modify: `frontend/ticket-front/src/api/order.js`
- Modify: `frontend/ticket-front/src/api/seckill.js`
- Modify: `frontend/ticket-front/src/store/user.js`
- Modify: `frontend/ticket-front/src/router/index.js`
- Modify: `frontend/ticket-front/src/views/show/ShowDetail.vue`
- Modify: `frontend/ticket-front/src/views/seckill/SeckillDetail.vue`
- Modify: `frontend/ticket-front/src/views/admin/TicketManage.vue`
- Modify: `frontend/ticket-front/src/views/home/Search.vue`
- Modify: `frontend/ticket-front/src/views/order/Order.vue`
- Test: `frontend/ticket-front/src/utils/*.test.js`

**Deliverable:** 删除业务服务直连和伪造身份头；防重复提交；秒杀按 requestId 查询自己的订单；修复票档库存初始化、订单状态映射、搜索路由刷新和乱码。

### Task 6: 验证、可观测性与前端视觉重构

**Files:**
- Modify: `backend/*/pom.xml`
- Create: `backend/ticket-common/src/main/java/com/jsu/common/config/ObservabilityConfig.java`
- Create: `backend/ticket-biz/src/test/resources/application-test.yaml`
- Create: `frontend/ticket-front/src/styles/tokens.css`
- Modify: `frontend/ticket-front/src/App.vue`
- Modify: `frontend/ticket-front/src/components/Header.vue`
- Modify: `frontend/ticket-front/src/views/home/Home.vue`
- Modify: `frontend/ticket-front/src/views/show/ShowDetail.vue`
- Modify: `frontend/ticket-front/src/views/order/Order.vue`
- Modify: `frontend/ticket-front/src/views/admin/AdminDashboard.vue`

**Deliverable:** 增加构建、单元、集成和前端检查；统一日志中的 trace/request id；重构用户主流程和管理首页的视觉层，保留现有路由和接口语义。

---

## Verification Matrix

- Backend compile: `mvn -f backend/pom.xml -DskipTests compile`
- Backend tests: `mvn -f backend/pom.xml test`
- Frontend build: `npm --prefix frontend/ticket-front run build`
- Static security scan: search for `login-debug`, direct `8082`, `X-User-Role` client injection, hard-coded secrets.
- Concurrency regression: order cancel/pay CAS tests and Redis Lua validation tests.

