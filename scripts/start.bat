@echo off
chcp 65001 >nul
title 医语 — AI 助手
echo 正在启动医语，请稍候…
cd /d "%~dp0.."
.\runtime\bin\java.exe -jar app.jar
