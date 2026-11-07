# Architecture & Project Decision Log

## Decision 001 - Programming Language Selection
**Date:** 22 August 2026  
**Status:** Closed  

**Options considered:** Standard Java (JDK) vs. C/C++ or Python.  
**Decision:** Standard Java selected.  
**Reason:** Java provides clean object-oriented encapsulation for modeling microcontroller hardware components (`CPU`, `Registers`, `Memory`, `PSW`, `DataStructures`), cross-platform TCP socket networking (`java.net.Socket`, `ServerSocket`), and built-in concurrency primitives (`ArrayBlockingQueue`, `Executors`, `synchronized` monitors) without requiring external third-party runtime dependencies.

---

## Decision 002 - Week 1 Hardware Architecture Topic Allocation
**Date:** 22 August 2026  
**Status:** Closed  

**Problem:** The 10 required STC89C52 architectural study topics needed to be distributed across the 4 team members balanced by technical complexity.  
**Decision:**
- **Shashidhara Poojari:** Memory organization (`8 KB` code ROM, `256 B` internal RAM) and Program Counter (`PC`).
- **Chrisel Lobo:** Timer/Counter organization and Hardware Stack mechanism (`SP` initialized at `07H`).
- **Cyrus Shobith Pinto:** Interrupt organization, multi-process system architecture, and integration coordination.
- **Arnold Noah Karyappa:** CPU/register organization (`R0`–`R7` across 4 banks), Program Status Word (`PSW`), instruction categories, and GPIO ports (`P0`–`P3`).

---

## Decision 003 - Verification Against STC89C52 / MCS-51 Hardware Specifications
**Date:** 27 August 2026  
**Status:** Closed  

**Problem:** Ensure the Java simulator accurately reflects physical STC89C52/8051 register and flag behavior rather than generic arithmetic approximations.  
**Decision:** Ground every implemented opcode and register transition in the Intel MCS-51 Architectural Manual and STC89C52RC datasheet:
- Apply `& 0xFF` bitwise masking across all 8-bit registers and memory cells to prevent Java 32-bit signed integer sign-extension.
- Model `DIV AB` (`0x84`) divide-by-zero behavior according to hardware specification (setting `OV=1, CY=0` without throwing an unhandled runtime exception).
- Support both synthetic `0xFF` (`HALT`) and the standard 8051 `SJMP $` (`80 FE`) self-loop halt idiom.

---

## Decision 004 - Decoupled Three-Process TCP Loopback Architecture
**Date:** 28 September 2026  
**Status:** Closed  

**Problem:** Monolithic simulators couple GUI rendering and disk file logging directly to the CPU execution loop, allowing UI stalls or slow disk writes to block instruction execution.  
**Decision:** Separate the simulator into three operating system processes communicating over local TCP loopback sockets (`127.0.0.1`):
1. `LoggerProcess` (`Port 5001`): Thread-safe append-only file logger writing to `simulator.log`.
2. `CoreProcess` (`Port 5000`): 8051 CPU engine, memory, stack, FIFO queue, and CRC-24b engine with a bounded non-blocking `ArrayBlockingQueue<String>(1024)` log dispatcher.
3. `UIProcess` (`Port 8080`): Embedded HTTP server (`GET /api/cmd`) serving the 7-panel Web Dashboard alongside the CLI and Swing desktop clients.

---

## Decision 005 - 3GPP TS 36.212 CRC-24b Multi-Register Implementation
**Date:** 3 November 2026  
**Status:** Closed  

**Problem:** Execute the allocated 24-bit 3GPP LTE-B cyclic redundancy check ($g_{\text{CRC24B}}(D) = D^{24} + D^{23} + D^6 + D^5 + D + 1$, `0x800063`) within an 8-bit microcontroller register model.  
**Decision:** Partition the 24-bit shift register across Working Register Bank 0 registers `R5` (bits 23–16), `R6` (bits 15–8), and `R7` (bits 7–0) in `CRC24b.step8051Hardware()`, shifting bit-by-bit through the carry bit across `R7 -> R6 -> R5` and applying $GF(2)$ XOR masks (`0x80`, `0x00`, `0x63`) when the MSB shifted out of `R5` is `1`.
