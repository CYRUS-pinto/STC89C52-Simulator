# Queue Flowchart — Week 3 Deliverable

## STC89C52 Simulator — Circular FIFO Queue

This document contains the flowchart for the FIFO Queue implemented in `DataStructures.java`.

---

## ENQUEUE Operation

```mermaid
flowchart TD
    A([Start: ENQUEUE val]) --> B{Is Queue FULL?\nqSize == qCapacity}
    B -- YES --> C[Throw RuntimeException\nCircular Queue Overflow]
    C --> Z([End])
    B -- NO --> D["Write val to queue[qRear]\n(masked to 0xFF)"]
    D --> E["qRear = (qRear + 1) % qCapacity\n(wrap around)"]
    E --> F[qSize++]
    F --> G["Queue status update:\nqSize, qRear advanced"]
    G --> Z
```

---

## DEQUEUE Operation

```mermaid
flowchart TD
    A([Start: DEQUEUE]) --> B{Is Queue EMPTY?\nqSize == 0}
    B -- YES --> C[Throw RuntimeException\nCircular Queue Underflow]
    C --> Z([End])
    B -- NO --> D["Read val = queue[qFront]"]
    D --> E["qFront = (qFront + 1) % qCapacity\n(wrap around)"]
    E --> F[qSize--]
    F --> G["Return val\n(loaded into Accumulator A)"]
    G --> H["Queue status update:\nqSize, qFront advanced"]
    H --> Z
```

---

## Queue Status / State Diagram

```mermaid
stateDiagram-v2
    [*] --> Empty : initialize (qSize=0)
    Empty --> Partial : enqueue()
    Partial --> Partial : enqueue() or dequeue()
    Partial --> Empty : dequeue() when qSize becomes 0
    Partial --> Full : enqueue() when qSize reaches qCapacity
    Full --> Partial : dequeue()
    Full --> [*] : error if enqueue() attempted
    Empty --> [*] : error if dequeue() attempted
```

---

## Queue Parameters

| Parameter | Value | Meaning |
|-----------|-------|---------|
| `qCapacity` | 16 | Maximum items the queue can hold |
| `qFront` | 0 (init) | Index of next item to dequeue |
| `qRear` | 0 (init) | Index where next item will be enqueued |
| `qSize` | 0 (init) | Current number of items in queue |
| Overflow guard | `qSize == qCapacity` | Throws exception |
| Underflow guard | `qSize == 0` | Throws exception |

---

## Assembly Example (FIFO validation)

See `programs/QueueTest.java` for the full runnable validation.

```
; Enqueue 3 values
MOV  A, #10    ; A = 10
ENQUEUE A      ; Q = [10]
MOV  A, #30    ; A = 30
ENQUEUE A      ; Q = [10, 30]
MOV  A, #5     ; A = 5
ENQUEUE A      ; Q = [10, 30, 5]

; Dequeue in FIFO order
DEQUEUE A      ; A = 10  ← first in, first out ✓
DEQUEUE A      ; A = 30                          ✓
DEQUEUE A      ; A = 5                           ✓
HALT
```
