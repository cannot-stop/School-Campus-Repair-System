@echo off
rem ===========================================================================
rem  Campus Repair System - one-key build launcher (no Maven, no network)
rem
rem  Usage:
rem     build.cmd                 compile all Java sources into build\classes
rem     build.cmd -Clean          clean the build dir first, then compile
rem     build.cmd -WithSelfTest   compile, then run the self test (99 asserts)
rem
rem  Note: this launcher is ASCII-only on purpose. cmd.exe parses a .cmd file
rem  using the console code page active when the file is opened, so non-ASCII
rem  text here would be mis-decoded on some machines. All Chinese console
rem  output is produced by scripts\build.ps1 (UTF-8 with BOM).
rem ===========================================================================
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build.ps1" %*
