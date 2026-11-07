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

**Selected mechanism:** TCP Loopback Sockets (`java.net.ServerSocket` / `java.net.Socket`, cross-platform)

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
STEP            Execute one instruction
RUN             Execute all instructions until HALT
RESET           Reset CPU and reload program
LOAD <hex>      Load custom assembly hex bytecode
CRC <payload>   Calculate 3GPP LTE CRC-24b and map to R5, R6, R7, A
QUIT            Terminate CoreProcess
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

| Metric | Standalone (Single JVM, 100k ops) | Multi-Process (TCP Loopback IPC :5000, 200 RTT) |
|--------|-------------------------|--------------------------------------|
| Total Elapsed Time | 28.92 ms (100,000 packets) | 306.92 ms (200 round-trips) |
| Throughput | 3,457,229 packets/sec | ~652 messages/sec |
| Per-item Latency | 0.0003 ms (0.3 µs) | mean = 1.535 ms (min 0.311 ms, p50 0.914 ms, p99 10.788 ms) |
| Process Fault Isolation | None (UI crash kills CPU Core) | Protected (Independent OS Processes + 1,024-slot non-blocking log queue) |
| Memory Protection | Shared heap (vulnerable) | Independent memory address spaces |

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
- [x] Standalone vs. multi-process benchmark (`programs/BenchmarkCRC.java`)
- [x] Performance analysis & architectural trade-off evaluation
- [x] Week 4 Allocated Algorithm: CRC-24b 3GPP LTE (`src/CRC24b.java`)
- [x] 8-Bit Hardware Register Slicing (R5=High, R6=Mid, R7=Low, A=Low)
- [x] CRC-24b Unit Verification Test Suite (`tests/TestCRC24b.java`, 9/9 PASS including 4,656-case exhaustive 1-bit & 2-bit sweep)
- [x] UI Preset 5 Integration (`[ Preset 5: CRC-24b LTE-B ]`)
- [x] Final Project Report (`docs/PBL_Report.pdf` and `docs/PBL_Report.docx`)
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
