# =============================================================================
#  校园报修系统 · 构建脚本（仅依赖 JDK，无需 Maven 与外网）
#  用法：
#     pwsh -File scripts\build.ps1                编译主程序
#     pwsh -File scripts\build.ps1 -Clean         先清理再编译
#     pwsh -File scripts\build.ps1 -WithSelfTest  编译并执行自测
#     pwsh -File scripts\build.ps1 -Run           编译后启动服务
# =============================================================================
param(
    [switch]$Clean,
    [switch]$WithSelfTest,
    [switch]$Run
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$root = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$testClassesDir = Join-Path $buildDir "test-classes"
$sourceDir = Join-Path $root "src\main\java"
$testSourceDir = Join-Path $root "src\test\java"

# ---------------------------------------------------------------- 定位 JDK
function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
        return $env:JAVA_HOME
    }
    $candidates = @(
        "D:\Java",
        "C:\Program Files\Java\jdk-25",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17"
    )
    foreach ($dir in $candidates) {
        if (Test-Path (Join-Path $dir "bin\javac.exe")) { return $dir }
    }
    $javac = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javac) { return (Split-Path -Parent (Split-Path -Parent $javac.Source)) }
    throw "未找到 JDK，请设置 JAVA_HOME 指向 JDK 安装目录"
}

$javaHome = Resolve-JavaHome
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
# 以运行时拼接的方式构造 -D 参数，避免脚本被外部工具改写时破坏参数前缀
$utf8Arg = ([string][char]45) + "D" + "file.encoding=UTF-8"
Write-Host "[构建] JDK: $javaHome" -ForegroundColor Cyan

if ($Clean -and (Test-Path $buildDir)) {
    Remove-Item -Recurse -Force $buildDir
    Write-Host "[构建] 已清理 build 目录" -ForegroundColor Cyan
}

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

# ------------------------------------------------- 收集源码（相对路径，规避空格）
Push-Location $root
try {
    $sources = Get-ChildItem -Path $sourceDir -Recurse -Filter *.java |
        ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\', '/') }
    if ($sources.Count -eq 0) { throw "未找到任何 Java 源文件：$sourceDir" }
    $argFile = Join-Path $buildDir "sources.txt"
    [System.IO.File]::WriteAllLines($argFile, $sources, (New-Object System.Text.UTF8Encoding($false)))

    Write-Host "[构建] 编译 $($sources.Count) 个源文件 ..." -ForegroundColor Cyan
    & $javac -encoding UTF-8 -d "build/classes" -Xlint:-options "@build/sources.txt"
    if ($LASTEXITCODE -ne 0) { throw "编译失败（javac 退出码 $LASTEXITCODE）" }
    Write-Host "[构建] 主程序编译成功 → build\classes" -ForegroundColor Green

    # 复制配置等资源到 classpath，使 -D 参数覆盖 config.properties 生效
    $resourceDir = Join-Path $root "src\main\resources"
    if (Test-Path $resourceDir) {
        Copy-Item -Path (Join-Path $resourceDir "*") -Destination $classesDir -Recurse -Force
        Write-Host "[构建] 已复制运行资源（config.properties 等）到 build\classes" -ForegroundColor Cyan
    }

    if ($WithSelfTest) {
        $testSources = @()
        if (Test-Path $testSourceDir) {
            $testSources = Get-ChildItem -Path $testSourceDir -Recurse -Filter *.java |
                ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\', '/') }
        }
        if ($testSources.Count -gt 0) {
            Write-Host "[构建] 编译自测程序 $($testSources.Count) 个源文件 ..." -ForegroundColor Cyan
            $testArgFile = Join-Path $buildDir "test-sources.txt"
            [System.IO.File]::WriteAllLines($testArgFile, $testSources, (New-Object System.Text.UTF8Encoding($false)))
            & $javac -encoding UTF-8 -cp "build/classes" -d "build/test-classes" -Xlint:-options "@build/test-sources.txt"
            if ($LASTEXITCODE -ne 0) { throw "自测程序编译失败" }
            Write-Host "[构建] 执行自测 ..." -ForegroundColor Cyan
            & $java $utf8Arg -cp "build/classes;build/test-classes" com.campus.repair.test.SelfTestRunner
            $code = $LASTEXITCODE
            if ($code -ne 0) { throw "自测未通过（退出码 $code）" }
        }
    }

    if ($Run) {
        Write-Host "[运行] 启动校园报修系统 ..." -ForegroundColor Cyan
        # 若 lib 目录存在 MySQL 驱动则加入 classpath（jdbc 模式必需）
        $cp = "build/classes"
        $driver = Join-Path $root "lib\mysql-connector-j-8.3.0.jar"
        if (Test-Path $driver) { $cp = $cp + ";lib/mysql-connector-j-8.3.0.jar" }
        & $java $utf8Arg -cp $cp com.campus.repair.boot.CampusRepairApplication
    }
}
finally {
    Pop-Location
}
