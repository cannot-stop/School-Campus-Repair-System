# =============================================================================
#  校园报修系统 · 编译校验与业务自测脚本（仅依赖 JDK，无需 Maven 与外网）
#
#  说明：本项目的运行方式是「IDEA + Tomcat 部署」，Tomcat 会自行编译/部署 Web 应用，
#        因此本脚本不启动服务，只做两件事：
#          1) 用 javac 编译全部 Java 源码，尽早发现编译错误；
#          2) 可选地运行业务自测（99 项断言），验证 Service/DAO 层业务规则。
#
#  用法：
#     pwsh -File scripts\build.ps1                编译主程序（含 Servlet 适配层需要 Tomcat 的 jar）
#     pwsh -File scripts\build.ps1 -Clean         先清理 build 目录再编译
#     pwsh -File scripts\build.ps1 -WithSelfTest  编译并执行业务自测（99 项断言）
# =============================================================================
param(
    [switch]$Clean,
    [switch]$WithSelfTest
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$root = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$testClassesDir = Join-Path $buildDir "test-classes"
$sourceDir = Join-Path $root "src\main\java"
$servletAdapterDir = Join-Path $root "src\servlet-adapter\java"
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

# ------------------------------------------------- 定位 Tomcat（编译 Servlet 适配层需要其 jar）
function Resolve-TomcatHome {
    foreach ($candidate in @($env:CATALINA_HOME, $env:TOMCAT_HOME, "C:\Program Files (x86)\Java\apache-tomcat-11.0.18")) {
        if ($candidate -and (Test-Path (Join-Path $candidate "lib\servlet-api.jar"))) { return $candidate }
    }
    return $null
}

$tomcatHome = Resolve-TomcatHome
$classPathParts = @()
if ($tomcatHome) {
    $classPathParts += (Join-Path $tomcatHome "lib\servlet-api.jar")
    $classPathParts += (Join-Path $tomcatHome "lib\jsp-api.jar")
    Write-Host "[构建] Tomcat: $tomcatHome（已加入 servlet-api/jsp-api，用于编译 Servlet 适配层）" -ForegroundColor Cyan
} else {
    Write-Host "[构建] 未找到 Tomcat（可用 CATALINA_HOME 指定）；本次仅编译主程序，跳过 Servlet 适配层" -ForegroundColor Yellow
}

# 第三方依赖（MyBatis、MySQL 驱动、slf4j）统一从 lib 目录获取
$libDir = Join-Path $root "lib"
if (Test-Path $libDir) {
    $libJars = Get-ChildItem -Path $libDir -Filter *.jar | Select-Object -ExpandProperty FullName
    if ($libJars.Count -gt 0) {
        $classPathParts += $libJars
        Write-Host ("[构建] lib 依赖 " + $libJars.Count + " 个：" + (($libJars | ForEach-Object { Split-Path $_ -Leaf }) -join "、")) -ForegroundColor Cyan
    }
}
$compileClassPath = ($classPathParts -join ";")
if (-not ($libJars | Where-Object { $_ -like "*mybatis*" })) {
    Write-Host "[构建] 警告：lib 下未找到 mybatis 依赖，MyBatis Mapper 相关代码将编译失败" -ForegroundColor Yellow
}

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

# ------------------------------------------------- 收集源码（相对路径，规避空格）
Push-Location $root
try {
    $sourceDirs = @($sourceDir)
    if ($tomcatHome -and (Test-Path $servletAdapterDir)) {
        $sourceDirs += $servletAdapterDir
    }
    $sources = @()
    foreach ($dir in $sourceDirs) {
        $sources += Get-ChildItem -Path $dir -Recurse -Filter *.java |
            ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\', '/') }
    }
    if ($sources.Count -eq 0) { throw "未找到任何 Java 源文件：$sourceDir" }
    $argFile = Join-Path $buildDir "sources.txt"
    [System.IO.File]::WriteAllLines($argFile, $sources, (New-Object System.Text.UTF8Encoding($false)))

    Write-Host "[构建] 编译 $($sources.Count) 个源文件 ..." -ForegroundColor Cyan
    if ($compileClassPath) {
        & $javac -encoding UTF-8 -cp $compileClassPath -d "build/classes" -Xlint:-options "@build/sources.txt"
    } else {
        & $javac -encoding UTF-8 -d "build/classes" -Xlint:-options "@build/sources.txt"
    }
    if ($LASTEXITCODE -ne 0) { throw "编译失败（javac 退出码 $LASTEXITCODE）" }
    Write-Host "[构建] 编译成功 → build\classes" -ForegroundColor Green

    # 复制配置等资源到 classpath，使 -D 参数覆盖 config.properties 生效
    $resourceDir = Join-Path $root "src\main\resources"
    if (Test-Path $resourceDir) {
        Copy-Item -Path (Join-Path $resourceDir "*") -Destination $classesDir -Recurse -Force
        Write-Host "[构建] 已复制运行资源（config.properties 等）到 build\classes" -ForegroundColor Cyan

        # 同步一份到 Web 根：IDEA 打包 artifact 时把 webapp/ 目录原样复制进 WAR，
        # 所以 mybatis-config.xml、mapper/*.xml 只有放在 webapp\WEB-INF\classes 下，
        # 才能保证部署后一定位于类路径上（不依赖 IDEA 是否拷贝 src/main/resources）。
        $webClassesDir = Join-Path $root "webapp\WEB-INF\classes"
        New-Item -ItemType Directory -Force -Path $webClassesDir | Out-Null
        Copy-Item -Path (Join-Path $resourceDir "*") -Destination $webClassesDir -Recurse -Force
        $mirrored = @(Get-ChildItem -Path $webClassesDir -Recurse -File).Count
        Write-Host ("[构建] 已同步运行资源到 webapp\WEB-INF\classes（" + $mirrored + " 个文件，供 IDEA 打包 WAR 使用）") -ForegroundColor Cyan
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
}
finally {
    Pop-Location
}
