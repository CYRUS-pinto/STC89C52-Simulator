# Week 3 Status Report

## Team: Bondaas | Date: September 2026

---

## Summary

Week 3 extended the simulator with Memory read/write, Stack (Push/Pop with Stack Pointer), and a Circular FIFO Queue. All three were validated through unit tests and a dedicated assembly-level validation program (`programs/QueueTest.java`). A Mermaid queue flowchart was also produced (see `docs/queue-flowchart.md`).

---

## Completed Work

### Memory (Arnold)
- `Memory.java` extended with `dumpCode()` and `dumpRAM()` helpers for UI display.
- `writeRAM(address, value)` and `readRAM(address)` tested.

### Stack (Chrisel)
- Stack backed by `int[256]` array in `DataStructures.java`.
- Stack Pointer (SP) initialised to `0x07` (matches real STC89C52 reset default).
- `push(val)`: SP increments first, then writes — STC89C52 hardware behavior.
- `pop()`: reads then decrements.
- Overflow and underflow throw `RuntimeException` with diagnostic message.

### FIFO Circular Queue (Shashidhara)
- `enqueue(val)` / `dequeue()` using circular index arithmetic.
- `isQueueFull()` / `isQueueEmpty()` correctly implemented.
- Queue capacity: 16 slots (configurable).
- ENQUEUE (opcode 0xE0) and DEQUEUE (opcode 0xD0) wired into CPU.

---

## Queue Flowchart

See `docs/queue-flowchart.md` for the full Mermaid flowchart covering:
- Enqueue (with FULL condition check)
- Dequeue (with EMPTY condition check)
- Queue status/size update

---

## Assembly Validation Program — FIFO Test

File: `programs/QueueTest.java`

```
Enqueue 10 → Enqueue 30 → Enqueue 5
Dequeue → expected 10 (FIFO first-in)
Dequeue → expected 30
Dequeue → expected 5
HALT
```

All 3 dequeue operations returned correct FIFO values.
Queue empty after all dequeues: true ✓

---

## Test Results

| Test | Operation | Expected | Actual | Status |
|------|-----------|----------|--------|--------|
| MEM-01 | writeRAM(0x20, 0xAB) then readRAM(0x20) | 0xAB | 0xAB | PASS |
| STK-01 | push(5), pop() | 5 | 5 | PASS |
| STK-02 | push on empty stack (SP=7→8) | SP=8 | SP=8 | PASS |
| Q-01 | enqueue(10), dequeue() | 10 | 10 | PASS |
| Q-02 | enqueue 3 items, dequeue in FIFO order | 10,30,5 | 10,30,5 | PASS |
| Q-03 | dequeue on empty queue | RuntimeException | Exception thrown | PASS |

---

## Known Issues

- SimulatorUI does not yet display stack or queue content panel (carried to Week 4 UI build).

---

## Plan for Week 4

- Separate simulator into 3 independent processes: UI, Core, Logger.
- Use TCP socket IPC between processes.
- Benchmark single-process vs. multi-process.
