# 高并发票务系统 (Ticket System)

> 基于 Spring Boot 微服务架构的高并发在线票务销售系统，支持**普通购票**与**秒杀抢购**两大核心场景，采用 Redis + Lua + RabbitMQ 三位一体应对高并发挑战。

---

## 项目概述

本项目是一个面向高并发场景的在线票务系统，采用微服务架构设计，核心目标是**在秒杀等高并发场景下保证数据一致性与系统可用性**。系统提供完整的演出管理、票档管理、选座购票、秒杀抢购、订单管理等业务功能，前端基于 Vue 3 + Element Plus 构建。

### 核心业务场景

| 场景 | 说明 | 并发特点 | 核心技术 |
|------|------|----------|----------|
| **普通购票** | 用户浏览演出 → 选择票档 → 创建订单 → 支付 | 一般并发 | 乐观锁 + 事务 |
| **秒杀抢购** | 定时开抢限量特价票 → 高并发涌入 | **高并发** | Redis+Lua + MQ削峰 + 最终一致性 |

---

## 系统架构

### 架构总览

```
┌─ 用户浏览器 ────────────────────────────────────────────────┐
│                Vue 3 前端 (localhost:3000)                    │
│    Element Plus + Vue Router + Pinia + Axios                 │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP
                         ▼
┌─ Spring Cloud Gateway (8080) ───────────────────────────────┐
│  AuthFilter：JWT认证/Token黑名单                              │
│  RateLimitFilter：IP限流                                      │
│  路由：/api/user/* → user-service, /api/** → biz-service     │
└──────┬─────────────────┬────────────────────────────────────┘
       │                 │
       ▼                 ▼
┌── ticket-user:8081 ──┐┌── ticket-biz:8082 ─────────────────────┐
│ 用户服务             ││ 核心业务服务                            │
│                      ││                                        │
│  ─ UserController    ││  ─ ShowController   (演出管理)         │
│                      ││  ─ TicketController (票档管理)         │
│  DB: sys_user        ││  ─ OrderController  (订单管理)         │
│                      ││  ─ SeckillController(秒杀管理)         │
│  JWT Token 管理      ││  ─ DataResetController(数据重置)       │
│  BCrypt密码加密       ││                                        │
│                      ││  DB: biz_show / biz_ticket            │
│                      ││       biz_order / biz_seat             │
│                      ││       biz_seckill_session              │
│                      ││                                        │
│                      ││  Redis (秒杀库存/分布式锁/限流/限购)    │
│                      ││  RabbitMQ (异步订单/库存同步)           │
└──────────────────────┘└────────────────────────────────────────┘

┌── ticket-common ────────────────────────────────────────────┐
│ 公共依赖模块                                                │
│  ─ 实体: User                                                │
│  ─ 常量: OrderStatus / ShowStatus / RedisKey                 │
│  ─ 配置: RedisConfig / RabbitMQConfig / SwaggerConfig        │
│  ─ 异常: BusinessException / GlobalExceptionHandler          │
│  ─ 结果: Result<T> / PageResult / ResultCode                 │
│  ─ 工具: StringUtils / DateUtils / IdUtils / IpUtils         │
│  ─ 增强: ResultResponseAdvice (统一响应包装)                  │
└──────────────────────────────────────────────────────────────┘
```

### 微服务模块

| 模块 | 端口 | 职责 | 关键依赖 |
|------|------|------|----------|
| **ticket-gateway** | 8080 | API 网关：JWT认证、路由转发、限流、CORS | Spring Cloud Gateway Reactive |
| **ticket-user** | 8081 | 用户服务：登录/注册/密码管理/Token管理 | MySQL, JWT, BCrypt |
| **ticket-biz** | 8082 | 核心业务：演出/票档/订单/秒杀** | MySQL, Redis, RabbitMQ |
| **ticket-common** | - | 公共依赖：实体/配置/工具/异常/统一响应 | 被所有模块引用 |

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.2.1 | 微服务基础框架 |
| **Spring Cloud Gateway** | 2023.0.0 | API 网关（响应式、路由、过滤） |
| **MyBatis-Plus** | 3.5.5 | ORM 框架（分页、乐观锁、自动填充、代码生成） |
| **MySQL** | 8.0+ | 关系型数据库 |
| **Redis** | 6.0+ | 缓存、分布式锁、秒杀库存、接口限流 |
| **RabbitMQ** | 3.12+ | 消息队列（异步订单创建、库存同步、削峰填谷） |
| **JWT (jjwt)** | 0.12.3 | 用户认证 Token |
| **BCrypt** | - | 密码加密 |
| **Knife4j (Swagger)** | 4.5 | API 接口文档 |
| **Vue 3** | - | 前端框架 |
| **Element Plus** | - | 前端 UI 组件库 |
| **Vite** | - | 前端构建工具 |
| **Axios** | - | HTTP 请求库 |
| **Pinia** | - | 前端状态管理 |

---

## 设计模式分析

### 三层架构 (分层架构)

系统采用经典的三层架构（Controller → Service → Mapper），这是最基本也是最重要的架构模式：

```
Controller (请求接收/参数校验) 
    ↓
Service (业务逻辑编排)
    ↓
Mapper (数据持久化/MyBatis-Plus)
```

> **为什么选择这种架构？** 清晰分离关注点，每一层职责单一，便于维护和测试。这是中小型微服务项目的标准实践。

### 模板方法模式 (Template Method)

**应用位置**：`OrderService.cancel()` → `OrderCancelScheduler`

业务服务定义订单取消的核心流程骨架（校验状态 → 更新 → 恢复库存），而定时任务和用户主动取消共享同一套 `cancel()` 实现。

### 工厂方法模式 + 策略模式

**应用位置**：Lua 脚本工厂 (`RedisConfig`)

`RedisConfig` 中定义多个 `@Bean` 方法，各自创建不同的 Lua 脚本实例，每个脚本实现一种原子操作策略：

```java
@Bean
public DefaultRedisScript<Long> decreaseStockScript() { /* 扣减库存策略 */ }
@Bean
public DefaultRedisScript<Long> restoreStockScript() { /* 恢复库存策略 */ }
@Bean
public DefaultRedisScript<Long> seckillLockScript() { /* 分布式加锁策略 */ }
@Bean
public DefaultRedisScript<Long> seckillUnlockScript() { /* 释放锁策略 */ }
@Bean
public DefaultRedisScript<Long> rateLimitScript() { /* 限流策略 */ }
```

> **为什么这样设计？** 将不同的 Redis 原子操作分离为独立的 Bean，便于单元测试和按需注入。同时 Lua 脚本在 Redis 服务端执行，保证了操作的原子性。

### 观察者模式 (事件驱动)

**应用位置**：`MessageProducer` + `MessageConsumer` (RabbitMQ)

秒杀成功后不立即写数据库，而是通过 RabbitMQ 发送消息：
- `order.create` → 异步创建订单（削峰填谷）
- `stock.deduct` → 异步同步库存到 DB
- `order.cancel` / `stock.restore` → 异步恢复库存

```
秒杀请求 → Redis扣库存 → MQ发送 → 立即返回
                               ↓
                         异步消费队列 → 创建订单 → 同步库存到DB
```

> **为什么选择事件驱动？** 将同步写操作转换为异步处理，MQ 作为缓冲区，避免高并发下数据库被打穿。

### 适配器模式 (消息转换)

**应用位置**：RabbitMQ Jackson2JsonMessageConverter

RabbitMQ 发送消息时默认使用 Java 序列化（不跨语言、性能差），系统通过 `Jackson2JsonMessageConverter` 将实体对象自动适配为 JSON 格式传输。

### 统一响应/统一异常处理

**应用位置**：
- `Result<T>` → 统一返回格式（code + message + data）
- `ResultResponseAdvice` → 全局自动包装 `Controller` 返回值
- `GlobalExceptionHandler` → 统一异常捕获与处理

```
所有 Controller 返回值自动被包装为：
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "success": true,
  "fail": false
}
```

> **为什么这样设计？** 前端无需为每个接口编写不同的响应处理逻辑；全局异常处理器避免了业务代码中大量的 try-catch。

### 建造者模式 (配置构建)

**应用位置**：`RabbitMQConfig` 中的 `durableQueue()`

```java
private Queue durableQueue(String name, String routingKey) {
    return QueueBuilder.durable(name)
            .deadLetterExchange(DLX_EXCHANGE)
            .deadLetterRoutingKey(routingKey + ".dead")
            .build();
}
```

> 通过 QueueBuilder 逐步配置队列属性（持久化、死信交换机、路由键），清晰构建复杂对象。

### 依赖注入（控制反转 IoC）

**应用位置**：全程使用 `@RequiredArgsConstructor` + `final` 字段实现构造器注入

```java
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {
    private final SeckillSessionMapper seckillSessionMapper;
    private final TicketService ticketService;
    // ... 自动注入
}
```

### 数据访问对象模式 (DAO)

**应用位置**：`XxxMapper` 接口（UserMapper, ShowMapper, TicketMapper, OrderMapper, SeckillSessionMapper）

每个 Mapper 接口对应一个实体类，封装数据库 CRUD 操作。配合 MyBatis-Plus 的 BaseMapper 提供通用方法，同时支持自定义 SQL 实现复杂查询。

---

## 核心业务流程

### 1. 用户认证流程

```
用户注册 /api/user/register
  → 验证用户名/手机/邮箱唯一性
  → BCrypt 加密密码
  → 插入数据库
  → 生成 JWT Token (含 tokenId, userId, role)
  → 返回用户信息 + Token

用户登录 /api/user/login
  → 用户名查找用户
  → BCrypt 密码校验
  → 生成 JWT Token
  → 返回用户信息 + Token

后续请求
  → 前端携带 Authorization: Bearer <Token>
  → Gateway.AuthFilter 拦截
     → 白名单接口放行（登录/注册/GET查询）
     → 验证 JWT 签名 + 有效期
     → 校验 Token 黑名单（Redis）
     → 提取 userId/role 注入请求头
     → 管理接口校验角色（admin 权限）
  → 转发到后端服务
```

### 2. 普通购票流程

```
用户下单 /api/order/create
  ─ OrderServiceImpl.create()
  ① 校验演出状态（必须为 ONSALE=1）
  ② 校验票档存在且属于该演出
  ③ 检查票档可用库存 ≥ 购买数量
  ④ 扣减库存（乐观锁：WHERE available_stock >= #{quantity}）
  ⑤ 计算订单金额：单价 × 数量
  ⑥ 生成订单号：ORD + 时间戳 + 6位随机数
  ⑦ 设置状态为"待支付"(0)，过期时间15分钟
  ⑧ 插入数据库
```

### 3. 秒杀抢购流程（核心亮点）

```
┌─ 事前准备 ────────────────────────────────────────────────────┐
│ 管理员创建秒杀场次 → warmUp → 库存预热到 Redis                │
│ seckill:stock:{sessionId} = 库存数量                          │
│ seckill:warmup:{sessionId} = 1 (预热标记)                     │
└──────────────────────────────────────────────────────────────┘
                         │
┌─ 用户抢购 (高并发入口) ─────────────────────────────────────┐
│ SeckillServiceImpl.seckill()  — 全程无 DB 写操作！          │
│                                                             │
│ ① 校验场次状态（时间范围 / 库存预热）                        │
│ ② 分布式锁 seckill:lock:{sessionId}:{userId}               │
│    (Lua SET NX PX 10s，防重复点击)                          │
│ ③ Lua脚本原子操作：                                         │
│    - DECRBY 扣减 Redis 库存                                 │
│    - 检查剩余库存 ≥ 0（否则自动回滚）                        │
│    - INCR 增加用户限购计数                                   │
│    - 检查限购未超限（否则回滚库存和计数）                    │
│ ④ MQ 发送 order.create 消息 (异步)                          │
│ ⑤ 立即返回"抢购成功，正在处理订单"                           │
└──────────────────────────────────────────────────────────────┘
                         │
┌─ 异步处理 (MQ消费者) ───────────────────────────────────────┐
│ MessageConsumer.handleOrderCreateMessage()                  │
│ ① 创建订单(DB)：orderNo = Producer侧生成的唯一订单号        │
│ ② MQ 发送 stock.deduct 消息                                │
│                                                             │
│ MessageConsumer.handleStockDeductMessage()                  │
│ ① 乐观锁扣减 biz_ticket.available_stock                    │
│ ② 乐观锁扣减 biz_seckill_session.stock                     │
└──────────────────────────────────────────────────────────────┘
```

### 4. 订单取消/超时取消

```
用户主动取消 /api/order/cancel/{id}
  → OrderServiceImpl.cancel() 同步恢复票档库存

定时任务 (每60秒)
  OrderCancelScheduler.cancelExpiredOrders()
  → 扫描 expire_time < NOW() 的待支付订单
  → 批量调用 OrderServiceImpl.cancel() 恢复库存
```

### 5. 订单状态流转

```
                           ┌──────┐
       创建订单             │ 已完成 │
           │               └──────┘
           ▼                    ▲
     ┌──────────┐               │
     │  待支付    │──────────────┘
     │ (15分钟)  │   支付成功
     └────┬─────┘
        │     │
    取消/过期  支付
        │     │
        ▼     ▼
   ┌────────┐ ┌────────┐
   │ 已取消  │ │ 已支付  │
   └────────┘ └────┬───┘
                    │ 退款
                    ▼
              ┌────────┐
              │ 已退款  │
              └────────┘
```

---

## 高并发设计亮点

### 1. 秒杀防超卖：Redis + Lua 脚本

所有库存操作（扣减、恢复、限购检查）都在 **Redis 服务端原子执行**，无需加锁。

```lua
-- decreaseStockScript (原子执行)
local quantity = tonumber(ARGV[1])
local user_id = ARGV[2]
local max_per_user = tonumber(ARGV[3])

-- 限购校验
local user_bought = tonumber(redis.call('GET', KEYS[2]) or '0')
if user_bought + quantity > max_per_user then return -3 end

-- 库存检查 + 扣减
local current_stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if current_stock < quantity then return -1 end

local remaining = redis.call('DECRBY', KEYS[1], quantity)
if remaining < 0 then
    redis.call('INCRBY', KEYS[1], quantity)  -- 回滚
    return -1
end

-- 更新限购计数
redis.call('INCRBY', KEYS[2], quantity)
redis.call('EXPIRE', KEYS[2], 86400)
return remaining
```

**为什么这样做？**
- **原子性**：Lua 脚本在 Redis 中串行执行，无并发隐患
- **高性能**：纯内存操作，单机 QPS 可达 10 万+
- **一次网络往返**：替代多次 Redis 命令调用

### 2. 异步削峰：RabbitMQ

```
高并发请求到来 → Redis 扛住 → MQ 缓冲 → DB 慢慢消费
```

秒杀接口只做 Redis 操作（微秒级），将 DB 写入操作通过 MQ 异步处理，前端无需等待数据库写入即获得响应。

**MQ 架构**：

```
ticket.exchange (Topic)
  ├─ order.create  → order.create.queue  + order.create.dlq (死信)
  ├─ order.cancel  → order.cancel.queue  + order.cancel.dlq
  ├─ stock.deduct  → stock.deduct.queue  + stock.deduct.dlq
  └─ stock.restore → stock.restore.queue + stock.restore.dlq
```

**死信队列**：消费失败的消息自动路由到 DLQ，防止消息丢失，方便排查。

### 3. 分布式锁：防重复抢购

```
seckill:lock:{sessionId}:{userId}  ← Lua SET NX PX 10000
```

同一用户同一场次重复点击，只有第一次能通过锁。10 秒自动过期，防止死锁。

### 4. 乐观锁：普通库存扣减

```sql
UPDATE biz_ticket 
SET available_stock = available_stock - #{quantity}
WHERE id = #{ticketId} AND available_stock >= #{quantity}
```

利用 `WHERE available_stock >= #{quantity}` 实现乐观锁，无额外锁开销，适合中等并发场景。

### 5. 库存预热机制

管理员创建秒杀场次后，需**手动预热**库存到 Redis：

```
POST /api/seckill/warmup/{sessionId}
→ 预热库存写入 Redis
→ 设置预热标记 seckill:warmup:{sessionId}
→ 预热完成后，用户才可抢购（未预热返回库存不足）
```

> 预热是"事前动作"，不在秒杀热路径中执行，避免自动预热带来的性能损耗。

### 6. 网关限流

Gateway 的 `RateLimitFilter` 基于 Redis 实现 IP 级限流（默认 100 次/60 秒），防止恶意刷接口。

### 7. Token 黑名单机制

用户登出时，JWT 的 `tokenId` 被加入 Redis 黑名单（TTL 与 Token 剩余有效期一致），实现**登出即失效**，无需等待 Token 自然过期。

---

## 数据库设计

### 库

```sql
CREATE DATABASE ticket_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 用户表 | id, username, **password**(BCrypt), phone, email, role(user/admin), status |
| `biz_show` | 演出表 | id, name, poster, description, venue, city, category, start_time, end_time, status(0下架/1上架/2进行中) |
| `biz_ticket` | 票档表 | id, show_id, name, price, total_stock, **available_stock**(实时库存, 乐观锁) |
| `biz_order` | 订单表 | id, order_no(唯一), user_id, show_id, ticket_id, quantity, total_amount, status, expire_time, is_seckill, session_id |
| `biz_seckill_session` | 秒杀场次表 | id, show_id, ticket_id, seckill_price, start_time, end_time, **stock**, status |
| `biz_seat` | 座位表 | id, ticket_id, seat_no, status(0可用/1锁定/2已售) |

### 数据库自动维护

`DatabaseInitializer` 在应用启动时自动执行：
- 检查缺失字段并添加（如 `is_seckill`, `session_id`, `pay_time`）
- 将明文密码自动迁移为 BCrypt 密文
- 回填 `create_time` 和 `is_seckill` 默认值

---

## API 接口一览

### 用户服务 (ticket-user, 8081)

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/user/login` | POST | 登录（返回JWT Token） | 无需 |
| `/api/user/register` | POST | 注册（BCrypt加密密码） | 无需 |
| `/api/user/info` | GET | 当前用户信息 | 需认证 |
| `/api/user/logout` | POST | 登出（Token加入黑名单） | 需认证 |
| `/api/user/update` | PUT | 更新用户信息 | 需认证 |
| `/api/user/reset-password` | PUT | 重置密码 | 需认证 |

### 业务服务 (ticket-biz, 8082)

**演出管理：**

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/show/list` | GET | 分页列表（筛选） | 公开 |
| `/api/show/detail/{id}` | GET | 演出详情 | 公开 |
| `/api/show/hot` | GET | 热门演出 | 公开 |
| `/api/show/search` | GET | 搜索演出 | 公开 |
| `/api/show/create` | POST | 创建演出 | admin |
| `/api/show/update` | PUT | 更新演出 | admin |
| `/api/show/delete/{id}` | DELETE | 删除演出 | admin |

**票档管理：**

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/ticket/detail/{id}` | GET | 票档详情 | 公开 |
| `/api/ticket/show/{showId}` | GET | 演出所有票档 | 公开 |
| `/api/ticket/create` | POST | 创建票档 | admin |
| `/api/ticket/update` | PUT | 更新票档 | admin |
| `/api/ticket/delete/{id}` | DELETE | 删除票档 | admin |

**订单管理：**

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/order/create` | POST | 创建订单（扣库存+乐观锁） | 需认证 |
| `/api/order/detail/{id}` | GET | 订单详情 | 需认证 |
| `/api/order/list` | GET | 用户订单列表 | 需认证 |
| `/api/order/admin/list` | GET | 管理所有订单 | admin |
| `/api/order/cancel/{id}` | POST | 取消订单（恢复库存） | 需认证 |
| `/api/order/pay` | POST | 支付订单 | 需认证 |

**秒杀管理：**

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/seckill/execute` | POST | **执行抢购**（Redis+Lua+MQ） | 需认证 |
| `/api/seckill/warmup/{sessionId}` | POST | 预热库存到Redis | admin |
| `/api/seckill/active` | GET | 进行中场次 | 公开 |
| `/api/seckill/upcoming` | GET | 即将开始场次 | 公开 |
| `/api/seckill/detail/{id}` | GET | 场次详情 | 公开 |
| `/api/seckill/create` | POST | 创建场次 | admin |
| `/api/seckill/update` | PUT | 更新场次 | admin |
| `/api/seckill/delete/{id}` | DELETE | 删除场次 | admin |

---

## 快速启动

### 前置要求

| 依赖 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| RabbitMQ | 3.12+ |
| Node.js | 18+ |

### 1. 数据库初始化

```sql
CREATE DATABASE ticket_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行 `backend/sql/init.sql` 建表并导入初始数据。

### 2. 配置文件修改

**ticket-user** (`backend/ticket-user/src/main/resources/application.yml`)：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_system
    username: root
    password: 你的密码
```

**ticket-biz** (`backend/ticket-biz/src/main/resources/application.yaml`)：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_system
    username: root
    password: 你的密码
```

> Redis 和 RabbitMQ 默认连接本地 (localhost:6379 / localhost:5672)，如不同需同步修改。

### 3. 编译构建

```bash
cd backend
mvn clean install -DskipTests
```

### 4. 启动顺序

按以下顺序启动各服务（建议分别在不同终端中启动）：

```bash
# 1. 网关服务 (8080)
cd backend/ticket-gateway
mvn spring-boot:run

# 2. 用户服务 (8081)
cd backend/ticket-user
mvn spring-boot:run

# 3. 业务服务 (8082)
cd backend/ticket-biz
mvn spring-boot:run

# 4. 前端 (3000)
cd frontend/ticket-front
npm install
npm run dev
```

### 5. 访问

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3000 |
| 接口文档 (Swagger) | http://localhost:8081/doc.html |
| 网关入口 | http://localhost:8080 |

### 6. 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 管理员 |
| user1 | 123456 | 普通用户 |

---

## 项目文件结构

```
ticket-system/
├── backend/
│   ├── pom.xml                          # 父POM（依赖版本管理）
│   ├── sql/init.sql                     # 数据库初始化脚本
│   ├── ticket-common/                   # 公共模块（jar包）
│   │   └── src/main/java/com/jsu/common/
│   │       ├── config/         # RedisConfig(Lua脚本定义), RabbitMQConfig, SwaggerConfig
│   │       ├── constant/       # OrderStatus, ShowStatus, RedisKey
│   │       ├── entity/         # User
│   │       ├── exception/      # BusinessException, GlobalExceptionHandler
│   │       ├── result/         # Result<T>, PageResult, ResultCode
│   │       ├── util/           # StringUtils, DateUtils, IdUtils
│   │       └── web/            # ResultResponseAdvice（统一响应包装）
│   ├── ticket-gateway/                 # 网关服务 (8080)
│   │   └── src/main/java/com/jsu/
│   │       ├── filter/         # AuthFilter（JWT认证）, RateLimitFilter（限流）
│   │       └── config/         # GatewayConfig（路由）, RedisConfig
│   ├── ticket-user/                    # 用户服务 (8081)
│   │   └── src/main/java/com/jsu/
│   │       ├── controller/     # UserController
│   │       ├── service/        # UserService → UserServiceImpl
│   │       ├── mapper/         # UserMapper
│   │       └── dto/            # LoginRequest, ResetPasswordRequest
│   └── ticket-biz/                     # 业务服务 (8082)
│       └── src/main/java/com/jsu/
│           ├── controller/     # Show, Ticket, Order, Seckill, DataReset
│           ├── service/        # Show, Ticket, Order, Seckill 接口+实现
│           ├── mapper/         # Show, Ticket, Order, SeckillSession Mapper
│           ├── entity/         # Show, Ticket, Order, SeckillSession, Seat
│           ├── dto/            # PayOrderRequest, SeckillExecuteRequest
│           ├── mq/             # producer/MessageProducer, consumer/MessageConsumer
│           ├── task/           # OrderCancelScheduler（定时取消过期订单）
│           └── config/         # DatabaseInitializer, MyBatisPlusConfig, MyMetaObjectHandler
│
└── frontend/
    └── ticket-front/                   # Vue 3 + Element Plus 前端
        ├── src/
        │   ├── views/          # 页面组件
        │   ├── router/         # 路由配置
        │   ├── store/          # Pinia 状态管理
        │   ├── api/            # Axios 接口封装
        │   └── components/     # 通用组件
        └── vite.config.js      # 开发代理配置
```

---

## 网关认证规则

| 接口分类 | 认证要求 | 角色要求 |
|----------|----------|----------|
| `/api/user/login`, `/api/user/register` | 无需认证 | - |
| GET `/api/show/**`, `/api/ticket/**` | 无需认证 | - |
| GET `/api/seckill/active\|upcoming\|detail/**` | 无需认证 | - |
| `/api/order/**` | 需认证 | user/admin |
| `/api/seckill/execute` | 需认证 | user |
| POST/PUT/DELETE 管理接口 | 需认证 | admin |

---

## 环境要求与注意事项

1. **MySQL 连接**：确保 MySQL 服务已启动且 `ticket_system` 数据库已创建
2. **Redis 服务**：秒杀功能依赖 Redis，需确保 Redis 服务可用
3. **RabbitMQ 服务**：异步订单处理依赖 RabbitMQ，需启用并开启管理插件
4. **前端开发代理**：`vite.config.js` 配置了 `/api` 代理到后端网关（8080），开发时无需启动网关，但需确保目标服务端口正确
5. **密码存储**：首次启动时 `DatabaseInitializer` 会自动将明文密码迁移为 BCrypt 密文
6. **秒杀测试**：预热库存后才能抢购，可通过 `warmup/{sessionId}` 接口或 `batchWarmUp` 接口执行
