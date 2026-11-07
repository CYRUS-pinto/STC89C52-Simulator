# How to Run the STC89C52 Simulator

**Team 7 (Bondaas)** — Cyrus Shobith Pinto (Lead - 25190111) | Chrisel Lobo (Core - 25190110) | Arnold Noah Karyappa (Logger - 25190106) | Shashidhara Poojari (UI - 25190148)

---

## 1. System Requirements

- **Java JDK 11+** installed (`java -version` and `javac -version`)
- **3 Terminal Windows** (Command Prompt or PowerShell on Windows / Bash on Linux/macOS)
- **Web Browser** (Chrome, Edge, Firefox, or Safari for the Web Dashboard)

---

## 2. Compilation (Step 1)

Open a terminal in the `STC89C52-Simulator/` root folder and compile all source files, unit tests, and benchmark suites into the `bin/` directory:

```powershell
# Windows (PowerShell or CMD)
javac -d bin -cp src src\*.java tests\*.java programs\*.java
```

> **Alternative (In-place compilation):**
> ```powershell
> javac -cp src src\*.java tests\*.java programs\*.java
> ```

---

## 3. Starting the 3-Process Architecture (Step 2)

> [!IMPORTANT]
> **Process Startup Sequence Matters!**  
> 1. **Logger** starts first on TCP port `5001` (waits for Core).
> 2. **Core** starts second on TCP port `5000` (connects to Logger, listens for UI).
> 3. **UI** starts third on HTTP port `8080` (connects to Core).

### Terminal 1 — Logger Process (Arnold's Subsystem)
```powershell
java -cp bin LoggerProcess
```
*Expected Terminal Output:*
```
[Logger] Starting on port 5001 ...
[Logger] Waiting for CoreProcess to connect...
```

---

### Terminal 2 — Core Process (Chrisel's Subsystem)
```powershell
java -cp bin CoreProcess
```
*Expected Terminal Output:*
```
[Core] Connecting to LoggerProcess on port 5001...
[Core] Connected to LoggerProcess.
[Core] Server listening for UIProcess on port 5000...
```

---

### Terminal 3 — UI Process (Shashidhara's Subsystem)

#### Option A: Web Dashboard + CLI (Primary / Recommended)
```powershell
java -cp bin UIProcess
```
*Expected Terminal Output:*
```
[UI] Connected to CoreProcess on port 5000.
[UI] Embedded Web Dashboard running at http://localhost:8080/
Type commands: STEP, RUN, STOP, RESET, LOAD <hex>, CRC <text>, QUIT
```

- Open your browser to **`http://localhost:8080/`**.
- You will see the **6-Panel Architecture Dashboard** plus the **3-Stage CRC-24b Error Detection Lab**.

#### Option B: Swing Desktop GUI (Alternative)
```powershell
java -cp bin SimulatorUI
```
- Opens a native Java Swing desktop window with live register displays, execution log, and Step/Run controls.

---

## 4. Web Dashboard Features & How to Use (Step 3)

The web dashboard is laid out in 6 symmetrical silicon panels followed by the 3GPP LTE telecommunications lab:

### 6 Silicon Panels

| Panel | Name | Key Functionality |
|:---:|---|---|
| **1** | **Program Memory & Disassembly** | Editable hex input box on top. Disassembles hex bytes into 8051 mnemonics (`MOV A, #data`, `MUL AB`, `PUSH ACC`) with address, bytes, and active instruction pointer `►` tracking $PC$. |
| **2** | **CPU Pipeline** | Displays real-time **Fetch**, **Decode**, and **Execute** stages with cycle changes. |
| **3** | **Working Registers** | 8051 register file: $PC, SP, ACC, B$, General Registers $R_0$ through $R_7$, and PSW flags (`CY`, `AC`, `OV`, `P`). |
| **4** | **Hardware Stack** | Vertical LIFO stack frame visualizer tracking the Stack Pointer ($SP$) from reset (`0x07`) upwards into Bank 1 / Scratchpad RAM. |
| **5** | **Message Queue** | $8 \times 2$ circular FIFO buffer (16 slots) displaying head and tail pointer badges (`FRONT` and `REAR`). |
| **6** | **Internal RAM (256B)** | Complete $16 \times 16$ byte matrix color-coding Register Banks (`00H–1FH`), Bit-Addressable RAM (`20H–2FH`), and General Scratchpad with a gold outline highlighting Top-of-Stack (`[SP Top]`). |

### 5 One-Click Silicon Presets

Click any preset button above the panels to instantly load and disassemble standard 8051 programs:
1. **`1: Arithmetic (MUL)`**: Loads `74 06 75 F0 07 A4 FF` $\to$ Multiplies $6 \times 7 = 42$ (`ACC = 0x2A`).
2. **`2: FIFO Queue`**: Loads `74 0A E0 74 1E E0 74 05 E0 D0 D0 D0 FF` $\to$ Enqueues 3 values and dequeues them in strict First-In First-Out order.
3. **`3: Logic (ANL)`**: Loads `74 AB 54 0F FF` $\to$ Bitwise AND masking with immediate data (`0xAB & 0x0F = 0x0B`).
4. **`4: Stack (PUSH)`**: Loads `74 42 C0 E0 74 99 C0 E0 FF` $\to$ Demonstrates stack growth and $SP$ advancement from `0x07` to `0x08` and `0x09`.
5. **`5: CRC-24b`**: Loads the 8051 assembly sequence for the 3GPP LTE generator polynomial.

### 3-Stage 3GPP LTE-B CRC-24b Telecom Lab

Located below the 6 panels:
- **Stage 1 (Transmitter):** Type any ASCII payload (default `"123456789"`) and click **Calculate CRC-24b**. Computes the 24-bit remainder (`0x23EF52`) using generator polynomial $D^{24} + D^{23} + D^6 + D^5 + D + 1$ (`0x800063`) and decomposes it into working registers:
  - **`R5`** = High Byte (`0x23`)
  - **`R6`** = Mid Byte (`0xEF`)
  - **`R7`** = Low Byte (`0x52`)
- **Stage 2 (Air Interface Codeword):** Displays the raw transmitted codeword stream dividing the data payload from the 24-bit CRC parity.
- **Stage 3 (Receiver Syndrome Checks):**
  - Click **Verify Clean Channel**: Receiver re-evaluates codeword $\to$ Syndrome **`0x000000` (PASS / Zero Error)**.
  - Click **Inject 1-Bit Noise**: Inverts bit 0 of the first byte $\to$ Non-zero syndrome **`0x520000` (1-Bit Error Detected)**.
  - Click **Inject 2-Bit Burst Noise**: Inverts bits 0 & 1 $\to$ Non-zero syndrome **`0xF60000` (2-Bit Burst Error Detected)**.

---

## 5. Running the Automated Test Suites (Step 4)

All unit tests and validation programs run without requiring external dependencies:

### Test Suite 1: 8051 CPU Opcode Unit Tests (17 Tests)
```powershell
java -cp bin TestCPU
```
*Expected Output:*
```
Results: 17 PASSED, 0 FAILED out of 17 tests
ALL TESTS PASSED ✓
```

---

### Test Suite 2: CRC-24b Algorithm & Register Mapping Tests (8 Tests)
```powershell
java -cp bin TestCRC24b
```
*Expected Output:*
```
Results: 8 PASSED, 0 FAILED out of 8 tests
ALL CRC-24b TESTS PASSED SUCCESSFULLY! (100%)
```

---

### Test Suite 3: Circular FIFO Buffer Validation
```powershell
java -cp bin QueueTest
```
*Expected Output:*
```
3/3 dequeue operations verified FIFO order.
Queue validation PASSED ✓ — FIFO ordering correct.
```

---

### Test Suite 4: Empirical Benchmark & IPC Latency Audit
```powershell
java -cp bin BenchmarkCRC
```
*Expected Output:*
- Measures standalone in-memory throughput (~1,000,000 pkts/sec at 0.001 ms/pkt).
- Measures multi-process POSIX socket IPC throughput (~1,500 msgs/sec at ~0.66 ms round-trip).
- Outputs the **Architectural Tradeoff Table** (Throughput vs. Process Fault Isolation).

---

## 6. Audit Trail & Log Inspection

`LoggerProcess` writes an append-only, thread-safe audit trail to **`simulator.log`** in the root directory:
- Every instruction execution with timestamp, level (`INFO`, `EXEC`, `LOAD`, `HALT`, `RESET`), and CPU register dump.
- Inspect recent logs:
  ```powershell
  # Windows PowerShell
  Get-Content simulator.log -Tail 20

  # Linux / macOS
  tail -n 20 simulator.log
  ```

---

## 7. Hardware & Architectural Edge-Case Verification

When testing boundary conditions on the simulator, each hardware edge case is handled as follows:

| Test Scenario | Input / Action | Hardware Silicon Reality | How Simulator Demonstrates It |
|---|---|---|---|
| **Divide by Zero** | Run `84` (`DIV AB`) with $B = 0$ | 8051 hardware does not crash; it sets $OV=1$ in PSW. | Handled safely; accumulator remains intact and `OV` flag turns green. |
| **Hardware Multiplication** | Load Preset 1 (`MUL AB`: $6 \times 7$) | Product stored in $A$ (low) and $B$ (high). | `ACC=0x2A` ($42$), `B=0x00`, `OV=false`. |
| **FIFO Buffer Overflow** | Push 17 items into 16-slot queue | Hardware circular queue caps at maximum capacity. | Capped safely at 16 elements without index out of bounds. |
| **FIFO Buffer Underflow** | Dequeue from empty queue | Circular buffer handles underflow safely. | Dequeues safely, logs underflow condition without throwing exception. |
| **Stack Bank Collision** | Push past $SP=07H$ | $SP$ advances into Bank 1 (`0x08`) unless relocated. | Stack Visualizer tracks $SP$ ascending from `0x07` $\to$ `0x08` $\to$ `0x09`. |
| **Past-HALT Stepping** | Click Step after opcode `FF` | CPU is in halted state; $PC$ must not advance into garbage. | CPU remains safely halted; $PC$ freezes at halt address. |
| **Process Fault Isolation** | Close browser tab or kill UI | Core CPU state is preserved in independent OS process. | Core and Logger continue running on ports 5000/5001 uninterrupted. |

---

## 8. Troubleshooting

| Symptom | Cause | Solution |
|---|---|---|
| `Connection refused: connect` in CoreProcess | LoggerProcess was not started first | Start `LoggerProcess` in Terminal 1 before launching `CoreProcess`. |
| `Connection refused: connect` in UIProcess | CoreProcess was not started first | Start `CoreProcess` in Terminal 2 before launching `UIProcess`. |
| `Address already in use: JVM_Bind` (Port 5000/5001/8080) | A previous simulator instance is still running in background | Run `Get-Process java \| Stop-Process -Force` in PowerShell to kill dangling instances. |
| `javac: file not found: *.java` | Terminal working directory is not inside `STC89C52-Simulator/` | Run `cd STC89C52-Simulator` before compiling. |
| Browser shows `Cannot connect to localhost:8080` | UIProcess HTTP server is not running | Ensure Terminal 3 shows `Embedded Web Dashboard running at http://localhost:8080/`. |

---

## 9. Project Directory Layout

```
STC89C52-Simulator/
├── README.md                                 ← Project architectural overview
├── HOWTORUN.md                               ← This run and verification manual
├── index.html                                ← Interactive Web Dashboard (widescreen, 1560px)
├── bin/                                      ← Compiled .class binaries
├── src/
│   ├── CPU.java                              ← 8051 Fetch/Decode/Execute ALU engine
│   ├── Registers.java                        ← Register file (A, B, R0-R7, PC, SP)
│   ├── PSW.java                              ← Program Status Word flags (CY, AC, OV, P)
│   ├── Memory.java                           ← 8KB Code ROM & 256-Byte On-Chip RAM
│   ├── DataStructures.java                   ← Circular FIFO Buffer & Stack Manager
│   ├── CRC24b.java                           ← 3GPP LTE-B CRC Engine (0x800063)
│   ├── CoreProcess.java                      ← CPU Core TCP Server (Port 5000)
│   ├── LoggerProcess.java                    ← Audit Logger TCP Server (Port 5001)
│   ├── LogHandler.java                       ← Synchronized Append-Only File Appender
│   ├── LogMessage.java                       ← Log frame data structure
│   ├── UIProcess.java                        ← HTTP Server (Port 8080) & CLI Bridge
│   ├── SimulatorUI.java                      ← Swing Desktop GUI Client
│   └── index.html                            ← Source copy of Web Dashboard
├── tests/
│   ├── TestCPU.java                          ← 17 CPU Opcode verification tests
│   ├── TestCRC24b.java                       ← 8 CRC-24b telecom verification tests
│   └── LoggerProcessTest.java                ← 6 socket & backpressure logger tests
├── programs/
│   ├── QueueTest.java                        ← Circular FIFO ordering verification
│   └── BenchmarkCRC.java                     ← Standalone vs Multi-Process benchmark
├── docs/
│   ├── PBL_Report.pdf                        ← Final 9-Page Project Report (PDF)
│   ├── PBL_Report.docx                       ← Final 9-Page Project Report (DOCX)
│   ├── decisions/                            ← Architecture decision log
│   ├── meetings/                             ← Meeting minutes (Meetings 1–5)
│   └── weekly-status/                        ← Week 1 - Week 4 progress trackers
└── simulator.log                             ← Generated audit trail log file
```
