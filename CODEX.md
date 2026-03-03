```md
# 四川农商银行信息技术部｜项目进度管理小工具（需求全景图）— CLAUDE_CODE.md
**版本**：v1.0（MVP）  
**定位**：管理层能力层（聚合多源数据 → 统一口径量化 → 信号灯展示 → 分级预警闭环 → 合规材料底账）  
**目标**：在强监管语境下，解决“进度信息分散且口径不一、需求变更与里程碑偏差量化不及时、合规材料易补录与超期”的管理痛点，实现“一图全景、一套口径、一键预警、一份底账”。  
**MVP计划**：3–4 周（允许人工填报 + 自动校验；预留对接行内系统/需求/测试/OA接口）。

---

## 0. 你是谁（Claude Code 的角色定义）
你是 **Claude Code**（代码生成与重构代理）。请在一个全新仓库中生成**可运行**的前后端项目代码（含数据库迁移与初始化数据），实现本文全部“必须实现（MUST）”项，并提供一键启动方式。

---

## 1. 业务范围与必须实现清单（MUST）

### 1.1 核心功能：需求全景图
- 以树形结构展示：**项目 → 一级功能（业务域/系统群）→ 二级功能（模块）→ 具体需求（可验收功能点）**  
  - 示例：一级功能“业务处理”对应 N 个二级功能；二级功能“贷款审批”下挂若干需求。
- **层级可灵活调整**：支持新增节点类型（未来可扩展 L3/L4），支持节点移动/重挂接、排序。
- **节点字段统一**：责任人、计划基线、实际数据、计算结果、信号灯、预警记录、合规材料台账。

### 1.2 信号灯与分级预警（必须严格遵循）
按“计划 vs 实际”的**量化偏差**自动给出信号灯，并触发预警闭环：

**信号灯规则：**
- 绿色（GREEN）：项目进展符合预期，**实际进展与计划偏差 ≤ 5%**
- 黄色（YELLOW）：需重点关注，**5% < 偏差 ≤ 15%**，触发**初级预警**（明确预警对象与初步处置建议）
- 红色（RED）：严重滞后，**偏差 > 15%**，触发**高级预警**（明确预警对象、应急处置流程、责任划分）
- **覆盖规则**：若存在**关键合规材料超期**，无论偏差阈值，直接判定 **RED**（必须实现）

**预警闭环（处置与升级）：**
- GREEN：日常跟踪
- YELLOW：**2 个工作日内纠偏**；若**连续 5 个工作日未回绿或偏差扩大** → 自动升级 RED
- RED：**1 个工作日应急处置**；形成处置单并复盘归档（留痕）

> 工作日算法：MVP 简化为周一–周五；预留节假日表扩展。

### 1.3 权限分级查看（必须实现）
- **领导（Leader）**：查看全行/全域全景图 + 红色预警督办
- **PMO / 项目经理（PMO/PM）**：维护基线与权重配置、预警处置闭环
- **模块负责人（Module Owner）**：仅查看本人负责的子树，填报原因分析与纠偏
- **合规/审计联络人（Compliance）**：重点查看合规材料缺失/超期联动（其它维度可脱敏摘要）
- **只读查看者（Viewer）**：仅能查看被授权节点

必须具备：**最小权限控制、按节点授权的数据范围、敏感字段脱敏展示、全程操作审计日志**。

### 1.4 进度刻画维度（必须具备且可扩展）
MVP 至少支持以下五类维度，并允许扩展：
1) **需求管理（req）**：变更申请数、变更分析数、实施变更数、冻结/确认数等  
2) **开发任务（dev）**：计划进度 vs 实际进度完成度、计划功能数、实际完成数、阻塞/返工等  
3) **测试验证（test）**：计划测试进度 vs 实际测试进度、计划用例数 vs 实际执行数、缺陷关闭率等  
4) **里程碑（milestone）**：计划 vs 实际里程碑完成情况、延期天数、按期完成率  
5) **合规审计材料（compliance）**：需求评审记录、测试报告归档等应提交/已提交、超期天数；关键材料超期触发 RED

---

## 2. 统一口径：偏差计算与权重（MUST）

### 2.1 关键原则
- **偏差计算必须结合多维度量化数据**，不能只用单一指标。
- 每个维度 d 计算两项：
  - 计划完成度 `p_d ∈ [0,1]`
  - 实际完成度 `a_d ∈ [0,1]`

### 2.2 默认权重（可配置，且权重之和必须=1）
建议默认：
- dev 0.35
- test 0.25
- req 0.15
- milestone 0.15
- compliance 0.10

支持：按项目/节点覆盖默认权重；校验 `Σw = 1`。

### 2.3 计算公式（严格实现）
对节点：
- `P = Σ(w_d * p_d)`（计划得分）
- `A = Σ(w_d * a_d)`（实际得分）
- `deviation = (P - A) / P`  
  - 若 `P == 0`：`deviation = 0` 且默认 GREEN（除非关键合规超期）

信号灯判定：
- 若关键合规材料超期：`trafficLight = RED`
- 否则：
  - `deviation ≤ 0.05` → GREEN
  - `0.05 < deviation ≤ 0.15` → YELLOW
  - `deviation > 0.15` → RED

### 2.4 偏差来源解释（用于预警中心，必须输出）
必须给出：
- `deviationTopFactors`：按 `w_d * (p_d - a_d)` 贡献度降序取 Top3
- `blockers`：阻塞项列表（MVP 人工录入，预留自动对接）
- `complianceOverdueList`：超期材料清单（含超期天数、是否关键）

---

## 3. 通用 JSON 数据结构（输入/存储/导入导出）— MUST

### 3.1 设计要求
- **每一个项目/需求的数据，用一个通用 JSON 结构存储**（DB 使用 JSONB）。
- `plan.kpi.*` 用 `expectedDone` 表示截止统计日“计划应完成度”
- `actual.kpi.*` 用 `done` 表示实际已完成度
- `computed` 由系统按权重自动生成（P/A/偏差/信号灯/Top因素/合规覆盖）

> 约定：内部数值统一用 0–1；导入时允许兼容 "100%" 这类字符串，入库统一规范化为 1.0。

### 3.2 Canonical Node JSON（必须支持导入/导出）
~~~json
{
  "nodeId": "PJT-2026-0001",
  "nodeType": "project",
  "nodeName": "信贷域-贷款审批优化",
  "owner": { "dept": "信贷研发组", "person": "张三", "userId": "u_zhangsan" },
  "asOfDate": "2026-02-25",
  "weights": { "req": 0.15, "dev": 0.35, "test": 0.25, "milestone": 0.15, "compliance": 0.10 },
  "plan": {
    "kpi": {
      "req": { "expectedDone": 1.0, "confirmedCount": 18, "baselineCount": 20 },
      "dev": { "expectedDone": 1.0, "plannedTasks": 40 },
      "test": { "expectedDone": 0.5, "plannedCases": 200 },
      "milestone": { "expectedDone": 0.0, "plannedMilestones": 3 },
      "compliance": { "expectedDone": 0.0, "requiredDocs": 8 }
    }
  },
  "actual": {
    "kpi": {
      "req": { "done": 1.0, "changeRequested": 6, "changeAnalyzed": 4, "changeImplemented": 2 },
      "dev": { "done": 0.9, "doneTasks": 36, "blockedTasks": 2, "reworkTasks": 1 },
      "test": { "done": 0.3, "executedCases": 60, "defectClosedRate": 0.4 },
      "milestone": { "done": 0.0, "onTimeRate": 0.0, "delayDays": 0 },
      "compliance": { "done": 0.0, "archivedDocs": 0, "overdueDocs": 0 }
    }
  },
  "computed": {
    "planScore": 0.625,
    "actualScore": 0.54,
    "deviation": 0.136,
    "trafficLight": "YELLOW",
    "deviationTopFactors": [
      { "dim": "test", "contrib": 0.05 },
      { "dim": "dev", "contrib": 0.035 },
      { "dim": "compliance", "contrib": 0.0 }
    ],
    "complianceKeyOverdue": false
  },
  "parent": [],
  "children": [
    { "nodeId": "L2-001", "nodeType": "l2", "nodeName": "规则引擎改造", "computed": { "trafficLight": "YELLOW" } }
  ]
}
~~~

### 3.3 示例计算说明（必须在 README 与测试中复现）
- planScore = 1.0\*0.15 + 1.0\*0.35 + 0.5\*0.25 + 0.0\*0.15 + 0.0\*0.10 = **0.625**
- actualScore = 1.0\*0.15 + 0.9\*0.35 + 0.3\*0.25 + 0.0\*0.15 + 0.0\*0.10 = **0.54**
- deviation = (0.625 - 0.54) / 0.625 = **0.136** → YELLOW（5%~15%）

### 3.4 父子节点汇总规则（MUST）
- 叶子节点（req）：用自身 plan/actual 直接计算 computed
- 非叶子节点（project/l1/l2）：
  1) 若自身 plan/actual 被填写：优先用自身（视为“管理口径覆盖”）
  2) 否则：从子节点汇总生成各维度完成度  
     - MVP 规则：对每个维度，用子节点该维度完成度**等权平均**（可扩展为按子节点权重或按工作量加权）
- 任意节点更新后：必须触发该节点及其祖先 computed 重算（事务内完成）。

---

## 4. 参考与借鉴（实现层面可参考但不照搬）
- 可借鉴开源工具（如 OpenProject、Plane）的“任务-里程碑-视图交互”体验
- 但银行落地必须补齐：组织权限、审计口径、合规材料底账、强预警闭环等

---

## 5. 技术栈与工程交付（MUST）

### 5.1 技术栈（按此生成）
- 前端：Vue 3 + TypeScript + Element Plus + ECharts
- 后端：Spring Boot 3.x（单体即可，保留 Spring Cloud 扩展空间）
- 数据库：PostgreSQL 14+（JSONB 存通用结构）
- 部署：Docker Compose（MVP），预留 K8s
- 安全：最小权限、操作审计、敏感字段脱敏与（可扩展）加密存储

### 5.2 一键启动（必须提供）
- `docker compose up` 可启动：postgres + backend + frontend
- README 必须写清：
  - 环境要求、启动步骤、默认账号/角色
  - 关键接口 curl 示例
  - 导入/导出 JSON 示例

---

## 6. 数据库设计（PostgreSQL）— MUST

### 6.1 表（必须创建 Flyway 迁移 + seed）
1) `users`
- `id, username, display_name, dept, roles(jsonb), created_at`

2) `nodes`
- `id(nodeId) PK, node_type, node_name`
- `owner_user_id, owner_dept`
- `parent_id, path(text 如 /PJT.../L1.../L2...), sort_order`
- `as_of_date`
- `weights jsonb, plan jsonb, actual jsonb, computed jsonb`
- `created_at, updated_at`

3) `compliance_items`
- `id, node_id`
- `doc_type, is_key`
- `required_at, submitted_at`
- `status, overdue_days`
- `attachment_url, meta jsonb`

4) `warnings`
- `id, node_id`
- `level(YELLOW/RED), status(OPEN/IN_PROGRESS/RESOLVED/CLOSED)`
- `deviation, reason`
- `deviation_top_factors jsonb`
- `sla_due_at, triggered_at, resolved_at`
- `assignees jsonb, action_log jsonb`

5) `audit_logs`
- `id, user_id, action, entity_type, entity_id`
- `before jsonb, after jsonb`
- `diff_summary text`
- `ip, user_agent, created_at`

6) `user_node_acl`
- `id, user_id, node_id`
- `permission(READ/WRITE/ADMIN), created_at`

索引（必须）：
- nodes(parent_id), nodes(path)
- warnings(status, level)
- compliance_items(node_id, status)
- audit_logs(entity_id, created_at)

---

## 7. 权限模型（RBAC + 数据范围）— MUST

### 7.1 角色枚举（MVP）
- ROLE_LEADER
- ROLE_PMO
- ROLE_PM
- ROLE_MODULE_OWNER
- ROLE_COMPLIANCE
- ROLE_VIEWER

### 7.2 数据范围控制（必须实现）
- Leader/PMO：可看全库
- PM：可看自己负责的项目（owner 或被授权）
- Module Owner：仅可看 “被授权节点 + 全部子孙节点”
- Compliance：可看所有项目的 compliance 明细；其他维度返回脱敏摘要（MVP：只返回必要字段）
- Viewer：只读授权节点

建议实现方式：
- `user_node_acl` 做节点级授权
- 查询树时，从授权根向下展开（递归 CTE 或 path 前缀匹配）

---

## 8. 后端 API（REST）— MUST

### 8.1 Auth（MVP 可 mock，但结构可替换为 SSO）
- POST `/api/auth/login`（支持选择用户或用户名密码；返回 token 或简单 session）
- GET  `/api/auth/me`

MVP 可用 Header：`X-User-Id`（后端校验用户存在与角色）。

### 8.2 Node（项目树）
- GET  `/api/nodes/tree?rootId=...`（按权限返回可见子树）
- GET  `/api/nodes/{id}`
- POST `/api/nodes`（新建节点）
- PUT  `/api/nodes/{id}`（更新：名称/owner/plan/actual/weights/asOfDate）
- POST `/api/nodes/{id}/move`（移动：newParentId, newOrder）
- POST `/api/nodes/import`（导入 JSON）
- GET  `/api/nodes/{id}/export`（导出 JSON）

### 8.3 Compute（重算）
- POST `/api/compute/recalc/{id}`（手动触发：节点及祖先）

### 8.4 Warning（预警中心）
- GET  `/api/warnings`（支持 level/status/rootId/owner 筛选）
- GET  `/api/warnings/{id}`
- POST `/api/warnings/{id}/action`（填写纠偏/应急/复盘；支持附件元数据）

### 8.5 Compliance（合规台账）
- GET  `/api/compliance/items?nodeId=...`
- POST `/api/compliance/items`
- PUT  `/api/compliance/items/{id}`

### 8.6 Report（导出）
- GET `/api/reports/weekly.csv?rootId=...`
- GET `/api/reports/monthly.csv?rootId=...`

> MVP 先实现 CSV；Excel 可预留。

---

## 9. 计算引擎与预警生成（后端 MUST 独立成 Service）

### 9.1 ComputeService.recalcNode(nodeId)（必须实现）
流程（事务内）：
1) 读取节点 + 子节点（必要字段）
2) 若叶子：用自身 plan/actual 计算  
   否则：按“父子汇总规则”生成各维度完成度
3) 检查合规：是否存在关键材料超期 → `complianceKeyOverdue`
4) 计算 P/A/deviation + trafficLight
5) 生成 `deviationTopFactors`（Top3）
6) 更新 nodes.computed
7) 若信号灯进入 YELLOW/RED 或严重度变化：写入/更新 warnings
8) 向上递归重算祖先直到 root

### 9.2 预警升级任务（必须实现）
- 定时任务（@Scheduled 每天一次即可）：
  - 扫描 OPEN 的 YELLOW
  - 若连续 5 个工作日未回绿或偏差扩大（例如 deviation_today > deviation_trigger + 0.02）→ 升级 RED
  - 生成升级记录（action_log）

### 9.3 通知（MVP）
- 站内信（DB 表）或 warnings 里记录通知事件 + 控制台日志
- 预留 webhook 配置
- 红色预警必须抄送 leader 列表（MVP 配置文件写死 leaderUsers）

---

## 10. 操作审计（MUST）
对以下动作必须记录审计：
- nodes：新增/更新/移动/导入
- compliance_items：新增/更新
- warnings：处置动作（action）
审计字段必须包含：操作者、时间、对象、前后 JSON、差异摘要 diff_summary、ip、user_agent。

---

## 11. 前端页面与交互（MUST）

### 11.1 页面清单
1) 登录页（MVP：选择用户/角色登录）
2) **全景图总览**
   - 左侧树：按 trafficLight 着色（绿/黄/红）
   - 右侧详情卡片：计划得分/实际得分/偏差、维度进度条、Top 因子、阻塞与合规摘要
   - 筛选：按 trafficLight、owner、关键字搜索
3) **预警中心**
   - 列表：项目/节点/级别/偏差/触发时间/SLA/状态/责任人
   - 详情：偏差来源 Top3、阻塞项、超期材料、处置记录、复盘附件
4) **合规材料台账**
   - 清单：材料类型、是否关键、应提交/已提交、到期日、超期天数、附件链接
   - 操作：标记提交、更新附件链接
5) **基线/权重配置**（PMO/PM）
   - 维度权重编辑（校验和=1）
   - 计划完成度 expectedDone 维护（按 asOfDate）
6) **报表导出**
   - 一键下载 weekly/monthly CSV

### 11.2 UI 规范
- 使用 Element Plus
- 图表使用 ECharts（树/条形/雷达任选，至少树+偏差来源条形）
- 所有百分比展示为 0–100%，数据传输与存储保持 0–1

---

## 12. 初始化数据（seed）— MUST
必须提供可演示闭环的初始化数据：
- 1 个示例项目（含 l1/l2/req 若干节点）
- 5 个示例用户（leader/pmo/pm/module_owner/compliance）
- 若干合规材料项：至少 1 条关键材料超期 → 触发 RED 覆盖规则
- 预设 1 个 YELLOW 节点用于演示升级逻辑（可通过调小 asOfDate 或设置偏差）

---

## 13. 工程与测试要求（MUST）

### 13.1 代码结构
- 后端分层：controller/service/repository/domain/dto/config
- 前端：router/store/api/components/views 清晰分层

### 13.2 测试（至少以下 2 类必须有）
1) ComputeService 单元测试：
   - 阈值判定（5%、15%）
   - P=0 的处理
   - 关键合规超期覆盖判红
2) 预警升级逻辑测试：
   - 5 个工作日未回绿升级 RED
   - 偏差扩大升级 RED

---

## 14. 开发计划（写进 README，可作为验收里程碑）
- 第 1 周：固化口径与原型（树/详情/JSON结构/权重）
- 第 2 周：全景图 + 计算引擎 + 审计日志
- 第 3 周：预警中心 + 合规台账 + 报表导出（试点）
- 第 4 周：试点优化 + 验收材料（留痕与导出）

---

## 15. 交付物清单（Claude Code 输出必须包含）
1) 仓库目录：
   - `frontend/`
   - `backend/`
   - `docker-compose.yml`
   - `README.md`
2) Flyway 迁移脚本 + seed
3) 可运行演示：`docker compose up` 后可访问页面完成：
   - 全景树展示信号灯
   - 点击节点查看偏差与来源
   - 预警中心查看/处置预警
   - 合规台账标记材料提交并触发重算
   - 导出周报 CSV

---

## 16. 生成顺序（请严格执行）
1) 生成仓库骨架与依赖（frontend/backend）
2) 生成 DB schema + Flyway migrations + seed
3) 实现后端核心：Node CRUD + Compute + Warning + Compliance + Audit
4) 实现前端核心：全景图 + 节点详情 + 预警中心 + 合规台账 + 导出
5) 补齐测试、README、docker-compose

> 输出要求：请直接生成所有代码文件内容，保证按 README 步骤可启动并完成演示闭环。
```
