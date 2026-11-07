# Team Bondaas: Project Meeting Record and Engineering Log

## STC89C52 Microcontroller Multi-Process Simulator

This document records the team's architectural planning, technical discussions, subsystem implementation, branch integration, and verification activities from project kickoff on 22 August 2026 through final integration in November 2026.

---

## 1. Project Team

| Register No | Member Name | Assigned Subsystem & Role |
| :---: | --- | --- |
| **25190111** | **Cyrus Shobith Pinto** | **Team Leader:** Multi-process architecture, TCP socket IPC orchestration, 3GPP LTE CRC-24b implementation, benchmarking, and integration |
| **25190110** | **Chrisel Lobo** | **Core Process Lead:** 17-opcode 8051 Fetch-Decode-Execute engine, 8 KB ROM / 256B RAM model, PSW flags, stack/queue integration, and documentation |
| **25190106** | **Arnold Noah Karyappa** | **Logging Subsystem Lead:** Independent TCP logging daemon (`Port 5001`), thread-safe `LogHandler` persistence (`simulator.log`), and logger verification tests |
| **25190148** | **Shashidhara Poojari** | **UI Subsystem Lead:** Embedded HTTP server (`Port 8080`), 7-panel Web Dashboard (`index.html`), Swing desktop client (`SimulatorUI.java`), and preset loader |

---

## 2. Project Overview

The project is an educational, instruction-accurate software simulator for a 17-opcode subset of the STC89C52 (Intel MCS-51 family) 8-bit microcontroller. It integrates computer architecture (Fetch-Decode-Execute pipeline, register banks, PSW status flags, internal RAM), data structures (hardware stack and 16-slot circular FIFO queue), operating systems concepts (multi-process fault isolation, bounded non-blocking producer-consumer queues, thread synchronization), and computer networking (TCP loopback IPC and 3GPP TS 36.212 CRC-24b error detection).

---

## 3. Chronological Meeting & Milestone Summary

| Date / Period | Phase / Meeting | Key Engineering Activities and Outcomes |
| --- | --- | --- |
| **22–28 August 2026** | **Phase 1 (Meetings 1 & 2):** Kickoff & Architecture Study | Problem breakdown, STC89C52 datasheet study, selection of standard Java, initial task allocation, and Git workflow setup. |
| **3–8 September 2026** | **Phase 2 (Meeting 3):** Core CPU & Memory Modeling | Implementation of `CPU.java`, `Registers.java`, `PSW.java`, `Memory.java`, and `DataStructures.java` with unsigned 8-bit masking (`& 0xFF`). |
| **28–30 September 2026** | **Phase 3 (Meeting 4):** Multi-Process IPC Separation | Separation into `CoreProcess` (`Port 5000`), `LoggerProcess` (`Port 5001`), and `UIProcess` (`Port 8080`) communicating over TCP loopback sockets. |
| **26 Oct – 2 Nov 2026** | **Phase 4:** Branch Audit & Integration | Reconciliation of `arnold-LOG`, `Shashidhara-UI-Final`, and `chrisel-CORE` branches (`URLDecoder.decode()` fix, stack underflow guard, clean ROM load). |
| **3–7 November 2026** | **Phase 5 (Meeting 5):** CRC-24b Lab, Backpressure & Final Report | Implementation of 3GPP LTE-B CRC-24b (`0x800063`) across `R5:R6:R7`, non-blocking `ArrayBlockingQueue(1024)` log dispatcher, `BenchmarkCRC.java`, and final report. |

---

## 4. Phase 1: Kickoff, Architecture Study, and Task Allocation (Meetings 1 & 2)

- **Meeting 1:** 22 August 2026 (Google Meet `qwi-axoh-wmh` and Miro architecture board)
- **Meeting 2:** 27 August 2026, 4:20 PM

### Key Discussions & Decisions
1. **Language Selection:** Standard Java (JDK) was selected for its object-oriented modularity, cross-platform `java.net.Socket` networking, and built-in concurrency primitives (`java.util.concurrent`).
2. **Hardware Study Allocation:**
   - **Shashidhara Poojari:** Memory organization (`8 KB` code ROM, `256 B` internal RAM) and Program Counter (`PC`).
   - **Chrisel Lobo:** Hardware Stack mechanism (`SP` initialized to `07H`) and Timer/Counter organization.
   - **Arnold Noah Karyappa:** CPU register organization (`R0`–`R7` across 4 banks), Program Status Word (`PSW`), instruction categories, and GPIO ports.
   - **Cyrus Shobith Pinto:** Interrupt organization, multi-process system architecture, and repository management.
3. **Git Workflow:** Feature development was organized on dedicated member and weekly branches (`week-01` through `week-05`, `chrisel-CORE`, `arnold-LOG`, `Shashidhara-UI-Final`, `cyrus-integration`) merged into `main` after testing.

---

## 5. Phase 2: Core CPU Foundation and Data Structures (Meeting 3)

- **Meeting 3:** 3 September 2026, 1:00 PM – 3:30 PM (College Library) & 7–8 September 2026

### Technical Implementation
- **Unsigned 8-Bit Arithmetic in Java:** Because Java's primitive `byte` is signed (`-128` to `127`), all register and RAM reads/writes apply an explicit bitwise mask (`& 0xFF`) to prevent 32-bit sign-extension from corrupting `ADD`/`SUBB` carry (`CY`) and overflow (`OV`) calculations.
- **Fetch-Decode-Execute Engine (`CPU.java`):** Implemented a 17-opcode instruction subset (`NOP`, `MOV A,#data`, `MOV direct,#data`, `ADD A,#data`, `SUBB A,#data`, `MUL AB`, `DIV AB`, `INC A`, `DEC A`, `ANL A,#data`, `ORL A,#data`, `XRL A,#data`, `SJMP rel`, `PUSH direct`, `ENQUEUE A`, `DEQUEUE A`, `HALT`), supporting both synthetic `0xFF` halting and standard 8051 `SJMP $` (`80 FE`) self-loop halting.
- **Hardware Stack & Circular Queue (`DataStructures.java`):** Implemented the 8051 stack pointer (`SP` initialized to `0x07`, pre-increment on `PUSH`, post-decrement on `POP` with underflow protection at `0x07`) alongside a 16-slot circular FIFO buffer (`(rear + 1) % 16`) validated by `programs/QueueTest.java`.

---

## 6. Phase 3: Multi-Process Architecture and Socket IPC (Meeting 4)

- **Meeting 4:** 28 September 2026 (3 hours)

### Three-Process Topology
The simulator was decoupled into three operating system processes communicating over local TCP loopback sockets (`127.0.0.1`):

| Process | Source File | TCP Port | Primary Responsibility |
| --- | --- | :---: | --- |
| **LoggerProcess** | `src/LoggerProcess.java` | `5001` | Listens for structured `LOG|LEVEL|CAT|MSG` frames and persists timestamped entries to `simulator.log` via `synchronized LogHandler.writeLog()`. |
| **CoreProcess** | `src/CoreProcess.java` | `5000` | Hosts the CPU FDE engine, RAM, stack, FIFO queue, and CRC-24b lab; processes UI commands (`STEP`, `RUN`, `RESET`, `LOAD`, `CRC`, `CRC_TEST`, `STATE`) and queues audit logs via a bounded non-blocking `ArrayBlockingQueue(1024)`. |
| **UIProcess** | `src/UIProcess.java` | `8080` | Serves the 7-panel interactive Web Dashboard (`index.html`), handles `/api/cmd?cmd=...` requests, and provides a CLI controller alongside the `SimulatorUI.java` Swing desktop client. |

---

## 7. Phase 4: Branch Audit, Bug Resolution, and Integration

- **Period:** 26 October – 2 November 2026

### Resolved Integration Issues

| Subsystem | Issue Identified | Resolution Implemented |
| --- | --- | --- |
| **CPU / ALU (`CPU.java`, `PSW.java`)** | Signed Java byte promotion corrupted `SUBB` and `ADD` flags; `DIV AB` with `B=0` risked throwing `ArithmeticException`. | Applied `& 0xFF` masking on all operands; implemented 8051 hardware `DIV AB` specification (`B=0` sets `OV=1, CY=0` without crashing). |
| **Stack & Memory (`DataStructures.java`, `Memory.java`)** | `pop()` allowed decrementing below reset base `0x07`; reloading a shorter program left trailing bytes in `codeMemory`. | Added underflow guard at `SP <= 0x07` (`fix(stack)` commit) and zero-filled `codeMemory` inside `loadProgram()`. |
| **Core Concurrency (`CoreProcess.java`)** | Synchronous socket logging risked blocking the CPU thread if `LoggerProcess` stalled and TCP buffers filled. | Added a bounded 1,024-slot `ArrayBlockingQueue<String>` (`offer()`) with atomic drop counter (`droppedLogs`) and daemon drain thread. |
| **Web UI (`UIProcess.java`)** | Hex bytecode strings containing spaces failed to parse over HTTP query parameters. | Added `URLDecoder.decode(query, StandardCharsets.UTF_8)` in the `/api/cmd` handler and removed stray duplicate files (`LoggerProcessgfhhdff.java`). |

---

## 8. Phase 5: 3GPP LTE CRC-24b Lab, Benchmarking, and Final Verification (Meeting 5)

- **Meeting 5:** 3 November 2026, 10:00 PM – 11:52 PM (Google Meet `hbo-ojhk-fzg`)

### 1. 3GPP TS 36.212 CRC-24b Implementation (`src/CRC24b.java`)
- Implemented the 3GPP LTE-B generator polynomial $g_{\text{CRC24B}}(D) = D^{24} + D^{23} + D^6 + D^5 + D + 1$ (`0x800063`).
- `CRC24b.step8051Hardware()` models the 8-bit register constraint by shifting bit-by-bit through the carry bit across `R7 -> R6 -> R5` and XORing `0x80`, `0x00`, and `0x63` when the MSB shifted out of `R5` is `1`.
- Verified standard test vector `"123456789"` $\rightarrow$ remainder `0x23EF52` (`R5=0x23`, `R6=0xEF`, `R7=0x52`), clean codeword syndrome `0x000000`, 1-bit error syndrome `0x520000` (byte 0, bit 0), and 2-bit adjacent burst syndrome `0xF60000` (byte 0, bits 0 & 1).

### 2. Automated Verification Summary (34/34 Passing Assertions)
- **`TestCPU.java`:** **17/17 PASS** (Data Transfer `MOV`, Arithmetic `ADD`/`INC`/`MUL`/`DIV`, Logic `ANL`, Control Flow `SJMP +1` and `SJMP $` [`80 FE`], Stack `PUSH ACC`, Queue `ENQUEUE`/`DEQUEUE`, and `HALT`).
- **`TestCRC24b.java`:** **8/8 PASS** (3GPP vector `"123456789"`, single-byte `"A"`, `R5:R6:R7` decomposition, `step8051Hardware()` equivalence, codeword encoding/decoding, 1-bit error, 2-bit burst error, empty payload).
- **`QueueTest.java`:** **3/3 PASS** (Assembly-level FIFO enqueue/dequeue ordering verification for `10, 30, 5`).
- **`LoggerProcessTest.java`:** **6/6 PASS** (Socket connection on Port 5001, `EXEC` log, `CRC` log, malformed frame handling, disk persistence in `simulator.log`, and `1,500`-frame non-blocking backpressure burst test completing in `11.30 ms`).

### 3. Benchmark Results (`programs/BenchmarkCRC.java`)
- **Environment:** Windows 11 x64, OpenJDK 26.0.2, 200-iteration JIT warm-up, 49-byte telecom payload.
- **Standalone In-Memory Baseline (1,000 iterations):** `~1.0 ms` total (`0.001 ms/pkt`, checksum `0x025A3C`).
- **Multi-Process TCP Loopback IPC (100 stop-and-wait round trips on Port 5000):** `66.6 ms` total (`0.666 ms/msg` average RTT, `~1,500 msgs/sec`), demonstrating sub-millisecond interactive latency alongside complete OS process isolation.
