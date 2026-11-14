# 健身计划与训练记录系统

基于 Spring Boot 3 + Vue 3 的前后端分离毕业设计项目。系统以“每一组训练记录”为最小单位，打通
“训练计划 → 开始训练 → 逐组记录 → 完成训练 → 数据统计 → 规则建议”的闭环。

## 技术栈

| 层次 | 选型 |
|---|---|
| 后端 | Java 17、Spring Boot 3.3、Spring Security、MyBatis-Plus、JWT |
| 数据库 | MySQL 8 |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router、ECharts |
| 构建 | Maven、npm |

## 目录结构

```text
.
├─ backend/                 Spring Boot 后端
├─ frontend/                Vue 3 前端
├─ sql/
│  ├─ 01_schema.sql         建库建表脚本
│  └─ 02_seed.sql           肌肉部位、器械、动作库种子数据
├─ docs/
│  ├─ 数据库字段清单_统计公式_AI建议规则表.md
│  └─ 开发路线图.md
└─ 开题报告_基于SpringBoot与Vue的健身计划与训练记录系统.docx
```

## 本机环境（已配置）

| 工具 | 版本 | 安装位置 |
|---|---|---|
| JDK | Amazon Corretto 17.0.20 | `C:\Users\Zile\dev-tools\jdk17.0.20_12` |
| Maven | 3.9.16 | `C:\Users\Zile\dev-tools\apache-maven-3.9.16` |
| MySQL | 8.0.29（便携版） | `C:\Users\Zile\dev-tools\mysql-8.0.29-winx64` |
| Node.js | 24.15.0 | 系统已安装 |

用户级环境变量已写入 `JAVA_HOME`、`MAVEN_HOME`、`MYSQL_HOME` 和 `PATH`。新开一个终端后，
可以直接使用 `java`、`mvn`、`mysql` 命令。

MySQL 数据目录为 `C:\Users\Zile\dev-tools\mysql-data`，root 密码为 `123456`。
当前数据库、后端和前端均已启动。

## 快速启动

### 0. 启动 MySQL（电脑重启后需要）

MySQL 目前是便携版，不会自动注册为 Windows 服务。重启电脑后执行：

```powershell
Start-Process -FilePath "$env:MYSQL_HOME\bin\mysqld.exe" `
  -ArgumentList '--defaults-file=C:\Users\Zile\dev-tools\my.ini' `
  -WindowStyle Hidden
```

确认端口已监听：

```powershell
Get-NetTCPConnection -LocalPort 3306 -State Listen
```

### 1. 初始化数据库（已完成，无需重复执行）

在 MySQL 客户端中依次执行：

```sql
SOURCE sql/01_schema.sql;
SOURCE sql/02_seed.sql;
```

也可以使用命令行：

```bash
mysql -u root -p < sql/01_schema.sql
mysql -u root -p < sql/02_seed.sql
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认连接 `localhost:3306/fitness_db`，用户名 `root`。如需修改，设置环境变量：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的密码"
$env:JWT_SECRET = "至少32个字符的随机字符串"
mvn spring-boot:run
```

后端地址：`http://localhost:8080/api`

首次启动会自动创建管理员账号。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

## 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 管理员 |
| `demo` | `demo123` | 普通用户 |

打开 `http://localhost:5173` 即可登录。生产或答辩演示前请修改默认密码。

更完整的环境说明和重启步骤见 [docs/环境配置.md](docs/环境配置.md)。

## 当前进度

- [x] 需求与数据库设计文档
- [x] 项目骨架与目录约定
- [x] 完整建表脚本与基础种子数据
- [x] 注册、登录、JWT 鉴权、个人资料接口
- [x] 动作库分页查询与筛选接口
- [x] 登录页、主布局、动作库页面
- [x] 训练计划、训练日与计划动作 CRUD
- [x] 从计划开始训练与计划快照
- [x] 逐组训练记录、编辑与删除
- [x] 训练中临时加动作、替换动作、跳过动作
- [x] 结束训练、取消训练与历史记录回看
- [x] 自由训练（不关联计划的快速开始）
- [x] 身体数据记录（体重、体脂率、腰围）
- [x] 数据统计接口与图表（训练量、频率、部位组数、e1RM、体重、日历）
- [x] 规则引擎与智能建议（含反馈，外部模型接口预留）
- [x] 管理端（用户管理、动作库维护、平台统计、操作日志）
- [x] 系统计划模板与一键复制
- [x] 统计公式单元测试

## 开发约定

- 所有接口统一返回 `{ code, message, data }`，`code = 0` 表示成功。
- 前端请求统一走 `/api`，由 Vite 代理到后端。
- 重量统一使用千克，长度使用厘米，时间使用秒。
- 统计只使用已完成的正式组（`phase = WORK` 且 `is_completed = 1`）。
- 训练开始后，计划中的动作和组数写入 `session_exercise` 快照，后续修改计划不影响历史记录。
