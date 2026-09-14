@echo off
rem ===========================================================================
rem  校园报修系统 · 一键构建脚本（无需 Maven / 无需联网）
rem  用法：build.cmd                 编译全部 Java 源码到 build\classes
rem        build.cmd -Clean         先清理 build 目录再编译
rem        build.cmd -WithSelfTest  编译后执行自测（99 项断言）
rem        build.cmd -Run           编译后启动服务
rem ===========================================================================
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build.ps1" %*
