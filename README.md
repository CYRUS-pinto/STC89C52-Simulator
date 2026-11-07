# STC89C52 Microcontroller Simulator

## Final System Overview (Weeks 1–5 Completed Architecture)
An educational, instruction-accurate **three-process software simulator** for a **19-opcode subset of the STC89C52 (Intel MCS-51) 8-bit microcontroller**, featuring decoupled TCP loopback inter-process communication (`Ports 5000, 5001, 8080`), a bounded non-blocking `ArrayBlockingQueue(1024)` log buffer, an interactive 7-panel Web Dashboard, and a **3GPP TS 36.212 CRC-24b (`g(D) = 0x800063`) telecommunications error-detection lab** operating on simulated 8-bit registers `R5`, `R6`, and `R7`.

- **Three Independent OS Processes (`src/`):**
  - **`LoggerProcess` (`Port 5001` — Arnold Noah Karyappa, `25190106`):** Append-only TCP logging daemon persisting execution traces to `simulator.log` via thread-safe `synchronized LogHandler.writeLog()`.
  - **`CoreProcess` (`Port 5000` — Chrisel Lobo, `25190110`):** 19-opcode Fetch-Decode-Execute CPU engine (`CPU.java`), 8 KB Code ROM, 256-byte Internal RAM (`Memory.java`), 4 Register Banks (`R0`–`R7`), `PSW` status flags (`CY, AC, OV, P`), hardware LIFO Stack (`SP=07H` reset), 16-slot circular FIFO ring buffer (`DataStructures.java`), and non-blocking log queue (`ArrayBlockingQueue(1024)`).
  - **`UIProcess` (`Port 8080` — Shashidhara Poojari, `25190148`):** Embedded HTTP server serving the 7-panel Web Dashboard (`index.html`), `/api/cmd` REST-style control bridge, and `SimulatorUI.java` Swing desktop client.
  - **System Architecture, Socket IPC, CRC-24b Engine & Benchmarking (`Cyrus Shobith Pinto` — Team Lead, `25190111`):** Multi-process socket orchestration, `CRC24b.step8051Hardware()` across `R5:R6:R7`, `BenchmarkCRC.java`, and full-system integration.
- **Automated Verification (`38/38` Passing Assertions):**
  - `TestCPU.java` (`20/20 PASS`), `TestCRC24b.java` (`9/9 PASS`, including exhaustive 96-bit 1-bit & 2-bit codeword sweep across 4,656 error patterns), `QueueTest.java` (`3/3 PASS`), and `LoggerProcessTest.java` (`6/6 PASS`, including `CountDownLatch` stalled-logger CPU step fault-isolation verification).
- **Quick Start & Full Documentation:** See [`HOWTORUN.md`](HOWTORUN.md) and [`docs/PBL_Report.pdf`](docs/PBL_Report.pdf).

---

## Initial Week 1 Problem Objective & Planning Archive
Develop a STC89C52 microcontroller simulator for instruction execution memory and peripheral operations and process management.This will help the student visualize the program and understand helping users visualize how programs execute and how CPU scheduling manages multiple programs.

## Problem Statement
Design and implement a software based simulator for the assigned 8-bit microcontroller, the STC89C52.The simulator will simulate the essential processor components and executes the instructions, manage program memory, data memory and stack, and provide simplified GPIO, timer, and interrupt functionality. It will also support multiple programs as processes using a Process Control Block (PCB), a ready queue, context switching, and the FCFS, Round Robin, and Priority CPU scheduling algorithms.

## Project Scope
- Simulate core CPU behavior of the STC89C52.
- Execute a defined subset of 8051 instructions with correct state updates.
- Simulate memory (program/data/stack), GPIO, timers, and interrupts.
- Represent multiple loaded programs as OS-style processes with PCBs and state transitions.
- Implement data structures: stack, queue, circular queue, instruction lookup table.
- Implement FCFS, Round Robin, and Priority scheduling with context switching.
- Provide an interactive UI: load, run, reset, single-step.
- Report performance metrics: waiting time, turnaround time, response time, context switches, CPU utilization.
- **Out of scope:** full/complete 8051 instruction set and cycle-accurate hardware emulation — only the subset needed to demonstrate the above.

## Microcontroller Being Simulated
**STC89C52** an enhanced 8051 family 8-bit microcontroller (4 KB+ internal RAM variant, standard 8051 register/instruction architecture, 4 GPIO ports, 3 timers/counters, standard 8051 interrupt structure).

## Team
**Team Name:** Bondaas

| Role | Name | Roll No | Contact |
|---|---|---|---|
| Team Leader | Cyrus Shobith Pinto | 25190111 | — |
| Member | Arnold Noah | 25190106 | 8951619591 |
| Member | Shashidhara | 25190148 | 7411004525 |
| Member | Chrisel Lobo | 25190110 | 9591716088 |

## Team Responsibilities

| Member | Primary Responsibility | Supporting Responsibility |
|---|---|---|
| Cyrus (Team Leader) | CPU & Instruction Execution — **Interrupt mechanism**; overall integration | GitHub repo administration, coordination, task balancing |
| Arnold Noah | Memory & Stack topics — **CPU/register organization, Status/flag info, Instruction categories, GPIO** | CPU support |
| Shashidhara | Data Structures & Process Management — **Memory organization, Program Counter** | Testing |
| Chrisel Lobo | OS Scheduling & Context Switching — **Timer, Stack mechanism / Stack pointer** | UI & Integration |


## Selected Programming Language
**Java**

- **Reason for selection:** Assigned as the preferred language for this project; strong OOP support suits modeling CPU/registers/memory/processes as classes; team has prior Java familiarity from coursework.
- **Advantages for this project:** Clear class-based modeling of PCB, registers, and memory; built-in collections (`Queue`, `Deque`, `PriorityQueue`) map directly onto FCFS/Round Robin/Priority scheduling; strong tooling for a Swing/JavaFX or web-based (Spring) visualization layer.
- **Limitations:** More verbose than scripting languages for quick prototyping; no low-level bit/memory-mapped register access as direct as C, so register/flag simulation is done via wrapper classes instead of raw memory.
- **Alternative languages considered:** None finalized — Java is being used as assigned and also add more tools in the future.

## Initial System Architecture

```mermaid
flowchart TD
    UI[User Interface<br/>Load / Run / Reset / Step]
    SCHED[Scheduler<br/>FCFS · Round Robin · Priority]
    PM[Process Manager<br/>PCB · Ready Queue · Context Switch]
    MEM[Memory<br/>Program / Data / Stack]
    CPU[CPU<br/>Registers · PC · Flags · Instruction Execution]
    PERIPH[Peripherals]
    GPIOT[GPIO / Timer]
    INT[Interrupts]

    UI --> SCHED
    SCHED --> PM
    PM --> CPU
    PM --> MEM
    CPU --> MEM
    CPU --> PERIPH
    PERIPH --> GPIOT
    PERIPH --> INT
    INT --> CPU
```


## Initial Development Plan

| Week | Focus |
|---|---|
| 1 | Team formation, repo setup, STC89C52 architecture study, language finalization, initial design |
| 2 | CPU model: registers, PC, flags, memory representation; start instruction execution |
| 3 | Stack, GPIO, timer, interrupt simulation |
| 4 | Process/PCB model, ready queue, context switching |
| 5 | Scheduling algorithms: FCFS, Round Robin, Priority |
| 6 | UI: load/run/reset/step, integration |
| 7 | Performance metrics (waiting/turnaround/response time, CPU utilization) |
| 8 | Testing, documentation, final polish |


## Git Workflow

- Every Monday: pull latest `main`, merge into the weekly branch, open PRs from individual branches into `week-01`.
- After review + testing + documentation, `week-01` is merged into `main`.

```mermaid
flowchart LR
    main[main] --> week01[week-01]
    week01 --> b1[cyrus/week1-interrupts]
    week01 --> b2[arnold/week1-cpu-registers]
    week01 --> b3[shashidhara/week1-memory]
    week01 --> b4[chrisel/week1-stack-timer]
    b1 --> review[Review]
    b2 --> review
    b3 --> review
    b4 --> review
    review --> testing[Testing]
    testing --> docs[Documentation]
    docs --> merge[Merge]
    merge --> main
```

## Communication
- **GitHub Issues** — technical questions, bugs, design concerns, tasks, decisions requiring discussion.
- **Google Classroom / shared Google Docs (college email only)** — meeting minutes, weekly status reports, formal documentation.
- **Miro** — shared board for planning and task breakdown.
- **Google Chat (college email only)** — day-to-day coordination among members.
- **Google Meet (with transcription)** — meetings are recorded and transcribed to support accurate minutes.

## Repository Structure
```
STC89C52-Simulator/
├── README.md
├── .gitignore
├── docs/
│   ├── meetings/
│   ├── decisions/
│   └── weekly-status/
├── src/
├── tests/
└── programs/
```
