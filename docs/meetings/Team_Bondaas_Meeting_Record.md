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

## 10. Individual contributions

| Member | Contribution |
| --- | --- |
| Cyrus Shobith Pinto | Led project planning and task allocation; guided the team through hardware and software concepts; established the code-understanding approach; followed up on progress; coordinated integration, branch reviews, system testing and verification. |
| Shashidhara | Worked on the UI process, including the dashboard using HTML, CSS and JavaScript, the Java server and communication with the Core process. Also created a stand-in Core for UI testing and addressed the URL-decoding issue during the final audit. |
| Chrisel Lobo | Worked on the Core process, including CPU-related functionality, memory, stack and circular queue concepts, and Core socket communication. The socket server was restored during the final branch audit. **[Add exact source files and final feature ownership.]** |
| Arnold Noah | Worked on the Logging process, including receiving structured log messages and writing to `simulator.log`. Logging support included runtime categories/levels such as `EXEC`, `LOAD`, `HALT` and `RESET`. **[Add exact source files and final feature ownership.]** |

---

## 11. Key technical and team decisions

| Decision | Reason / purpose |
| --- | --- |
| Use Java with a pure JDK approach | Keep the implementation focused on Java and avoid unnecessary external frameworks |
| Divide the project into independently developed areas | Allow each member to take responsibility for a component |
| Use personal branches and integrate through review | Reduce conflicts and avoid direct unreviewed changes to `main` |
| Represent 8-bit values using masking such as `& 0xFF` | Keep Java integer values within the simulated hardware's unsigned 8-bit range |
| Separate UI, Core and Logging into processes | Make the system modular and demonstrate inter-process communication |
| Use TCP sockets for communication | Allow the processes to exchange commands and log messages |
| Use AI as a learning aid rather than blindly accepting generated code | Improve code understanding, ownership and viva preparation |
| Run a final branch audit and verification | Identify integration issues and check that the services work together |

---

## 12. Lessons learned

- Code that is produced quickly is not necessarily code that a team member can explain or maintain.
- Using AI to understand concepts, work through smaller steps and check code can support learning more effectively than accepting complete solutions without review.
- Dividing work into separate components makes parallel development possible, but integration still requires careful testing.
- Socket-based systems need attention to both sides of the communication: the server, the client and the message format.
- Small branch issues, such as stray source files or missing methods, can affect integration and should be checked before merging.
- Regular progress follow-up helps identify incomplete work before the deadline.
- When time pressure affects the development approach, the trade-off should be documented honestly.
- Test results are stronger when the documentation includes reproducible commands and the actual output.

---

## 13. Evidence and items to complete before submission

The following items should be added or checked before treating this record as the final submission copy:

1. Confirm the exact dates, meeting numbers and participants for the meetings/checkpoints where these are still marked **[to confirm]**.
2. Confirm whether the final UI service uses port 8080, port 8000, or both.
3. Add the exact source files and final feature ownership for Chrisel and Arnold.
4. Attach or link the actual `verify_all.py` output supporting the reported 100% pass result.
5. Attach or link the test output supporting the reported 1,214 log entries.
6. Verify commit `9af0dcb` and the merge details against the repository's Git history.
7. Add links to the relevant commits or pull requests for each member's work.
8. Confirm that the final Core implementation has been reviewed and can be explained by its owner.
9. Add screenshots of the final dashboard and terminal/process output if these are available.

---

## 14. Final project status

As of 2 November 2026, the supplied development chronology records that the team had completed a final branch audit, addressed the listed integration issues, merged `arnold-LOG` into `main`, and run the `verify_all.py` verification script with a reported 100% pass result.

The reported verification also generated 1,214 log entries. These results should be accompanied by the corresponding test output or logs in the repository so that they can be independently checked.

**Documentation status:** Updated with the project chronology and final audit information supplied by the team. Items requiring additional evidence or confirmation remain identified above.
