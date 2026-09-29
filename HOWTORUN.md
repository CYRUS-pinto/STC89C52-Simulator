# How to Run the STC89C52 Simulator

**Team Bondaas** — Cyrus | Arnold | Shashidhara | Chrisel

---

## Requirements

- **Java 11+** installed (check with `java -version`)
- **3 terminal windows** (Command Prompt or PowerShell on Windows)

---

## Step 1 — Compile Everything

Open a terminal in the `STC89C52-Simulator/` folder and run:

```powershell
# Windows (PowerShell)
javac -cp src src\CPU.java src\PSW.java src\Registers.java src\Memory.java src\DataStructures.java src\LoggerProcess.java src\CoreProcess.java src\SimulatorUI.java
```

Or to also compile tests and programs:
```powershell
javac -cp src src\*.java
javac -cp src tests\TestCPU.java
javac -cp src programs\QueueTest.java
```

---

## Step 2 — Start the Three Processes (in order!)

### ⚠️ Order matters — Logger must start before Core, Core before UI.

**Terminal 1 — Logger Process (Arnold's piece)**
```powershell
java -cp src LoggerProcess
```
Expected output:
```
[Logger] Starting on port 5001 ...
[Logger] Waiting for CoreProcess to connect...
```

**Terminal 2 — Core Process (Chrisel's piece)**
```powershell
java -cp src CoreProcess
```
Expected output:
```
[Core] Connecting to LoggerProcess on port 5001...
[Core] Server listening for UIProcess on port 5000...
```

**Terminal 3 — UI (Shashidhara's piece)**
```powershell
java -cp src SimulatorUI
```
A Swing window will open. Status bar should say "Connected to CoreProcess ✓".

---

## Step 3 — Use the Simulator

| Button | What it does |
|--------|-------------|
| **⬆ Load** | Resets CPU and reloads the built-in demo program |
| **⏭ Step** | Executes ONE instruction (shows FETCH→DECODE→EXECUTE) |
| **▶ Run** | Runs all instructions until HALT |
| **↺ Reset** | Resets CPU state and reloads program |
| **✕ Quit** | Closes UI and shuts down Core |

The **Execution Log** panel on the right shows each step with PC, A, flags, SP, and queue size.  
The **FETCH / DECODE / EXECUTE** indicators animate (✓) each time a step runs.

---

## Step 4 — Run the Tests

```powershell
# From STC89C52-Simulator/
java -cp src;tests TestCPU
```

Expected output ends with:
```
ALL TESTS PASSED ✓
```

---

## Step 5 — Run the Queue Validation Program

```powershell
java -cp src;programs QueueTest
```

Expected output ends with:
```
Queue validation PASSED ✓ — FIFO ordering correct.
```

---

## Log File

After running, check `simulator.log` in the `STC89C52-Simulator/` folder for a full timestamped execution log.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| "Connection refused" on Core | Make sure LoggerProcess is running first |
| "Connection refused" on UI | Make sure CoreProcess is running first |
| `javac: error: file not found` | Check you are in the `STC89C52-Simulator/` directory |
| UI shows "Waiting for CoreProcess..." forever | Core crashed — check Terminal 2 for errors |

---

## Project Structure

```
STC89C52-Simulator/
├── README.md               ← Project overview
├── HOWTORUN.md             ← This file
├── src/
│   ├── CPU.java            ← Fetch/Decode/Execute engine
│   ├── Registers.java      ← A, B, R0-R7 register file
│   ├── PSW.java            ← CY, AC, OV, P flags
│   ├── Memory.java         ← Code memory + internal RAM
│   ├── DataStructures.java ← Stack + Circular Queue
│   ├── CoreProcess.java    ← Core IPC process (port 5000)
│   ├── SimulatorUI.java    ← Swing UI process (client→5000)
│   └── LoggerProcess.java  ← Logger IPC process (port 5001)
├── tests/
│   └── TestCPU.java        ← Unit tests (14 test cases)
├── programs/
│   └── QueueTest.java      ← Queue FIFO validation program
└── docs/
    ├── decisions/decision-log.md
    ├── queue-flowchart.md
    └── weekly-status/
        ├── Week 1 .md
        ├── Week2.md
        ├── Week3.md
        └── Week4.md
```
