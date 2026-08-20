@echo off
REM ─────────────────────────────────────────────────────────────────────────
REM JVM Diagnostic Recipe for Principal-level troubleshooting (Windows)
REM ─────────────────────────────────────────────────────────────────────────
REM
REM USAGE:
REM   scripts\jvm-diagnose.bat <pid>
REM   scripts\jvm-diagnose.bat %ERRORLEVEL%  (if you know the PID)
REM
REM OUTPUT: %TEMP%\jvm-diagnose-<pid>\ with timestamped files
REM ─────────────────────────────────────────────────────────────────────────

setlocal enabledelayedexpansion

set PID=%~1
if "%PID%"=="" (
    echo Usage: %0 ^<pid^>
    echo Find PID: jps -l ^| findstr account-service
    exit /b 1
)

set OUTDIR=%TEMP%\jvm-diagnose-%PID%
mkdir "%OUTDIR%" 2>nul

echo ═══════════════════════════════════════════════════════════
echo  JVM Diagnostic Collection — PID: %PID%
echo  Output: %OUTDIR%
echo ═══════════════════════════════════════════════════════════

REM ── 1. JVM Flags ────────────────────────────────────────────────────────
echo.
echo ▸ [1/5] JVM Flags...
jcmd %PID% VM.flags > "%OUTDIR%\vm-flags.txt" 2>&1
jcmd %PID% VM.system_properties > "%OUTDIR%\system-properties.txt" 2>&1

echo   Key flags:
findstr /i "UseZGC ZGenerational MaxGCPauseMillis UseVirtualThreads" "%OUTDIR%\vm-flags.txt" 2>nul
if errorlevel 1 echo   ⚠ Could not parse VM.flags

REM ── 2. GC Stats ─────────────────────────────────────────────────────────
echo.
echo ▸ [2/5] GC Stats...
jstat -gc %PID% > "%OUTDIR%\gc-stats.txt" 2>&1
jstat -gcutil %PID% > "%OUTDIR%\gc-utilization.txt" 2>&1

REM ── 3. Thread Dump ──────────────────────────────────────────────────────
echo.
echo ▸ [3/5] Thread Dump...
jcmd %PID% Thread.print > "%OUTDIR%\thread-dump.txt" 2>&1

REM ── 4. Heap Histogram ──────────────────────────────────────────────────
echo.
echo ▸ [4/5] Heap Histogram (top 30)...
jcmd %PID% GC.class_histogram > "%OUTDIR%\heap-histogram.txt" 2>&1

REM ── 5. Deadlock Detection ─────────────────────────────────────────────
echo.
echo ▸ [5/5] Deadlock Detection...
jcmd %PID% Thread.print -l > "%OUTDIR%\thread-dump-locked.txt" 2>&1
findstr /c:"Found one Java-level deadlock" "%OUTDIR%\thread-dump-locked.txt" >nul 2>&1
if not errorlevel 1 (
    echo   ⚠⚠⚠ DEADLOCK DETECTED ⚠⚠⚠
    echo   See %OUTDIR%\thread-dump-locked.txt
)

echo.
echo ═══════════════════════════════════════════════════════════
echo  Diagnostic files saved to: %OUTDIR%
echo ═══════════════════════════════════════════════════════════

endlocal
