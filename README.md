# 项目进度管理小工具（MVP）

基于 `Vue 3 + TypeScript + Element Plus + ECharts` 与 `Spring Boot 3 + PostgreSQL(JSONB)` 实现的项目进度全景图，覆盖：
- 树形项目结构（project/l1/l2/req）
- 五维进度量化（req/dev/test/milestone/compliance）
- 偏差计算与红黄绿信号灯
- 预警闭环（处置、升级、通知）
- 合规材料台账
- 审计日志
- 周报/月报 CSV 导出
- ECharts 可视化（树图、雷达图、柱状图）
- 节假日工作日计算

## 1. 目录结构

- `backend/` Spring Boot 后端
- `frontend/` Vue 前端
- `docker-compose.yml` 一键启动 postgres + backend + frontend

## 2. 环境要求

- Docker + Docker Compose

本仓库设计为 `docker compose up` 直接运行，不依赖宿主机本地 Maven/Node。

## 3. 一键启动

```bash
docker compose up --build
```

启动后访问：
- 前端：http://localhost:5173
- 后端：http://localhost:8080

## 4. 默认账号（MVP）

登录页可直接选择以下用户：
- `u_leader`（ROLE_LEADER）
- `u_pmo`（ROLE_PMO）
- `u_pm`（ROLE_PM）
- `u_owner`（ROLE_MODULE_OWNER）
- `u_compliance`（ROLE_COMPLIANCE）
- `u_viewer`（ROLE_VIEWER）

后端所有 API 也可使用 Header：
- `X-User-Id: u_leader`

## 5. 偏差口径（示例复现）

默认权重：
- dev 0.35
- test 0.25
- req 0.15
- milestone 0.15
- compliance 0.10

示例（与需求文档一致）：
- `planScore = 1.0*0.15 + 1.0*0.35 + 0.5*0.25 + 0.0*0.15 + 0.0*0.10 = 0.625`
- `actualScore = 1.0*0.15 + 0.9*0.35 + 0.3*0.25 + 0.0*0.15 + 0.0*0.10 = 0.54`
- `deviation = (0.625 - 0.54) / 0.625 = 0.136`
- 命中阈值：`YELLOW`

阈值：
- `deviation <= 0.05` => GREEN
- `0.05 < deviation <= 0.15` => YELLOW
- `deviation > 0.15` => RED
- 若关键合规材料超期：强制 RED

## 6. 核心 API 示例（curl）

### 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"u_leader"}'
```

### 查询树

```bash
curl 'http://localhost:8080/api/nodes/tree' \
  -H 'X-User-Id: u_leader'
```

### 更新节点（权重/计划）

```bash
curl -X PUT 'http://localhost:8080/api/nodes/REQ-001' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: u_pmo' \
  -d '{
    "weights": {"req":0.15,"dev":0.35,"test":0.25,"milestone":0.15,"compliance":0.10},
    "plan": {
      "kpi": {
        "req": {"expectedDone": 1.0},
        "dev": {"expectedDone": 1.0},
        "test": {"expectedDone": 0.5},
        "milestone": {"expectedDone": 0.0},
        "compliance": {"expectedDone": 0.0}
      }
    }
  }'
```

### 手动重算

```bash
curl -X POST 'http://localhost:8080/api/compute/recalc/REQ-001' \
  -H 'X-User-Id: u_pmo'
```

### 预警处置

```bash
curl -X POST 'http://localhost:8080/api/warnings/1/action' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: u_pm' \
  -d '{"status":"IN_PROGRESS","note":"2个工作日内完成纠偏"}'
```

### 合规材料更新

```bash
curl -X PUT 'http://localhost:8080/api/compliance/items/1' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: u_compliance' \
  -d '{
    "nodeId":"REQ-002",
    "docType":"需求评审记录",
    "isKey":true,
    "requiredAt":"2026-02-20",
    "submittedAt":"2026-02-28",
    "status":"SUBMITTED",
    "overdueDays":8,
    "attachmentUrl":"https://example.com/doc/new.pdf"
  }'
```

### 导入/导出 JSON

导入：

```bash
curl -X POST 'http://localhost:8080/api/nodes/import' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: u_pmo' \
  -d '{
    "nodeId":"PJT-IMP-001",
    "nodeType":"project",
    "nodeName":"导入演示项目",
    "owner":{"dept":"信息技术部","userId":"u_pm","person":"李四"},
    "asOfDate":"2026-02-25",
    "weights":{"req":0.15,"dev":0.35,"test":0.25,"milestone":0.15,"compliance":0.10},
    "plan":{"kpi":{}},
    "actual":{"kpi":{}},
    "children":[]
  }'
```

导出：

```bash
curl 'http://localhost:8080/api/nodes/PJT-2026-0001/export' \
  -H 'X-User-Id: u_leader'
```

### 报表导出

```bash
curl -L 'http://localhost:8080/api/reports/weekly.csv' \
  -H 'X-User-Id: u_leader' -o weekly.csv

curl -L 'http://localhost:8080/api/reports/monthly.csv' \
  -H 'X-User-Id: u_leader' -o monthly.csv
```

## 7. 数据库与迁移

Flyway 脚本：
- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_data.sql`
- `backend/src/main/resources/db/migration/V3__add_holidays.sql`（节假日表扩展）

包含：
- users
- nodes
- compliance_items
- warnings
- audit_logs
- user_node_acl
- holidays（节假日表，支持工作日精确计算）
及所需索引。

## 8. 测试

测试文件：
- `backend/src/test/java/com/bank/progress/service/ComputeServiceTest.java`
- `backend/src/test/java/com/bank/progress/service/WarningEscalationSchedulerTest.java`

覆盖：
- 5%/15% 阈值判定
- P=0 处理
- 关键合规超期覆盖判红
- 5 个工作日未回绿升级 RED
- 偏差扩大升级 RED

## 9. 开发计划里程碑（验收项）

- 第 1 周：口径与原型（树/详情/JSON结构/权重）✅
- 第 2 周：全景图 + 计算引擎 + 审计日志 ✅
- 第 3 周：预警中心 + 合规台账 + 报表导出 ✅
- 第 4 周：试点优化 + 验收留痕 ✅

## 10. 新增优化功能

### 10.1 ECharts 可视化增强
- **树图可视化**：全景图页面支持列表/图表双模式切换，使用 ECharts tree 展示项目层级结构，节点按信号灯着色
- **雷达图**：节点详情页新增雷达图 Tab，直观对比计划与实际完成度的五维差异
- **柱状图**：偏差来源 Top3 使用 ECharts 柱状图展示贡献度

### 10.2 节假日工作日计算
- 新增 `holidays` 表存储国家法定节假日
- `BusinessDayUtil` 升级支持节假日查询，精确计算工作日
- 预置 2026 年节假日数据（元旦、春节、清明、劳动节、端午、国庆）
- 支持动态扩展节假日配置

### 10.3 通知系统
- 新增 `NotificationService` 统一管理通知
- MVP 阶段：控制台日志输出（ERROR/WARN 级别）
- 预留 Webhook 配置（`app.notification.webhook-url`）
- 支持三类通知：
  - 预警触发通知（YELLOW/RED）
  - 预警升级通知（YELLOW → RED）
  - 合规材料超期通知

### 10.4 配置项
在 `application.yml` 中新增：
```yaml
app:
  notification:
    enabled: false          # 是否启用 webhook 通知
    webhook-url: ""         # Webhook URL（预留）
```

## 11. 完整功能清单

### 后端（Spring Boot 3）
- ✅ 6 个核心表 + 1 个节假日表
- ✅ Flyway 数据库迁移 + 种子数据
- ✅ RBAC 权限控制（6 种角色）
- ✅ 节点 CRUD + 树查询 + 移动
- ✅ JSON 导入/导出
- ✅ 计算引擎（加权偏差、信号灯、Top3 因子）
- ✅ 父子节点汇总（等权平均）
- ✅ 预警生成与升级（5 工作日/偏差扩大）
- ✅ 合规材料台账
- ✅ 审计日志（操作前后对比）
- ✅ CSV 报表导出（周报/月报）
- ✅ 工作日计算（周末 + 节假日）
- ✅ 通知服务（日志 + Webhook 预留）
- ✅ 单元测试（计算逻辑 + 预警升级）

### 前端（Vue 3 + TypeScript）
- ✅ 登录页（角色选择）
- ✅ 全景图总览
  - 列表模式：el-tree 展示，支持筛选
  - 图表模式：ECharts tree 可视化
  - 节点详情：统计卡片 + 维度进度条/雷达图切换
  - 偏差来源：ECharts 柱状图
  - 阻塞与合规摘要
- ✅ 预警中心（列表 + 处置）
- ✅ 合规材料台账（CRUD）
- ✅ 基线/权重配置（权重和校验）
- ✅ 报表导出（CSV 下载）
- ✅ 响应式布局（Element Plus）

### 部署
- ✅ Docker Compose 一键启动
- ✅ 多阶段构建（Maven + Node）
- ✅ Nginx 反向代理（前端）
- ✅ PostgreSQL 14 持久化

## 12. 技术亮点

1. **JSONB 灵活存储**：plan/actual/computed 使用 PostgreSQL JSONB，支持动态扩展维度
2. **事务内重算**：节点更新后自动触发祖先链重算，保证数据一致性
3. **权限分级**：Leader/PMO 全局视图，Compliance 脱敏查看，Module Owner 子树授权
4. **预警闭环**：自动触发 → SLA 跟踪 → 升级机制 → 处置留痕
5. **工作日精算**：支持节假日表扩展，满足银行合规要求
6. **可视化增强**：ECharts 树图/雷达图/柱状图，多维度展示进度偏差
7. **审计完整**：操作前后 JSON 对比 + 差异摘要，满足审计追溯

## 13. 快速验证

启动后访问 http://localhost:5173，使用 `u_leader` 登录：
1. 全景图：切换到"图表"模式，查看 ECharts 树图，点击节点查看详情
2. 节点详情：切换到"雷达图" Tab，对比计划与实际
3. 预警中心：查看 REQ-001 的 YELLOW 预警，点击"处置"填写纠偏说明
4. 合规台账：查看 REQ-002 的关键材料超期（触发 RED 覆盖规则）
5. 基线配置：选择节点，调整权重（确保和=1），修改 expectedDone，保存并重算
6. 报表导出：下载周报 CSV，验证数据完整性
