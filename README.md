# 🎫 高并发票务系统 (Ticket System)

> 基于 Spring Cloud 微服务架构的高并发票务系统，支持秒杀抢购、演出购票等核心功能。

## 📋 项目概述

本项目是一个面向高并发场景的在线票务系统，采用微服务架构设计，核心目标是在高并发秒杀场景下保证数据一致性和系统可用性。

### 核心业务场景

| 场景 | 说明 | 并发特点 |
|------|------|----------|
| **普通购票** | 用户浏览演出 → 选择票档 → 创建订单 → 支付 | 一般并发 |
| **秒杀抢购** | 定时开抢限量特价票 → 用户抢购 → 系统承受高并发 | **高并发** |

---

## 🏗️ 系统架构

### 微服务模块划分

```
ticket-system/
├── ticket-gateway (8080)    ← 网关层：统一入口、JWT认证、路由转发、CORS
├── ticket-user (8081)       ← 用户服务：登录/注册/JWT Token管理
├── ticket-biz (8082)        ← 业务服务：演出/票档/订单/秒杀核心业务
├── ticket-common            ← 公共模块：实体类、工具类、配置、异常处理
└── frontend/                ← 前端：Vue3 + Vite
```

### 技术栈

| 技术 | 用途 |
|------|------|
| **Spring Boot 3.2.1** | 微服务基础框架 |
| **Spring Cloud Gateway** | API 网关（路由转发、认证过滤） |
| **MyBatis-Plus 3.5.5** | ORM 框架（分页、自动填充、乐观锁） |
| **MySQL** | 关系型数据库 |
| **Redis** | 缓存（秒杀库存、分布式锁、限购计数） |
| **RabbitMQ** | 消息队列（异步订单创建、库存同步） |
| **Redisson** | Redis 分布式锁框架 |
| **JWT (jjwt 0.12.3)** | 用户认证 Token |
| **Knife4j (Swagger)** | API 接口文档 |
| **Vue 3 + Vite** | 前端框架 |

### 架构图

```
┌─ 用户浏览器 ─────────────────────────────────────────────┐
│                   Vue3 前端 (localhost:3000)               │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTP
                         ▼
┌─ Spring Cloud Gateway (8080) ────────────────────────────┐
│   AuthFilter：JWT验证 + 路由转发 + CORS                    │
│   白名单：/api/user/login, /api/user/register, GET查询    │
└──────┬─────────────────┬────────────────────────────────┘
       │                 │
       ▼                 ▼
┌── ticket-user:8081 ──┐┌── ticket-biz:8082 ──────────────────┐
│ UserController        ││ ShowController  (演出管理)           │
│   /api/user/*         ││ TicketController (票档管理)          │
│                       ││ OrderController  (订单管理)          │
│ 数据库：sys_user       ││ SeckillController(秒杀管理)          │
│                       ││                                      │
│ JWT Token 生成/验证    ││ 数据库：biz_show / biz_ticket       │
│                       ││        biz_order / biz_seckill_session│
│                       ││                                      │
│                       ││ Redis (秒杀库存、限购、分布式锁)       │
│                       ││ RabbitMQ (异步订单、库存同步)         │
└───────────────────────┘└──────────────────────────────────────┘
```

---

## 🔄 核心业务逻辑

### 1️⃣ 用户认证流程

```
用户注册/登录
   → UserService
      → 用户名/手机号/邮箱唯一性校验（注册）
      → 密码验证（登录）
      → 生成JWT Token（用户ID + 角色role存储在claims中）
      → 返回用户信息 + Token
      
后续请求
   → 前端在 Authorization 头携带 Bearer Token
   → Gateway.AuthFilter 拦截
      → 白名单接口：直接放行
      → 非白名单：验证 Token → 提取 userId, role
      → 注入 X-User-Id, X-User-Role 请求头
      → 转发到后端服务
```

### 2️⃣ 普通购票流程

```
1. 用户浏览演出列表 /api/show/list
2. 查看演出详情 /api/show/detail/{id}
3. 查看票档列表 /api/ticket/show/{showId}
4. 创建订单 /api/order/create
   → OrderServiceImpl.create()
      a. 校验演出状态（必须为ONSALE=1）
      b. 校验票档存在且属于该演出
      c. 检查票档可用库存 ≥ 购买数量
      d. **扣减库存**（乐观锁：UPDATE ... WHERE available_stock >= #{quantity}）
      e. 计算金额：票档单价 × 数量
      f. 生成订单号：ORD + 时间戳 + 6位随机数
      g. 设置状态为"待支付"，过期时间15分钟
      h. 插入数据库
5. 用户支付 /api/order/pay
6. 取消订单 /api/order/cancel/{id}
   → 修改状态为"已取消"
   → 发送MQ消息恢复库存
```

### 3️⃣ 秒杀抢购流程（核心亮点）

```
┌─ 事前准备 ────────────────────────────────────────────────┐
│ 管理员创建秒杀场次 → /api/seckill/create                   │
│ 预热库存 → /api/seckill/warmup/{sessionId}                │
│   → 校验：秒杀库存 ≤ 票档可用库存                          │
│   → 将库存写入Redis：seckill:stock:{sessionId}             │
│   → 标记已预热：seckill:warmup:{sessionId}                 │
└──────────────────────────────────────────────────────────┘
                         │
┌─ 用户抢购 ────────────────────────────────────────────────┐
│ /api/seckill/execute                                      │
│                                                           │
│ SeckillServiceImpl.seckill():                             │
│ ① 校验场次存在且在有效期内                                │
│ ② **自动预热**（Redis无库存key时自动从DB加载）              │
│ ③ **用户级分布式锁**（Lua脚本防重复点击）                  │
│ ④ **Lua脚本原子操作**：                                    │
│    - 检查Redis库存是否充足                                  │
│    - 检查用户限购数量未超限                                 │
│    - 原子扣减Redis库存                                      │
│    - 增加用户已购计数                                       │
│ ⑤ 发送订单创建消息到RabbitMQ                               │
│ ⑥ 返回"抢购成功，正在处理订单"                              │
└──────────────────────────────────────────────────────────┘
                         │
┌─ 异步处理 ────────────────────────────────────────────────┐
│ MessageConsumer.handleOrderCreateMessage():               │
│ ① 生成订单号，设置待支付状态，插入数据库                    │
│ ② 发送库存扣减消息 → 同步到数据库                          │
│                                                           │
│ [若用户取消订单]                                           │
│ MessageConsumer.handleOrderCancelMessage():               │
│ ① 修改订单状态为已取消                                     │
│ ② 发送库存恢复消息 → 恢复票档库存 + Redis库存 + 限购计数   │
└──────────────────────────────────────────────────────────┘
```

### 4️⃣ 订单状态流转

```
                                ┌─────────┐
             创建订单            │ 已完成   │
                 │              └─────────┘
                 ▼                    ▲
           ┌──────────┐               │
           │ 待支付    │───────────────┘
           │ (15分钟)  │    支付成功
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

## 🎯 设计亮点

### 1. 秒杀防超卖：Redis + Lua 脚本

```lua
-- 库存扣减Lua脚本（原子执行）
-- 返回：0=成功，-1=库存不足，-2=参数错误，-3=超过限购
local stockKey = KEYS[1]
local limitKey = KEYS[2]
local quantity = tonumber(ARGV[1])
local userId = ARGV[2]
local limitVal = tonumber(ARGV[3])

-- 扣减库存
local stock = redis.call('DECRBY', stockKey, quantity)
if stock < 0 then
    redis.call('INCRBY', stockKey, quantity)  -- 回滚
    return -1
end

-- 更新限购计数
local bought = redis.call('INCR', limitKey)
if bought > limitVal then
    redis.call('DECRBY', stockKey, quantity)  -- 回滚
    redis.call('DECR', limitKey)
    return -3
end
return 0
```

**为什么这样做？**
- **原子性**：Lua 脚本在 Redis 中串行执行，不存在并发问题
- **高性能**：Redis 纯内存操作，单机 QPS 可达 10万+
- **减少网络IO**：一次脚本执行替代多次 Redis 命令

### 2. 异步削峰：RabbitMQ

- 秒杀成功 → 立即响应前端 → MQ异步创建订单
- 前端不用等待数据库写入，体验流畅
- MQ缓冲请求，避免数据库被瞬间打死

### 3. 分布式锁：防重复抢购

```
seckill:lock:{sessionId}:{userId}  ← Lua实现，10秒过期
```

同一用户在同一秒杀场次重复点击，只有第一次能通过锁。

### 4. 乐观锁：普通库存扣减

```sql
UPDATE biz_ticket 
SET available_stock = available_stock - #{quantity}
WHERE id = #{ticketId} AND available_stock >= #{quantity}
```

利用 `WHERE available_stock >= #{quantity}` 条件实现乐观锁，防止普通下单超卖。

### 5. 自动预热

如果Redis中还没有秒杀库存数据，用户第一次抢购时会自动从数据库加载并预热，无需管理员手动操作。

---

## 📦 项目模块详解

### ticket-gateway（网关服务 - 8080）

| 文件 | 功能 |
|------|------|
| `AuthFilter.java` | JWT 全局认证过滤器：解析Token、注入用户信息、权限校验 |
| `application.yml` | 路由配置：user-service、biz-service、前端静态资源代理 |

### ticket-user（用户服务 - 8081）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/login` | POST | 用户登录，返回JWT Token |
| `/api/user/register` | POST | 用户注册，自动登录 |
| `/api/user/info` | GET | 获取当前用户信息 |
| `/api/user/update` | PUT | 更新用户信息 |
| `/api/user/reset-password` | PUT | 重置密码 |
| `/api/user/logout` | POST | 用户登出 |

### ticket-biz（业务服务 - 8082）

**演出管理：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/show/list` | GET | 分页列表（支持name/category/city/status筛选） |
| `/api/show/detail/{id}` | GET | 演出详情 |
| `/api/show/category/{category}` | GET | 按分类查询 |
| `/api/show/hot` | GET | 热门演出 |
| `/api/show/search` | GET | 搜索（name/venue/description） |
| `/api/show/create` | POST | 创建演出（管理员） |
| `/api/show/update` | PUT | 更新演出（管理员） |
| `/api/show/delete/{id}` | DELETE | 删除演出（管理员） |

**票档管理：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ticket/detail/{id}` | GET | 票档详情 |
| `/api/ticket/show/{showId}` | GET | 演出所有票档 |
| `/api/ticket/create` | POST | 创建票档（管理员） |
| `/api/ticket/update` | PUT | 更新票档（管理员） |
| `/api/ticket/delete/{id}` | DELETE | 删除票档（管理员） |

**订单管理：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/order/create` | POST | 创建订单 → 扣库存 |
| `/api/order/detail/{id}` | GET | 订单详情 |
| `/api/order/list` | GET | 用户订单列表（分页） |
| `/api/order/admin/list` | GET | 管理员查询所有订单 |
| `/api/order/cancel/{id}` | POST | 取消订单 → 恢复库存（MQ） |
| `/api/order/pay` | POST | 支付订单 |

**秒杀管理：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/seckill/execute` | POST | **执行抢购**（Redis+Lua+MQ） |
| `/api/seckill/warmup/{sessionId}` | POST | 预热库存到Redis |
| `/api/seckill/active` | GET | 进行中的场次 |
| `/api/seckill/upcoming` | GET | 即将开始的场次 |
| `/api/seckill/all` | GET | 所有场次（管理员） |
| `/api/seckill/detail/{id}` | GET | 场次详情 |
| `/api/seckill/create` | POST | 创建场次（管理员） |
| `/api/seckill/update` | PUT | 更新场次（管理员） |
| `/api/seckill/delete/{id}` | DELETE | 删除场次（管理员） |

---

## 🛠️ 本地开发环境搭建

### 前置要求

- **JDK 17+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **RabbitMQ 3.12+**
- **Node.js 18+**（前端）

### 1. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE ticket_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 建表SQL见项目文档或自行创建（MyBatis-Plus自动建表需配置）
```

### 2. 配置修改

**ticket-user/application.yml：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_system
    username: root
    password: 你的密码
```

**Redis/RabbitMQ 默认连接本地，无需修改。**

### 3. 启动顺序

```bash
# 1. 安装公共模块
cd backend && mvn clean install -DskipTests

# 2. 启动网关（端口8080）
cd ticket-gateway && mvn spring-boot:run

# 3. 启动用户服务（端口8081）
cd ticket-user && mvn spring-boot:run

# 4. 启动业务服务（端口8082）
cd ticket-biz && mvn spring-boot:run

# 5. 启动前端（端口3000）
cd frontend/ticket-front && npm install && npm run dev
```

### 4. 访问

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3000 |
| 接口文档 | http://localhost:8081/doc.html |
| 网关入口 | http://localhost:8080 |

---

## 📁 项目文件结构

```
backend/
├── pom.xml                          # 父POM（版本管理）
├── ticket-common/                   # 公共模块
│   └── src/main/java/com/jsu/common/
│       ├── config/         # RedisConfig, RabbitMQConfig, SwaggerConfig
│       ├── constant/       # OrderStatus, ShowStatus, RedisKey
│       ├── entity/         # User实体
│       ├── exception/      # BaseException, BusinessException, GlobalExceptionHandler
│       ├── result/         # Result, ResultCode, PageResult
│       └── util/           # StringUtils, IdUtils, DateUtils
├── ticket-gateway/                 # 网关（8080）
│   └── src/main/java/com/jsu/
│       ├── TicketGatewayApplication.java
│       ├── filter/         # AuthFilter（JWT认证）
│       └── config/         # GatewayConfig, RedisConfig
├── ticket-user/                    # 用户服务（8081）
│   └── src/main/java/com/jsu/
│       ├── TicketUserApplication.java
│       ├── controller/     # UserController
│       ├── service/        # UserService -> UserServiceImpl
│       ├── mapper/         # UserMapper
│       └── dto/            # LoginRequest, ResetPasswordRequest
└── ticket-biz/                     # 业务服务（8082）
    └── src/main/java/com/jsu/
        ├── TickerBizApplication.java
        ├── controller/     # ShowController, TicketController, OrderController, SeckillController
        ├── service/        # ShowService, TicketService, OrderService, SeckillService
        ├── mapper/         # ShowMapper, TicketMapper, OrderMapper, SeckillSessionMapper
        ├── entity/         # Show, Ticket, Order, SeckillSession, Seat
        ├── mq/             # MessageProducer, MessageConsumer
        └── config/         # MyBatisPlusConfig, MyMetaObjectHandler
```

---

## 🔐 网关认证规则

| 接口分类 | 认证要求 | 角色要求 |
|----------|----------|----------|
| `/api/user/login` | 无需认证 | - |
| `/api/user/register` | 无需认证 | - |
| GET `/api/show/**` | 无需认证 | - |
| GET `/api/ticket/**` | 无需认证 | - |
| GET `/api/seckill/active\|upcoming\|detail/**` | 无需认证 | - |
| `/api/order/**` | 需认证 | user/admin |
| `/api/seckill/execute` | 需认证 | user |
| POST/PUT/DELETE `/api/show/**` | 需认证 | admin |
| POST/PUT/DELETE `/api/ticket/**` | 需认证 | admin |
| POST/PUT/DELETE `/api/seckill/**`（非execute） | 需认证 | admin |

---

## 📝 数据库设计要点

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 用户表 | id, username, password(明文), phone, email, role(user/admin), status |
| `biz_show` | 演出表 | id, name, category, city, venue, status(0下架/1上架) |
| `biz_ticket` | 票档表 | id, show_id, name, price, total_stock, **available_stock**（实时库存） |
| `biz_order` | 订单表 | id, order_no(唯一), user_id, show_id, ticket_id, quantity, total_amount, status, is_seckill, session_id, expire_time |
| `biz_seckill_session` | 秒杀场次表 | id, show_id, ticket_id, start_time, end_time, stock, **seckill_price** |
