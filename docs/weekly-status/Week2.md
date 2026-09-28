# Week 2 Status Report

## Team: Bondaas | Date: September 2026

---

## Summary

Week 2 focused on converting the Week 1 architecture research into a working, executable Java simulator. By the end of this week the team produced a working CPU core that can fetch, decode, and execute 8 instructions, update CPU state (registers, flags, PC), and terminate correctly on HALT.

---

## Completed Work

### CPU Foundation (Chrisel / Core)
- Implemented `CPU.java` with a clean `step()` method calling `fetch()`, `decode()`, and `execute()` in sequence.
- Implemented `Registers.java`: Accumulator (A), B register, four-bank register file (R0–R7 × 4 banks).
- Implemented `PSW.java`: Carry (CY), Auxiliary Carry (AC), Overflow (OV), Parity (P), Register Bank Select (RS0/RS1) flags. Correct hardware-accurate flag update formulas.
- Implemented `Memory.java`: 8 KB code memory + 256-byte internal RAM + `loadProgram()`.

### Instruction Set (Chrisel / Arnold supporting)

| # | Opcode | Mnemonic | Category |
|---|--------|----------|----------|
| 1 | 0x74 | MOV A, #data | Data Transfer |
| 2 | 0x24 | ADD A, #data | Arithmetic |
| 3 | 0x04 | INC A | Increment |
| 4 | 0x54 | ANL A, #data | Logical |
| 5 | 0x80 | SJMP rel | Control Flow |
| 6 | 0xE0 | ENQUEUE A | Data Structure |
| 7 | 0xD0 | DEQUEUE A | Data Structure |
| 8 | 0xFF | HALT | Termination |

All 8 instructions pass through `fetch()` → `decode()` → `execute()`.

### Data Structures (Shashidhara)
- `DataStructures.java`: Stack (256-deep, SP default 0x07), Circular Queue (16-slot).
- Push/Pop with stack overflow/underflow detection.
- Enqueue/Dequeue with full/empty detection.

### Demonstration Program (loaded in CoreProcess)
```
0000: MOV  A, #254    ; A = FE
0002: ADD  A, #3      ; A = 01, CY = 1
0003: INC  A          ; A = 02
0004: ANL  A, #0x0F   ; A = 02
0005: ENQUEUE A       ; Q = [02]
0006: MOV  A, #9      ; A = 09
0008: ENQUEUE A       ; Q = [02, 09]
0009: DEQUEUE A       ; A = 02 (FIFO)
000A: HALT
```

---

## Issues Encountered

- `Memory.java` was initially missing `loadProgram()`. Added in Week 2 fix pass.
- SimulatorUI started as an empty stub; UI build carried to Week 4 (process separation).

---

## Test Results

See `tests/TestCPU.java`. 14 test cases covering all 8 instruction categories. All PASS.

| Test | Instruction | Expected | Status |
|------|-------------|----------|--------|
| TC-01 | MOV A, #0x42 | A=42 | PASS |
| TC-02 | MOV A, #0x00 | A=00 | PASS |
| TC-03 | ADD A, #5 (no carry) | A=15, CY=0 | PASS |
| TC-04 | ADD A, #3 (carry) | A=01, CY=1 | PASS |
| TC-05 | INC A | A=08 | PASS |
| TC-06 | INC A (wrap 0xFF) | A=00 | PASS |
| TC-07 | ANL A, #0x0F | A=0B | PASS |
| TC-08 | SJMP rel +1 | PC skips INC | PASS |
| TC-09 | ENQUEUE | Q_SIZE=1 | PASS |
| TC-10 | DEQUEUE | A=33 (FIFO) | PASS |
| TC-11 | HALT | isHalt=true | PASS |
| TC-12–14 | Full demo program | A=02, Q=1, CY=1 | PASS |

---

## Plan for Week 3

- Add stack (PUSH/POP) and FIFO queue operations.
- Display memory, stack, and queue in the UI.
- Queue flowchart.
- Assembly validation program for the queue.
