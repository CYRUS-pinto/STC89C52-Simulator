# Week 4 Status Report

## Team: Bondaas | Date: September 2026

---

## Summary

Week 4 separated the single-process simulator into **three independent processes** communicating via **TCP socket IPC**. Each team member independently owns one process. Cyrus integrated all three into a working multi-process system.

---

## Process Breakdown

| Process | File | Owner | Port | Role |
|---------|------|-------|------|------|
| CoreProcess | `src/CoreProcess.java` | Chrisel | 5000 | CPU, Memory, Stack, Queue execution |
| SimulatorUI | `src/SimulatorUI.java` | Shashidhara | client→5000 | User interaction, display |
| LoggerProcess | `src/LoggerProcess.java` | Arnold | 5001 | Execution and error logging |

---

## IPC Mechanism: TCP Sockets

**Selected mechanism:** TCP Sockets (POSIX-compatible, cross-platform)

**Justification:**
- Simple, reliable, ordered byte stream — no message loss, no reordering.
- No shared memory or shared heap between processes — each process is fully independent.
- Works on Windows, Linux, and macOS without any special configuration.
- Fits the communication pattern perfectly:
  - UI → Core: short command strings (STEP, RUN, RESET, QUIT)
  - Core → UI: structured state strings (STATE|PC=...|A=...|...)
  - Core → Logger: log strings (LOG|TYPE|message)

**Alternatives considered:**
- Named Pipes: platform-specific, more complex setup on Windows.
- Shared Memory (POSIX shm): not available in standard Java without JNI.
- Files: too slow, polling-based, not suitable for real-time display.

---

## IPC Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                   Multi-Process IPC Architecture                  │
└──────────────────────────────────────────────────────────────────┘

  ┌─────────────────┐                ┌──────────────────────────┐
  │  SimulatorUI    │  TCP :5000     │       CoreProcess        │
  │  (Shashidhara)  │ ─────────────▶│       (Chrisel)          │
  │                 │◀─────────────  │  CPU + Memory + Stack    │
  │  Swing UI       │  STATE|...     │  + Queue execution       │
  └─────────────────┘                └────────────┬─────────────┘
                                                   │ TCP :5001
                                                   │ LOG|TYPE|message
                                                   ▼
                                      ┌───────────────────────┐
                                      │    LoggerProcess      │
                                      │      (Arnold)         │
                                      │  Console + file log   │
                                      │  (simulator.log)      │
                                      └───────────────────────┘

  Startup order:
    1. LoggerProcess  (port 5001 must be ready before Core connects)
    2. CoreProcess    (connects to Logger, then waits for UI on 5000)
    3. SimulatorUI    (connects to Core on 5000)
```

---

## Communication Protocol

### UI → Core (commands)
```
STEP        Execute one instruction
RUN         Execute all instructions until HALT
RESET       Reset CPU and reload program
QUIT        Terminate CoreProcess
```

### Core → UI (responses)
```
STATE|PC=0005|A=08|B=00|CY=false|OV=false|SP=07|Q_SIZE=1
HALTED
RESET_DONE
```

### Core → Logger (log entries)
```
LOG|EXEC|PC=0005|A=8|CY=false
LOG|RESET|System state reset.
LOG|SHUTDOWN|CoreProcess termination requested.
```

---

## Benchmark: Standalone vs. Multi-Process

### Test program
The standard 11-instruction demo program (see CoreProcess, Week 2 demo).

### Results

| Metric | Standalone (single JVM) | Multi-Process (3 JVMs + sockets) |
|--------|------------------------|----------------------------------|
| CPU step time (avg) | < 1 ms | 1–3 ms (IPC overhead) |
| Total run time | ~0.1 ms | ~20–50 ms (socket round-trips) |
| Memory (JVM heap) | ~50 MB (1 JVM) | ~150 MB (3 JVMs) |
| CPU usage | 1 core | Up to 3 cores (parallel processes) |
| IPC overhead | 0 | ~1–3 ms per message |

### Analysis
- Multi-process adds IPC overhead (1–3 ms/message) which is negligible for an educational simulator.
- The benefit of multi-process is **architectural clarity** and **independent development** — each team member can develop and test their component independently.
- In real embedded tools (GDB, QEMU, OpenOCD) the debugger UI and target process are always separated, so this architecture mirrors professional practice.

---

## Completed Deliverables

- [x] Three-process working simulator
- [x] IPC architecture diagram (above)
- [x] TCP socket IPC — justified above
- [x] Individual component implementation by each member
- [x] Integration by Team Leader (Cyrus)
- [x] IPC tested: STEP, RUN, RESET, QUIT, HALTED, STATE, LOG all working
- [x] Standalone vs. multi-process benchmark
- [x] Performance analysis
- [x] Updated GitHub documentation

---

## How to Run

```bash
# Windows PowerShell — run each in a separate terminal

# Step 1: Compile everything
javac -cp src src\*.java tests\TestCPU.java programs\QueueTest.java

# Step 2: Terminal 1 — Logger (start first!)
java -cp src LoggerProcess

# Step 3: Terminal 2 — Core
java -cp src CoreProcess

# Step 4: Terminal 3 — UI (last)
java -cp src SimulatorUI

# To run tests
java -cp src;tests TestCPU

# To run queue validation
java -cp src;programs QueueTest
```

---

## Known Issues / Limitations

- SJMP with negative offset (backward jump) supported but not tested end-to-end in multi-process mode.
- LoggerProcess currently handles one session (one Core connection). Restart required for a new session.
- SimulatorUI does not yet show a raw memory dump panel (out of scope for Week 4, planned for later).
