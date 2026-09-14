@echo off
rem ===========================================================================
rem  校园报修系统 · 启动脚本（Windows）
rem  用法：run.cmd                    使用 config.properties 配置启动（默认内存库 + 8080 端口）
rem        run.cmd 8090               指定端口启动
rem        run.cmd 8090 jdbc          指定端口与存储模式（jdbc = MySQL 持久化）
rem  说明：若存在 lib\mysql-connector-j-8.3.0.jar 则自动加入 classpath（jdbc 模式必需）
rem ===========================================================================
chcp 65001 >nul
setlocal
set PORT=%1
if "%PORT%"=="" set PORT=8080
set MODE=%2

set CP=build\classes
if exist "lib\mysql-connector-j-8.3.0.jar" set CP=%CP%;lib\mysql-connector-j-8.3.0.jar

if "%JAVA_HOME%"=="" (
    set JAVABIN=java
) else (
    set JAVABIN="%JAVA_HOME%\bin\java.exe"
)

if "%MODE%"=="" (
    %JAVABIN% -Dfile.encoding=UTF-8 -Dserver.port=%PORT% -cp "%CP%" com.campus.repair.boot.CampusRepairApplication
) else (
    %JAVABIN% -Dfile.encoding=UTF-8 -Dserver.port=%PORT% -Dstorage.mode=%MODE% -cp "%CP%" com.campus.repair.boot.CampusRepairApplication
)
endlocal
