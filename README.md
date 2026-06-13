<details>
<summary>English</summary>

# Yiyu (医语)

> AI assistant for doctors — speak naturally, get a PPT.
> A gift for brother.

Open the app, say what you need, and Yiyu generates a polished presentation using your hospital's template. No commands, no settings, just natural language.

## Why Yiyu

Doctors shouldn't have to wrestle with AI tools. Yiyu is a standalone desktop app — double-click to launch, type or speak what you need, and get a ready-to-use PPT.

## Tech Stack

| Layer | Tech |
|---|---|
| Desktop shell | JavaFX WebView |
| Backend | Spring Boot 3 + JDK 21 |
| Frontend | Vue 3 (Vite) |
| AI | Spring AI + DeepSeek API |
| PPT | Apache POI + POI-TL |
| Packaging | jlink (bundled JRE, ~50MB) |

## Quick Start

```bash
git clone https://github.com/shenyb/yiyu.git
cd yiyu

# Backend
cd java
./mvnw spring-boot:run

# Frontend (separate terminal)
cd web
npm install
npm run dev
```

## Project Structure

```
yiyu/
├── java/          # Spring Boot backend
├── web/           # Vue 3 frontend
├── docs/          # Documentation
├── scripts/       # Build & launch scripts
└── README.md
```

</details>

<details open>
<summary>中文</summary>

# 医语 (Yiyu)

> 给哥做的 AI PPT 助手 — 说大白话就能做 PPT。
> A gift for brother.

打开软件，说你要什么，医语就用医院模板帮你生成一份漂亮的 PPT。不用学、不用设置、不用打字。

## 为什么要做

医生不应该跟 AI 工具搏斗。医语是一个双击就开的桌面软件，说你需要什么，直接出 PPT。

## 技术栈

| 层 | 技术 |
|---|---|
| 桌面窗口 | JavaFX WebView |
| 后端 | Spring Boot 3 + JDK 21 |
| 前端 | Vue 3 (Vite) |
| AI | Spring AI + DeepSeek API |
| PPT 生成 | Apache POI + POI-TL |
| 打包 | jlink 裁剪 JRE（~50MB，无需安装） |

## 快速开始

```bash
git clone https://github.com/shenyb/yiyu.git
cd yiyu

# 后端
cd java
./mvnw spring-boot:run

# 前端（另开终端）
cd web
npm install
npm run dev
```

## 项目结构

```
yiyu/
├── java/          # Spring Boot 后端
├── web/           # Vue 3 前端
├── docs/          # 文档
├── scripts/       # 构建和启动脚本
└── README.md
```

</details>
