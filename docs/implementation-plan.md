# AgentForge 10–12 周实施计划

## 一、目标与完成标准

在当前空仓库中建设一个可本地运行的云原生自治软件工程平台，完整跑通：

`GitHub 仓库 → Grill 澄清 → 结构化需求 → 自动计划 → 独立 K8s 沙箱 → Agent 编码/测试/修复 → PR、产物与临时预览`

第 8 周完成稳定 M0，第 12 周形成求职作品集版本。最终必须做到：

- Grill 完成后自动执行，不增加人工审批。
- 成功任务创建 Ready PR；失败任务不建 PR，只交付 Patch、日志和未满足标准。
- 控制面或 Agent Runtime 重启后可以从 checkpoint 恢复，外部副作用不重复。
- 沙箱内 Agent 以 root 工作，但不能访问宿主机、Docker Socket、Kubernetes API、其他任务或长期凭证。
- Function Calling、ReAct、RAG、Memory、Skill、Hook、MCP 和 2–3 个 Subagent 都至少有一个真实使用闭环。
- 三个录制级 Demo：全栈功能开发、Spring Boot 启动修复、依赖升级与文档 MCP 验证。

## 二、系统架构与关键实现

### 技术基线

- Web：Vue 3、TypeScript、Vite、Pinia、Element Plus、Monaco、xterm.js、Playwright。
- 控制面：Java 21、Spring Boot 3.5.x、Spring Security、Fabric8、Maven Wrapper。
- Agent Runtime：Python 3.12、uv、FastAPI、LangGraph 1.0.x、Pydantic。
- 基础设施：PostgreSQL + pgvector、Kafka、Redis、MinIO、k3d/K3s、Helm。
- 精确补丁版本在脚手架创建时锁入 Maven、npm 和 uv lockfile，不使用浮动依赖。

### 逻辑组件

- Java 模块化控制面负责 GitHub OAuth、GitHub App、项目/任务/运行状态、K8s 资源、事件投影、SSE、产物和预览生命周期。
- Python Runtime 负责 Grill、需求契约、LangGraph 编排、模型网关、ReAct Worker、预算、RAG、记忆、Skill、Hook、MCP 和 Subagent。
- 每个 `AgentRun` 创建独立 Namespace、PVC、Job、Service、Quota 和 NetworkPolicy。Job 内运行 root Executor，实际文件、Shell、Git、测试和浏览器操作都在这里发生。
- PostgreSQL 是唯一状态真源；控制面以 Outbox 发布 Kafka，消费者以 Inbox 去重。Redis用于登录会话、分布式锁和 SSE 实时扇出。
- LangGraph 使用 `AsyncPostgresSaver`，`thread_id=runId`。模型调用、工具执行、Git push、PR 创建和产物上传全部使用幂等键，适应节点恢复时的重放。

### 沙箱策略

- 本地使用 k3d/runc，一次最多运行 1 个 AgentRun；生产 Profile 可通过 `runtimeClassName=kata` 切换 Kata，但不宣称本地已具备 VM 级隔离。
- 默认限制：4 CPU、6 GiB 内存、20 GiB PVC、60 分钟 Deadline。
- 容器使用 UID 0，但禁止 privileged、hostPath、hostNetwork、hostPID、Docker Socket、ServiceAccount Token、`NET_ADMIN` 和 `SYS_ADMIN`。
- Egress 只允许 DNS、控制面和统一出口代理；代理按域名开放 GitHub、Maven Central、npm、PyPI 等必要站点。
- Executor 镜像预装 JDK 21、Maven、Node 24、npm、Python 3.12、uv、Git、ripgrep、Playwright Chromium；仍允许 Agent 在容器内安装额外依赖。
- 成功 Web 任务通过本地 Ingress 暴露 `run-<id>.127.0.0.1.sslip.io`，保留 30 分钟；失败任务打包后立即清理沙箱。

## 三、工作流、接口与智能体能力

### 状态机与执行图

任务状态：

`CREATED → GRILLING → SPEC_READY`

运行状态：

`QUEUED → PROVISIONING → RUNNING → VERIFYING → PACKAGING → SUCCEEDED | FAILED | TIMEOUT | CANCELLED`

执行图固定为：

`Analyze → Retrieve Context → Plan DAG → Dispatch Workers → Integrate → Verify → Critique → Repair → Deliver`

- Grill 使用 LangGraph `interrupt/resume`，最多三轮，每轮只询问阻塞实现或验收的缺失信息。
- Spec 完成后生成可展示的计划并自动进入队列。
- Worker 内部使用有界 ReAct 工具循环；Graph 管理预算、并行、checkpoint、验证和最多三轮 Repair。
- Critic 只能根据命令退出码、测试报告、浏览器断言、截图和 Git Diff 判定验收结果，不能接受 Agent 自述作为通过证据。
- 默认预算：60 分钟、3 次 Repair、150 次工具调用、200k 总 Token、最多 3 个并行 Subagent、可配置的 5 美元模型费用上限。

### 关键数据契约

- `RequirementSpec`：目标、范围内/外、验收标准、约束、技术栈、测试要求、预览要求。
- `ExecutionPlan`：DAG 节点、依赖、负责 Agent、文件边界、验证命令、交付物。
- `RunEvent`：`eventId/runId/sequence/type/agent/summary/timestamp/payload`，只展示动作和证据，不保存或暴露私有思维链。
- `ToolDefinition`：名称、JSON Schema、超时、执行位置、权限和结果大小限制。
- `ToolInvocation`：调用 ID、幂等键、参数、状态、日志引用、结果摘要和错误类型。
- `AgentResult`：修改文件、Commit、测试证据、未解决问题、Token/工具/时间消耗。
- 可选 `.agentforge/project.yaml` 描述模块、安装/构建/测试/启动命令和预览端口；缺失时自动识别并把画像保存在平台数据库，不擅自提交到目标仓库。

### 外部和内部接口

- `POST /api/v1/projects/import`
- `POST /api/v1/projects/{id}/tasks`
- `POST /api/v1/tasks/{id}/grill/messages`
- `GET /api/v1/tasks/{id}/spec`
- `GET /api/v1/runs/{id}`
- `GET /api/v1/runs/{id}/events`：支持 `Last-Event-ID` 断线续传。
- `POST /api/v1/runs/{id}/cancel`
- `GET /api/v1/runs/{id}/artifacts`
- Executor 提供幂等的创建调用、日志流、结果查询和取消接口。
- Kafka 使用版本化 JSON Envelope，核心 Topic 为 `agentforge.run.commands.v1` 与 `agentforge.run.events.v1`，按 `runId` 分区。

### 真实进阶能力

- Function Calling：实现文件、搜索、Shell、Git、测试、浏览器、产物和记忆工具；高层 `test.run` 与通用 `shell.execute` 同时保留。
- RAG：针对 Java/TypeScript 做 tree-sitter 符号切分，组合 PostgreSQL FTS、pgvector、ripgrep 和依赖邻居；禁止简单固定字符切块。
- Memory：Run Memory 由 checkpoint 管理；Project Memory 只从成功并验证过的任务写入，按项目隔离；Skill 作为 Procedural Memory。
- Subagent：Supervisor 动态选择 Backend、Frontend、Test/Critic；共享一个沙箱但使用独立 Git Worktree，Supervisor 按依赖 cherry-pick 到集成分支，文件范围冲突时改为串行。
- Skill：首个真实 Skill 为 `spring-boot-debug`，包含触发条件、允许工具、诊断流程和验证输出契约。
- Hook：实现 `before_model_call`、`before_tool_call`、`after_tool_call`、`after_verification` 和 `before_delivery`，用于预算、参数校验、上下文压缩、错误注入和产物整理。
- MCP：集成一个只读 Context7 文档 MCP，用于依赖升级任务；不可用时降级到仓库内文档并明确记录缺失上下文。Git 分支、Push 和 PR 仍走原生 GitHub Provider。
- GitHub：OAuth 仅用于登录；GitHub App Installation Token 用于仓库操作。私钥和模型 Key只存在于受控服务，沙箱只得到当前仓库、当前 Run、最长一小时的临时凭证。
- 成功后推送 `agentforge/run-<shortId>` 并创建 Ready PR；平台不自动合并。失败时保留分支快照和 Patch，但不创建 PR。

## 四、12 周交付顺序

1. 第 1–2 周：创建 monorepo、可复现 k3d/Helm 环境、PostgreSQL/Kafka/Redis/MinIO、GitHub OAuth/App、基础 Vue 控制台；同时创建独立 `AgentForge-DemoMall` 仓库。
2. 第 3–4 周：完成项目、任务、Grill、RequirementSpec、状态机、Outbox/Inbox、Kafka 事件和 SSE 时间线；接入 OpenAI 兼容模型网关与 Fake Model 测试实现。
3. 第 5–6 周：实现 Fabric8 沙箱编排、Executor 协议、Tool Registry、单 Agent ReAct、Git 工作区和临时凭证；跑通修改代码与测试。
4. 第 7–8 周：加入 Verify/Critic/Repair、MinIO 产物、截图、30 分钟预览、成功 PR/失败 Patch、checkpoint 恢复和清理控制器；达到 M0 发布门槛。
5. 第 9–10 周：实现代码混合检索、Project Memory、Skill Registry、Hook 框架和只读 Context7 MCP。
6. 第 11 周：实现 2–3 Subagent、Worktree 隔离、DAG 并行、结果集成和前端 Agent 拓扑。
7. 第 12 周：完善三个 Demo、20–30 个任务 Benchmark、OpenTelemetry/Prometheus 指标、架构文档、面试材料和一键演示脚本。

### 当前实施进度（2026-07-22）

- 第 1–2 周本地工程目标已完成：monorepo、依赖服务、k3d/Helm、GitHub OAuth/App 后端契约、基础 Vue 控制台和独立 `AgentForge-DemoMall` 仓库均已落地并通过构建/测试。
- k3d 本地入口为 `http://agentforge.localhost:8080`；开发认证模式无需 GitHub 凭据即可运行。
- GitHub 真实 OAuth、App 安装和仓库导入的端到端验证仍依赖仓库所有者按 `docs/github-app-setup.md` 创建并配置自己的 GitHub App；私钥和 Client Secret 不写入仓库。
- 下一阶段进入第 3–4 周，优先补齐 Inbox/版本化 Kafka Envelope、OpenAI 兼容模型网关和 Fake Model 确定性测试，再扩展现有 Grill、RequirementSpec、状态机、Outbox 与 SSE 的集成覆盖。

### 第 3–4 周推进记录（2026-07-23）

- 已完成 Event Envelope v1 与 JSON Schema，Outbox 不再发布裸 payload；事件 ID、聚合 ID、关联 ID、发生时间与 schemaVersion 均进入稳定契约。
- 已完成 PostgreSQL Inbox 幂等投影：Kafka 数据库事务提交后再确认 offset，崩溃重投时由 `(consumer, event_id)` 去重；无效消息进入 DLT，瞬时错误最多重试三次。
- 已完成 `RUN_STATUS_CHANGED` 消费、Run 状态机推进、RunEvent 持久化和 SSE 回放链路。
- 已完成 OpenAI-compatible Chat Completions 模型网关、Function Calling 映射和确定性 Fake Model；LangGraph Plan 节点通过 `submit_execution_plan` 生成并校验 `ExecutionPlan`，同时累计模型调用和 Token 使用量。
- 已补充真实 PostgreSQL/Testcontainers Inbox 测试、Envelope/消费者测试、OpenAI MockTransport 契约测试和 Fake Model 图执行测试。
- 已完成 k3d 跨进程冒烟：重复投递同一个 Kafka Event 后 Inbox 与 RunEvent 均只写入一次，状态机正确推进且 SSE 可从序号 1 回放；Outbox 实际消息符合 Envelope v1。
- 已修正 Docker Desktop 重启后的宿主服务寻址和 k3d 升级顺序：统一使用 `host.docker.internal`，Helm 更新后再强制滚动并等待新 Pod，避免旧 Pod 不健康时的 `--wait` 死锁。
- 第 3–4 周剩余重点：由 Runtime 正式消费 `RUN_QUEUED`、发布完整状态事件，使用模型驱动 Grill/RequirementSpec，并补充自动化 Kafka/Testcontainers 跨进程 E2E 与控制面恢复测试。

伴随仓库包含 Spring Boot + Vue + PostgreSQL/Flyway 小型商城，并预置稳定基线与任务分支：支付状态筛选、启动配置故障、依赖升级。测试栈使用 JUnit/Testcontainers、Vitest 和 Playwright。

## 五、测试与验收

- 单元与契约测试覆盖状态迁移、预算、Spec 校验、工具 Schema、事件兼容、路径处理和 GitHub Provider。
- Testcontainers 集成测试覆盖 PostgreSQL/pgvector、Kafka、Redis、MinIO、Outbox/Inbox 和重复事件。
- Fake Model + Fake Executor 可确定性重放完整 LangGraph，验证 Repair 上限、取消、超时和 MCP 降级。
- k3d 安全测试证明 root 可以安装依赖和执行浏览器，但不能读取宿主机、访问 Docker Socket/K8s API、跨 Namespace 或绕过出口代理。
- 恢复测试在工具执行、Git push 和 PR 创建前后分别杀掉控制面与 Runtime；恢复后不得重复执行工具、Commit、事件或 PR。
- GitHub E2E 从 OAuth 登录、安装 App、导入 DemoMall、完成 Grill，一直验证到 Ready PR、测试报告、截图和预览。
- 三个录制 Demo 在固定模型配置下各连续成功两次后才作为作品集演示；Benchmark 发布真实完成率、测试通过率、Repair 次数、Token/费用、运行时间和沙箱启动 P50/P95，不预填虚假指标。
- CI 在每个 PR 执行 Java、Python、Web、契约和集成测试；夜间执行完整 k3d E2E。

默认不实现多租户、团队权限、GitLab、Operator/CRD、Skill 市场、通用桌面控制、自动合并、审计审批或云端 Kata 运维。运行事件与指标仅用于产品反馈、恢复和效果证明。
