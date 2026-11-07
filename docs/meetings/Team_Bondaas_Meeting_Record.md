# Team Bondaas: Project Meeting Record and Work Log

## STC89C52 Microcontroller Multi-Process Simulator

This document records the team's planning, technical discussions, development approach, individual responsibilities, integration work, and final verification activities. It covers the project timeline from the initial kickoff on 22 August 2026 through the branch audit and integration work completed on 2 November 2026.

The record combines the original meeting notes with the later development chronology supplied by the team. Details that were not provided are marked **[to confirm]** rather than assumed.

---

## 1. Team

| Member | Role / Responsibility |
| --- | --- |
| Cyrus Shobith Pinto | Team Leader: architecture planning, task allocation, guidance, integration, system testing and progress tracking |
| Shashidhara | UI Process |
| Chrisel Lobo | Core Process |
| Arnold Noah | Logging Process |

## 2. Project overview

The project is an educational simulator for the STC89C52 8-bit microcontroller. It combines microcontroller architecture, data structures and operating-system concepts to demonstrate processor execution and the coordination of multiple programs/processes.

The project began with the planning and implementation of the microcontroller simulator in Java. During development, the architecture evolved into a multi-process design with separate UI, Core and Logging processes communicating through sockets.

## 3. Chronological project summary

| Period | Phase | Main activities and outcomes |
| --- | --- | --- |
| 22–28 August 2026 | Kickoff and project planning | Team formation, problem breakdown, architecture planning, task allocation and selection of Java |
| 7–8 September 2026 | Hardware architecture and implementation concepts | Research into 8051/STC89C52 registers, PSW, memory organisation, 8-bit values, bit masks, stack behaviour and data structures |
| 28–30 September 2026 | Multi-process architecture | Development of the three-process approach, socket communication, CPU opcode tests and UI/backend communication |
| 14–16 October 2026 | Development approach and viva preparation | Shift away from unverified AI-generated code, emphasis on understanding and explaining code, and preparation for detailed technical questioning |
| 26 October–2 November 2026 | Branch audit, integration and verification | Review of member branches, correction of integration issues, merge reconciliation and final verification activities |
| 3 November 2026 | Meeting 5: CRC-24b derivation, viva preparation and final delivery | Mathematical derivation of 3GPP LTE-B CRC-24b (0x800063), 8-bit register decomposition (R5, R6, R7), report restructuring (Detailed vs. Short), member branch synchronization, and viva defense rehearsals |

---

## 4. Phase 1: Kickoff, language selection and task allocation

**Key date:** 22 August 2026  
**Further planning:** 27–28 August 2026  
**Meeting details:** Kickoff Google Meet, 22 August 2026, 9:09 PM (meeting code: qwi-axoh-wmh); Miro board used for architecture planning.

### Agenda

1. Understand and break down the project problem statement.
2. Decide the implementation language.
3. Plan the simulator's major components.
4. Divide responsibilities among team members.
5. Establish the initial development and Git workflow.

### Discussion and research

- The team discussed the requirements of an educational STC89C52 microcontroller simulator.
- Java was selected, with a pure JDK approach (Java 17/21) preferred over adding external frameworks.
- The initial architecture divided the work into decoupled areas so that members could develop and test their responsibilities separately.
- The team considered how to represent the 8-bit processor's registers, memory, stack and execution behaviour in software.

### Initial task allocation

The responsibilities were organised around the following areas:

| Member | Initial area of responsibility |
| --- | --- |
| Arnold Noah | Registers and PSW |
| Shashidhara | Memory organisation |
| Chrisel Lobo | Stack, queues and related data structures |
| Cyrus Shobith Pinto | Integration, scheduling, architecture guidance and coordination |

The later process-based responsibilities are recorded in Section 11.

### Decisions

1. Java was selected as the implementation language.
2. Each member was expected to research and develop their assigned area.
3. The team would use a shared GitHub repository for collaboration.
4. Members would work on personal branches, commit and push their work, and avoid making direct edits on `main`. Integration would be handled through review and merging.

### Outcome

The team was formed, the initial architecture was discussed, topics were distributed, and the project repository structure was planned. The initial README, `.gitignore` and folder structure were prepared. The exact date of the first completed repository commit is **[to confirm]**.

---

## 5. Phase 2: Hardware mental models and implementation concepts

**Key dates:** 7–8 September 2026  
**Activity:** Architecture research and team guidance; the 7 September session included a late-night architecture sprint.

### Topics researched

- The difference between Java's signed `byte` range and the unsigned 8-bit range required by the simulated hardware.
- The use of `& 0xFF` to keep values within the range 0–255.
- 8051 special function registers, including ACC (A), B, PSW and SP.
- Register-bank organisation and the four register banks.
- Harvard memory organisation and the distinction between program memory and data memory.
- Stack behaviour and the 8051 stack pointer.
- LIFO behaviour for stacks and FIFO behaviour for queues.

### Concept transfer and team guidance

**Arnold – Registers and PSW**
- Discussed the role of registers as small, fast storage locations inside the processor.
- Explained the two-dimensional array representation for the four register banks.
- Explained how `value & 0xFF` can be used to constrain a Java integer to an 8-bit value.

**Shashidhara – Memory**
- Used the analogy of a protected recipe book for program/code memory and a reusable kitchen counter for data memory.
- Discussed reading byte-array values without allowing Java's signed-byte representation to cause confusion.

**Chrisel – Stack and data structures**
- Used a stack-of-plates analogy to explain LIFO behaviour and a queue/line analogy to explain FIFO behaviour.
- Discussed the 8051 stack pointer and the order of updating the stack pointer during push operations.

### Outcome

The team developed a shared understanding of the hardware concepts needed for the simulator and how those concepts could be represented using Java data structures.

---

## 6. Phase 3: Multi-process architecture and socket communication

**Key dates:** 28–30 September 2026  
**Activity:** Week 4 multi-process development and integration planning.

### Architecture work

The project moved from a single, tightly coupled simulator design towards three separately running Java processes:

1. **UI Process** – provides the user-facing dashboard and sends commands to the backend.
2. **Core Process** – handles simulator execution and CPU-related operations.
3. **Logging Process** – receives log messages and writes them to the simulator log file.

The processes communicate using TCP sockets on localhost. The ports recorded during development were:

| Component / connection | Port |
| --- | ---: |
| Core socket service | 5000 |
| Logging socket service | 5001 |
| Web dashboard / UI service | 8080 and/or 8000 **[confirm final configuration]** |

### Development and testing work

- The team worked on the socket-based communication between the processes.
- `TestCPU.java` was prepared with 14 opcode test cases.
- `QueueTest.java` was used for queue-related testing.
- A dual-mode interface was discussed, with a web dashboard and a command-line interface.
- The UI's JavaScript frontend used `fetch('/api/cmd')` to communicate with the Java backend.
- The Core process used a `ServerSocket` to accept commands and a socket connection to communicate with the Logger.
- The Logger process was designed to parse messages in the `LOG|LEVEL|CATEGORY|details` format and append entries to `simulator.log`.

### Concept transfer and team guidance

- Shashidhara was guided through the connection between the browser's JavaScript `fetch()` request and the Java backend's socket client.
- Chrisel was guided through the Core process's role as a socket server and its connection to the Logger.
- Arnold was guided through parsing structured log messages and writing them to the log file.

### Outcome

The project adopted a multi-process architecture. The separation of UI, Core and Logging responsibilities made it possible to develop and test the components independently before integration.

---

## 7. Phase 4: Development approach, code understanding and viva preparation

**Key dates:** 14–16 October 2026  
**Activity:** Development checkpoint and team learning review.

### Background

During the earlier implementation stage, some members had produced code quickly using AI-generated solutions. Although this helped create initial working versions, the team recognised that code which a member could not explain would be difficult to defend during a project review or viva.

### Change in approach

Cyrus introduced a more learning-focused approach, using the Vibe-Wise methodology as a reference for avoiding unverified AI-generated code and encouraging members to understand the implementation.

The team was asked to:

- Avoid accepting generated code without understanding it.
- Use AI as a tutor for explanations, smaller implementation steps and checking work.
- Type and build the code themselves as they learned.
- Test each part and understand how it worked.
- Be prepared to explain their own implementation without relying on AI during the viva.

### Technical preparation

The team also prepared for detailed questions about:

- The difference between signed overflow (`OV`) and the carry flag (`CY`).
- Processor flags and their meaning.
- Edge cases in instruction execution.
- Process communication and what happens when a process stops unexpectedly.
- The reasoning behind implementation choices and the behaviour of individual code sections.

### Team guidance

Cyrus asked members to explain what they had actually understood and implemented, rather than presenting unfinished or unverified work as complete. Members were encouraged to prepare for detailed examiner questions covering both basic concepts and implementation edge cases.

### Outcome

The team placed greater emphasis on code ownership, understanding and viva readiness. The approach was intended to ensure that members could explain the work assigned to them, rather than merely demonstrate code that had been generated for them.

---

## 8. Phase 5: Development follow-up and completion

**Period:** October 2026  
**Exact meeting dates and participants:** [to confirm]

### Progress follow-up

- Members continued learning and developing their assigned components.
- Cyrus regularly followed up with members about their current tasks and progress.
- The team worked towards completing the UI, Core and Logging components and integrating them into the multi-process architecture.

### Completion and time constraint

The UI and Logging components were completed as part of the development work. The Core component remained incomplete close to the deadline. According to the original meeting record, AI-generated code was used to complete the Core component under the time constraint so that the processes could be integrated and demonstrated.

This should be understood as a development-time trade-off, not as evidence that the Core implementation had already been fully reviewed or understood by every member. The later audit and integration activities are recorded in Section 10.

---

## 9. Phase 6: Branch audit, integration fixes and final verification

**Period:** 26 October–2 November 2026  
**Latest work-log date:** 2 November 2026

### Branches reviewed

The following member branches were included in the final reconciliation work:

- `arnold-LOG`
- `Shashidhara-UI-Final`
- `chrisel-CORE`

### Issues identified and fixes recorded

| Area | Issue identified | Fix / action recorded |
| --- | --- | --- |
| Arnold – Logging | Stray file named `src/LoggerProcessgfhhdff.java` | File removed during branch cleanup |
| Shashidhara – UI | URL decoding was missing | `URLDecoder.decode()` was added |
| Chrisel – Core | Socket server code was missing from the branch version under review | Socket server was restored |

### Logging improvements

The Logger work included support for runtime execution log levels/categories, including:

- `EXEC`
- `LOAD`
- `HALT`
- `RESET`

The Logger parses structured log messages and appends them to `simulator.log`.

### Integration and merge

The recorded integration work includes a fast-forward merge of `arnold-LOG` into `main`.

**Recorded commit:** `9af0dcb`

The final repository state and the exact relationship between this commit and the other two member branches should be checked against the GitHub commit history before formal submission.

### Verification

A verification script named `verify_all.py` was prepared to check the services associated with ports 5000, 5001 and 8080.

The supplied development chronology reports:

- 100% passing results from `verify_all.py`.
- 1,214 log entries generated during the verification work.
- Successful branch reconciliation and merge activity.

**Evidence note:** Attach the actual script output, test logs or screenshots when available. Until that evidence is included, these figures are recorded as results reported in the development chronology.

---

## 10. Phase 6: Meeting 5 — CRC-24b Algorithm Derivation, Viva Defense Rehearsal, and Final Delivery (3 November 2026)

**Key date:** 3 November 2026  
**Meeting duration:** 10:00 PM – 11:52 PM (1 hr 52 min)  
**Platform / Meeting Code:** Google Meet (`hbo-ojhk-fzg`)  
**Attendance Report:** Verified from official Google Meet attendance log (`2026-11-03 22_00 hbo-ojhk-fzg Attendance Report.xlsx`)

### Attendance and participation

| Member | Roll No | Time Joined | Time Exited | Duration | Participation Status |
| --- | :---: | :---: | :---: | :---: | --- |
| Cyrus Shobith Pinto | 25190111 | 10:00 PM | 11:52 PM | 1 hr 52 min | Present (Full duration, session lead) |
| Chrisel Lobo | 25190110 | 10:03 PM | 11:51 PM | 1 hr 49 min | Present |
| Shashidhara Poojari | 25190148 | 10:03 PM | 11:34 PM | 1 hr 1 min | Present |
| Arnold Noah Karyappa | 25190106 | 10:29 PM | 11:16 PM | 47 min | Present (Joined at 10:29 PM) |

### Meeting context and thought process

Meeting 5 was held on the eve of the final evaluation and viva defense. The session was organized to achieve three primary objectives:
1. **Algorithmic Derivation and 8-Bit Adaptation:** Walk through the mathematical foundation of the course-allocated telecommunications algorithm: **3GPP LTE-B CRC-24b** ($g_{\text{CRC24B}}(D) = D^{24} + D^{23} + D^6 + D^5 + D + 1$, generator `0x800063`). Address the silicon reality of how an 8-bit STC89C52 ALU and register file physically processes and stores a 24-bit remainder.
2. **Two-Tier Report Strategy:** Resolve the documentation scope by establishing both a comprehensive academic **Detailed Technical Report** (`Bondaas_STC89C52_Detailed_Report.docx`) and an executive summary **Short Report** (`Bondaas_STC89C52_Short_Report.docx`), while formally logging Meeting 5 minutes into the project history.
3. **Rubric Alignment and Viva Preparation:** Address the 5-mark GitHub contribution rubric by ensuring all members committed their respective modules to personal branches (`chrisel-CORE`, `arnold-LOG`, `Shashidhara-UI-Final`), and distributing customized viva defense cheat sheets to prepare each student for individual faculty questioning.

### Technical discussions and solutions

- **CRC-24b Cellular Baseband Walkthrough:** Cyrus demonstrated the 3GPP LTE transport block error detection formula. The generator polynomial $D^{24} + D^{23} + D^6 + D^5 + D + 1$ yields binary `1000 0000 0000 0000 0110 0011` (`0x800063`).
- **Decomposition across 8051 Working Registers:** Because the STC89C52 lacks 32-bit registers, the 24-bit CRC remainder is split across three 8-bit registers in Register Bank 0:
  - **`R5` (High Byte):** Bits 23..16 (e.g., `0x23` for test vector `"123456789"`)
  - **`R6` (Mid Byte):** Bits 15..8 (e.g., `0xEF`)
  - **`R7` (Low Byte):** Bits 7..0 (e.g., `0x52`)
  Bits are shifted left across $R_7 \to R_6 \to R_5$ using the Carry flag ($CY$), XORing `0x800063` when the MSB is high.
- **Two-Stage Telecom Pipeline:**
  - *Transmitter (Encoding):* Computes remainder and appends 24 parity bits to form the codeword (`31 32 33 34 35 36 37 38 39 23 EF 52`).
  - *Receiver (Decoding / Syndrome):* Re-evaluates codeword. Syndrome `0x000000` indicates a zero-error transmission; 1-bit or 2-bit burst errors yield non-zero syndromes (`0x520000`, `0xF60000`), proving error detection.
- **Logging Subsystem Walkthrough (Arnold):** Cyrus reviewed `LoggerProcess.java` with Arnold, explaining why disk logging runs as an independent OS process on Port 5001 to prevent CPU stalling, the role of `synchronized` in `LogHandler.java` for thread safety, and how to verify logging using `LoggerProcessTest.java`.
- **UI Web Server and Presets (Shashidhara):** Confirmed the Web dashboard on Port 8080 (`UIProcess.java`), featuring 6 silicon panels, 5 one-click presets, and the 3-stage CRC telecommunications lab.
- **Meeting Minutes Integration:** Chrisel and Cyrus confirmed that Meeting 5 minutes would be documented in `docs/meetings/meeting-05-2026-11-03.md` and appended to this master meeting record.

---

## 11. Individual contributions and file ownership

| Member | Assigned Subsystem & Files | Final Feature Ownership & Contributions |
| --- | --- | --- |
| **Cyrus Shobith Pinto** (Lead) | `src/CRC24b.java`<br>`programs/BenchmarkCRC.java`<br>`tests/TestCRC24b.java`<br>`test_sir_scenarios.py`<br>`verify_all.py` | Overall multi-process socket IPC orchestration (ports 5000, 5001, 8080); mathematical derivation and bit-accurate Java implementation of 3GPP LTE-B CRC-24b (`0x800063`); standalone vs. IPC performance benchmarking; system regression test suites; master technical reports. |
| **Chrisel Lobo** (Core Lead) | `src/CPU.java`<br>`src/PSW.java`<br>`src/Memory.java`<br>`src/Registers.java`<br>`src/CoreProcess.java`<br>`tests/TestCPU.java` | 8051 Fetch-Decode-Execute pipeline; ALU operations (`MUL AB`, `DIV AB`, `ADD`, `SUBB`, `ANL`, `ORL`, `XRL`, `PUSH`); hardware flags (`CY`, `AC`, `OV`, `P`); 256-byte internal RAM with bit-addressable area; 4 discrete register banks; TCP socket server on port 5000; CPU unit test suite (14/14 tests passing). |
| **Arnold Noah** (Logging Lead) | `src/LoggerProcess.java`<br>`src/LogHandler.java`<br>`src/LogMessage.java`<br>`tests/LoggerProcessTest.java`<br>`simulator.log` | Independent TCP logging daemon on port 5001; write-ahead append-only log file persistence; synchronized thread safety preventing concurrent write corruption; execution log parsing across categories (`INFO`, `EXEC`, `LOAD`, `HALT`, `RESET`); logger socket test suite. |
| **Shashidhara** (UI Lead) | `src/UIProcess.java`<br>`src/SimulatorUI.java`<br>`index.html`<br>`src/index.html` | Embedded HTTP Web server on port 8080 and Swing GUI fallback; 1560px responsive widescreen dashboard; 6 silicon panels (disassembly table, pipeline trace, registers, stack tower, circular FIFO queue, 256B RAM matrix); 5 one-click presets; 3-stage CRC-24b interactive noise lab. |

---

## 12. Key technical and team decisions

| Decision | Reason / purpose |
| --- | --- |
| Use Java with a pure JDK approach | Keep the implementation focused on standard Java without bulky external frameworks |
| Divide the project into 3 independent processes | Provide process fault isolation; ensure UI or logging stalls never freeze the 8051 CPU core |
| Use POSIX TCP loopback sockets (5000, 5001, 8080) | Reliable, ordered, cross-platform IPC without shared memory or heap corruption risks |
| Represent 8-bit silicon using bitmasks (`& 0xFF`) | Prevent Java signed 32-bit integer sign-extension in 8-bit registers and memory |
| Slice 24-bit CRC into $R_5, R_6, R_7$ | Overcome 8051 8-bit ALU register constraint while adhering strictly to 3GPP LTE specifications |
| Use synchronized writeLock in `LogHandler` | Ensure concurrent socket log frames never interleave or corrupt `simulator.log` |
| Maintain two-tier documentation (Detailed & Short) | Provide comprehensive technical documentation for deep review alongside an executive summary for viva |
| Use personal branches and integrate through pull requests | Keep traceable individual contributions for grading rubrics while maintaining a verified `main` branch |

---

## 13. Lessons learned

- Code that is produced quickly is not necessarily code that a team member can explain or defend during viva.
- Structured code walkthroughs with personalized task guides significantly build student confidence and technical depth.
- Multi-process architectures introduce socket overhead (~0.65ms per IPC message) but provide critical operating system fault isolation.
- Hardware edge cases (such as divide by zero setting $OV=1$ rather than crashing) must be modeled faithfully to reflect silicon realities.
- Dividing work into distinct personal branches requires disciplined merge reconciliation and regression testing before submission.
- Meeting records backed by verifiable attendance sheets and timestamps provide definitive proof of collaboration.

---

## 14. Evidence and items completed before submission

1. **Meeting Minutes Completed:** All 5 meeting minutes recorded in `docs/meetings/` (`meeting-01` to `meeting-05-2026-11-03.md`).
2. **Attendance Reports Verified:** Meeting 5 attendance verified from Google Meet report `2026-11-03 22_00 hbo-ojhk-fzg Attendance Report.xlsx`.
3. **Ports Documented:** Core Process on port 5000, Logger Process on port 5001, Web UI on port 8080.
4. **Source Ownership Clarified:** All four team members assigned clear file ownership and source references in Section 11.
5. **Unit & Edge-Case Tests Verified:**
   - `TestCPU.java`: 14/14 tests PASS (100%).
   - `TestCRC24b.java`: 8/8 tests PASS (100%).
   - `QueueTest.java`: 3/3 tests PASS (100%).
   - `test_sir_scenarios.py`: 8/8 edge-case scenarios PASS (100%).
6. **Benchmark Data Archived:** `BenchmarkCRC.java` empirical results recorded (1,028,489 pkts/sec standalone vs. 1,503 msgs/sec socket IPC).
7. **Git Branches Pushed:** `main`, `cyrus-integration`, `chrisel-CORE`, `arnold-LOG`, and `Shashidhara-UI-Final` fully synchronized and pushed to GitHub.
8. **Documentation Embedded:** High-resolution dashboard and lab screenshots embedded into both `Bondaas_STC89C52_Detailed_Report.docx` and `Bondaas_STC89C52_Short_Report.docx`.

---

## 15. Final project status

As of 5 November 2026, Team Bondaas has completed all implementation, integration, testing, documentation, and verification milestones for the STC89C52 Multi-Process Microcontroller Simulator.

All four team members have verified their code ownership, synchronized their GitHub branches, reviewed their personalized viva defense guides, and confirmed that the multi-process architecture is operating seamlessly across ports 5000, 5001, and 8080.

**Documentation status:** Complete, fully audited, and verified ready for academic submission.
