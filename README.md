# AI Clouthes · AI 智能穿搭助手

> 一个基于 AI 的智能穿搭推荐与虚拟试穿系统。能够根据天气推荐衣物、识别衣柜照片中的服饰并按场景搭配，并生成 AI 试穿效果图。

## ✨ 功能特性

### 用户系统
- 用户注册 / 登录 / 登出
- 修改密码（仅支持登录后修改，暂不支持找回密码）
- 基于 JWT Cookie 的鉴权

### 根据天气推荐
- 获取指定地区的实时天气（高德天气 API）
- 结合天气情况由 AI 推荐合适的衣物
- 根据推荐结果生成穿着效果图

### 根据衣柜推荐
- 上传衣柜照片
- AI 识别衣柜中的服饰，并按用户输入的场景 / 目的进行搭配推荐
- 根据搭配结果生成试穿效果图

### 虚拟试穿
- 无模特试穿：整套服装 / 上衣 + 裤子
- 有模特试穿：整套服装 / 上衣 + 裤子

> ⚠️ 当前版本暂时移除了「保存记录」与「保存衣橱」功能，待复赛后视情况重新加入。

## 🏗️ 技术栈

| 层 | 技术 |
| --- | --- |
| Web 框架 | [Go Fiber v3](https://gofiber.io/) |
| 数据库 | SQLite（通过 [xorm](https://github.com/go-xorm/xorm) ORM） |
| 鉴权 | JWT（`golang-jwt/jwt v5`）+ Cookie |
| 配置管理 | [Viper](https://github.com/spf13/viper) |
| AI 能力 | Vivo BlueLM、阿里云百炼（图像生成）、阿里云轨迹流动 |
| 天气 / 地理 | 高德地图开放平台 |
| 前端 | 原生 HTML/JS 网页 + Android 原生 App |

## 📁 项目结构

```
.
├── main.go                 # 程序入口：中间件、日志、目录初始化、启动服务
├── config/                 # 配置文件读取（Viper）
├── database/               # 数据库连接与表结构（SQLite + xorm）
├── router/                 # 路由注册
├── handler/                # 业务处理器
│   ├── users/              #   用户：注册/登录/登出/改密
│   ├── clouthes_weather/   #   天气推荐与生成
│   ├── clouthes_recommend/ #   衣柜识别、推荐与生成
│   └── try_clouthes/       #   虚拟试穿（有/无模特）
├── utils/                  # 工具：JWT、中间件、文件处理、日志等
├── frontend/               # 网页前端（静态页面）
├── appSrc/                 # Android App 源码（Gradle 工程）
├── clouthesPic/            # 生成 / 试穿图片存储
├── 接口文档.md              # 完整 API 接口文档
├── 城市对应的区域行政编码.md  # 城市行政区划编码对照表
└── build-linux.sh          # Linux 静态编译脚本
```

## 🚀 快速开始

### 环境要求
- Go 1.24.2+
- 启用 CGO（SQLite 驱动 `mattn/go-sqlite3` 依赖）

### 1. 准备运行所需文件

在 `main.go` 同级目录下创建以下文件：

- `config.yaml` —— 配置文件（见下方格式）
- `server.log` —— 错误日志
- `use.log` —— 使用日志

> `parsePic`、`dapeiPic`、`clouthesPic/temp` 等目录会在启动时自动创建。

### 2. 配置文件 `config.yaml`

```yaml
server:
  port: 端口号          # 例如 8080
  host: 服务器IP
cookie:
  secret: 生成token的secret
vivo:
  appid: vivo_id        # Vivo BlueLM AppID
  appkey: vivo_key      # Vivo BlueLM AppKey
  aliguijikey: 轨迹流动的key
  aliyun: 阿里云百炼的key  # 图像生成
  gaode: 高德地图的key     # 天气 / 地理
```

### 3. 运行

```bash
go mod tidy
go run main.go
```

服务默认监听 `0.0.0.0:<port>`。

### 4. Linux 静态编译（可选）

使用 musl 工具链进行静态编译：

```bash
./build-linux.sh
```

## 🌐 接口概览

> 完整接口说明（参数、返回码、示例）见 [`接口文档.md`](./接口文档.md)。

### 无需鉴权
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/register` | 用户注册 |
| POST | `/login` | 用户登录 |

### 需要鉴权（携带 `token` Cookie，前缀 `/user`）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/user/chanpass` | 修改密码 |
| GET | `/user/logout` | 退出登录 |
| POST | `/user/pic/upload` | 上传衣柜照片 |
| POST | `/user/pic/parse` | 识别衣柜服饰 |
| GET | `/user/pic/makepic` | 根据推荐生成图片 |
| POST | `/user/weather/get` | 获取地区天气 |
| POST | `/user/weather/make` | 根据天气推荐衣物 |
| GET | `/user/weather/makepic` | 根据天气推荐生成图片 |
| POST | `/user/try/double` | 试穿：上衣 + 裤子（有模特） |
| POST | `/user/try/single` | 试穿：整套（有模特） |
| POST | `/user/try/nomodel1` | 试穿：整套（无模特） |
| POST | `/user/try/nomodel2` | 试穿：上衣 + 裤子（无模特） |

### 页面路由（网页前端）
`/`、`/login`、`/weather`、`/wardrobe`、`/try-on`、`/profile`

## 💻 前端

项目包含两套前端：

- **网页端**：位于 `frontend/`，启动后端后通过浏览器访问对应页面路由即可使用。
- **Android App**：位于 `appSrc/`，使用 Gradle 构建的原生 Java 工程，包含登录、首页、天气、衣柜、试穿、个人中心等模块。

## 📝 备注

- 上传请求体上限为 50MB（`BodyLimit`），以支持图片上传。
- 服务已开启 CORS，允许跨域访问。
- 数据库文件默认为 `./test.db`，首次启动自动建表。
