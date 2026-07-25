# KinVoice 家声后端

这是赛事交付使用的精简后端，只提供家庭口述整理和有来源问答。它不保存家庭记忆，不包含 vivo TTS、音色克隆、旧聊天或旧快应用数据库接口。

## 接口

- `GET /health`：服务和大模型配置状态。
- `POST /v1/memories/draft`：口述文字整理为可校订草稿。
- `POST /v1/knowledge/ask`：仅依据客户端提交的候选记忆回答并返回来源。

## 本地运行

要求 Python 3.11 或 3.12。

```powershell
cd backend
Copy-Item .env.example .env
# 编辑 .env 中的 LLM_API_KEY、LLM_API_BASE、LLM_MODEL
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --port 8000
```

打开 `http://127.0.0.1:8000/docs` 查看接口。运行测试：

```powershell
python -m unittest discover -s tests -p "test_*.py" -v
```

## 中转站配置

中转站必须兼容 OpenAI Chat Completions：

```dotenv
LLM_API_KEY=你的密钥
LLM_API_BASE=https://中转站地址/v1
LLM_MODEL=中转站提供的模型标识
```

`LLM_API_BASE` 填到 `/v1`，不要把 `/chat/completions` 重复写进去。密钥只放在服务器 `.env`，不要放进 Swift、截图、提交文档或交付压缩包。

可选设置 `APP_ACCESS_TOKEN`，并把同一个值填入 iOS `Info.plist` 的 `APIAccessToken`。它只能减少接口被随意调用，不能替代正式登录鉴权，因为 App 包内的值可以被提取。生产环境还应配置云平台限流和费用告警。

## Docker 部署

```bash
cp .env.example .env
# 编辑 .env
docker compose up -d --build
curl https://你的域名/health
```

云平台必须在容器前提供有效 HTTPS。iOS 正式构建不应连接 HTTP 地址，也不要添加 App Transport Security 例外。

## 隐私边界

- 记忆默认保存在 iPhone 的 SwiftData 中。
- 只有用户主动点击 AI 整理或问家时，客户端才发送当前请求所需文字。
- 后端请求日志只记录字符数和候选记忆数量，不记录正文。
- 模型输出没有有效来源 ID 时，不展示无来源模型答案，改为返回可核对原文。
- 后端没有数据库和文件上传，重启不会丢失用户数据，因为用户数据从未保存在服务端。

## 旧代码说明

仓库中 `app/api`、`app/services` 和 `app/models` 下仍保留部分早期快应用文件，便于追溯原创开发过程；`app.main` 不导入它们，赛事运行和 Docker 镜像也不依赖它们。Mac 同学只需维护 `/v1` 相关代码。
