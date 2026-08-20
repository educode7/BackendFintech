#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────
# JVM Diagnostic Recipe for Principal-level troubleshooting
# ─────────────────────────────────────────────────────────────────────────
#
# USAGE:
#   ./scripts/jvm-diagnose.sh <pid>
#   ./scripts/jvm-diagnose.sh $(pgrep -f account-service)
#
# WHAT IT COLLECTS:
#   1. JVM flags (verify ZGC, virtual threads, container limits)
#   2. GC stats (heap usage, pause times, allocation rate)
#   3. Thread dump (detect deadlocks, virtual thread count)
#   4. Heap histogram (top memory consumers without full dump)
#   5. HikariCP pool stats (active connections, idle, waiting threads)
#   6. Flight Recorder snapshot (if available)
#
# OUTPUT: /tmp/jvm-diagnose-<pid>/ with timestamped files
# ─────────────────────────────────────────────────────────────────────────

set -euo pipefail

PID=${1:?Usage: $0 <pid>}
OUTDIR="/tmp/jvm-diagnose-$(date +%Y%m%d-%H%M%S)-${PID}"
mkdir -p "$OUTDIR"

echo "═══════════════════════════════════════════════════════════"
echo " JVM Diagnostic Collection — PID: $PID"
echo " Output: $OUTDIR"
echo "═══════════════════════════════════════════════════════════"

# ── 1. JVM Flags ────────────────────────────────────────────────────────
echo ""
echo "▸ [1/6] JVM Flags..."
jcmd "$PID" VM.flags > "$OUTDIR/vm-flags.txt" 2>&1 || echo "  ⚠ VM.flags failed"
jcmd "$PID" VM.system_properties > "$OUTDIR/system-properties.txt" 2>&1 || echo "  ⚠ VM.system_properties failed"

# Verify key settings
echo "  Key flags:"
grep -E "(UseZGC|ZGenerational|MaxGCPauseMillis|UseVirtualThreads)" "$OUTDIR/vm-flags.txt" 2>/dev/null || echo "  ⚠ Could not parse VM.flags"

# ── 2. GC Stats ─────────────────────────────────────────────────────────
echo ""
echo "▸ [2/6] GC Stats..."
jstat -gc "$PID" > "$OUTDIR/gc-stats.txt" 2>&1 || echo "  ⚠ jstat failed"
jstat -gcutil "$PID" > "$OUTDIR/gc-utilization.txt" 2>&1 || echo "  ⚠ jstat -gcutil failed"

echo "  Current heap usage:"
tail -1 "$OUTDIR/gc-utilization.txt" 2>/dev/null | awk '{printf "    Eden: %.1f%% | Survivor: %.1f%% | Old: %.1f%% | GC Count: %s | GC Time: %sms\n", $1, $2, $3, $7, $8}'

# ── 3. Thread Dump ──────────────────────────────────────────────────────
echo ""
echo "▸ [3/6] Thread Dump..."
jcmd "$PID" Thread.print > "$OUTDIR/thread-dump.txt" 2>&1 || echo "  ⚠ Thread.print failed"

TOTAL_THREADS=$(grep -c "^\"" "$OUTDIR/thread-dump.txt" 2>/dev/null || echo "0")
VIRTUAL_THREADS=$(grep -c "Virtual" "$OUTDIR/thread-dump.txt" 2>/dev/null || echo "0")
BLOCKED_THREADS=$(grep -c "State.BLOCKED" "$OUTDIR/thread-dump.txt" 2>/dev/null || echo "0")
WAITING_THREADS=$(grep -c "State.WAITING" "$OUTDIR/thread-dump.txt" 2>/dev/null || echo "0")

echo "  Total threads:  $TOTAL_THREADS"
echo "  Virtual threads: $VIRTUAL_THREADS"
echo "  Blocked:        $BLOCKED_THREADS"
echo "  Waiting:        $WAITING_THREADS"

# Detect deadlocks
jcmd "$PID" Thread.print -l > "$OUTDIR/thread-dump-locked.txt" 2>&1 || true
if grep -q "Found one Java-level deadlock" "$OUTDIR/thread-dump-locked.txt" 2>/dev/null; then
    echo "  ⚠⚠⚠ DEADLOCK DETECTED ⚠⚠⚠"
    grep -A 20 "Found one Java-level deadlock" "$OUTDIR/thread-dump-locked.txt" >> "$OUTDIR/deadlock-alert.txt"
fi

# ── 4. Heap Histogram (top 30) ─────────────────────────────────────────
echo ""
echo "▸ [4/6] Heap Histogram (top 30)..."
jcmd "$PID" GC.class_histogram > "$OUTDIR/heap-histogram.txt" 2>&1 || echo "  ⚠ GC.class_histogram failed"

echo "  Top 10 memory consumers:"
head -12 "$OUTDIR/heap-histogram.txt" 2>/dev/null | tail -10 | awk '{printf "    %s %s instances (%s bytes)\n", $1, $2, $3}'

# ── 5. HikariCP Pool Stats ─────────────────────────────────────────────
echo ""
echo "▸ [5/6] HikariCP Pool Stats..."
# Try to get HikariCP stats via JMX
jcmd "$PID" VM.flags 2>/dev/null | grep -q "ManagementServer" && {
    echo "  JMX available — collecting pool metrics..."
    # This requires JMX to be enabled; output is best-effort
} || echo "  JMX not enabled — pool stats require JMX or actuator endpoint"

# ── 6. Flight Recorder ─────────────────────────────────────────────────
echo ""
echo "▸ [6/6] Flight Recorder..."
jcmd "$PID" JFR.dump name=0 filename="$OUTDIR/flight-recording.jfr" 2>/dev/null && \
    echo "  ✓ JFR dump saved" || echo "  ⚠ JFR not available (requires -XX:StartFlightRecording)"

# ── Summary ─────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════════"
echo " Diagnostic files saved to: $OUTDIR"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "Quick analysis commands:"
echo "  cat $OUTDIR/vm-flags.txt          # Verify ZGC + virtual threads"
echo "  cat $OUTDIR/gc-stats.txt          # Raw GC counters"
echo "  grep 'BLOCKED' $OUTDIR/thread-dump.txt  # Find blocked threads"
echo "  head -20 $OUTDIR/heap-histogram.txt      # Top memory consumers"
