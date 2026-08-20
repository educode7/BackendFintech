# JVM Tuning Reference — Wallet Backend

## Why These Flags?

### ZGC (Z Garbage Collector)
```
-XX:+UseZGC -XX:+ZGenerational -XX:MaxGCPauseMillis=1
```
- **Sub-millisecond pauses**: G1GC targets 200ms; ZGC targets <1ms. For a fintech backend where p99 latency = revenue, this matters.
- **Generational mode** (JDK 21+): better throughput than single-gen ZGC by separating short-lived (request-scoped) from long-lived (connection pool) objects.
- **Container-aware**: JDK 25 respects cgroup limits automatically. `-Xmx512m` is a ceiling; the JVM won't exceed the container's memory limit.

### Virtual Threads
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
- **Why**: Spring Boot 4 + JDK 25 = virtual threads are production-ready. Every HTTP request, Kafka listener, and @Async task runs on a virtual thread.
- **Impact**: With 10,000 concurrent requests, platform threads would exhaust memory (each thread ~1MB stack). Virtual threads use ~1KB, so 10,000 = ~10MB.
- **Blocking is OK**: JDBC calls, Redis ops, HTTP calls all block the virtual thread — but that's fine because virtual threads are cheap to park/unpark.

### HikariCP Pool Sizing
```yaml
hikari:
  maximum-pool-size: 20
```
- **With virtual threads**: The bottleneck shifts from thread count to DB connections. 20 connections for PostgreSQL is optimal (PG handles ~100 concurrent connections well, but each service only needs ~20).
- **leak-detection-threshold: 5000**: If a connection is held >5s, log a warning. This catches bugs where a connection is never returned to the pool.

### Hibernate Batching
```yaml
hibernate:
  jdbc:
    batch_size: 25
    order_inserts: true
    order_updates: true
  default_batch_fetch_size: 25
```
- **batch_size: 25**: Hibernate accumulates INSERT/UPDATE statements and flushes them in batches of 25. Reduces round-trips to Postgres by ~10x.
- **order_inserts/updates**: Groups statements by entity type for better batch efficiency.
- **default_batch_fetch_size: 25**: When loading collections, fetch 25 rows at a time instead of 1 (N+1 killer).

## GC Tuning Decisions

| Flag | Value | Why |
|------|-------|-----|
| `-XX:+UseZGC` | on | Sub-ms pauses for latency-critical workloads |
| `-XX:+ZGenerational` | on | Better throughput than single-gen ZGC |
| `-XX:MaxGCPauseMillis=1` | 1ms | Aggressive target; ZGC can achieve it |
| `-Xms256m` | 256m | Initial heap; avoids resize pauses at startup |
| `-Xmx512m` | 512m | Ceiling; container limit is the real ceiling |
| `-Xlog:gc*` | file output | Structured GC logs for Prometheus/Grafana |
| `-XX:StartFlightRecording` | on | JFR for profiling without restart |

## Diagnostic Commands

### Quick Health Check
```bash
# Find PID
jps -l | grep account-service

# GC stats
jstat -gc <pid>
jstat -gcutil <pid>

# Thread count
jcmd <pid> Thread.print | grep -c "^\""

# Heap histogram (top 20)
jcmd <pid> GC.class_histogram | head -22
```

### Full Diagnosis
```bash
./scripts/jvm-diagnose.sh <pid>
```

### Heap Dump (when OOM suspected)
```bash
# Trigger heap dump
jcmd <pid> GC.heap_dump /tmp/heap-$(date +%s).hprof

# Analyze with jhat (JDK 25)
jhat -J-Xmx4g /tmp/heap-*.hprof
# Open http://localhost:7000

# Or with Eclipse MAT (recommended for production)
# MAT can analyze jcmd-produced heap dumps directly
```

### Thread Dump Analysis
```bash
# Detect deadlocks
jcmd <pid> Thread.print -l | grep -A 20 "deadlock"

# Find blocked threads (contention)
jcmd <pid> Thread.print | grep "State.BLOCKED" -A 5

# Virtual thread count
jcmd <pid> Thread.print | grep -c "Virtual"
```

## What a Principal Looks For

1. **GC pause distribution**: Not average — look at p99. If p99 > 10ms, investigate.
2. **Allocation rate**: High allocation → frequent GC → higher CPU. Profile object creation.
3. **Thread count**: With virtual threads, platform thread count should be low (~20-50). If it's high, something is pinning platform threads.
4. **HikariCP wait**: If threads are waiting for connections, increase pool size or reduce connection hold time.
5. **Deadlocks**: Always check thread dumps for deadlocks. Even one is a production incident.
6. **Heap pressure**: If Old gen is >80% full consistently, objects are surviving too long. Check for memory leaks.
