/**
 * QueueTest.java — Week 3 Assembly Validation Program
 * Demonstrates FIFO Queue behavior using the STC89C52 simulator's instruction set.
 *
 * This file serves two purposes:
 *  1. Shows the Assembly-level representation (as comments) of the test program.
 *  2. Provides a standalone Java runner that loads the bytecode into the
 *     simulator and prints the expected vs. actual results.
 *
 * Run from project root (Windows):
 *   javac -cp src programs\QueueTest.java && java -cp src;programs QueueTest
 */
public class QueueTest {

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  STC89C52 Simulator — Queue Validation Program");
        System.out.println("  Week 3 Deliverable: Assembly-level FIFO test");
        System.out.println("=".repeat(60));

        // ── Assembly program listing ─────────────────────────────────────
        // Addr  Opcode  Operand  Mnemonic
        // 0000  74      0A       MOV  A, #10       ; Load 10 into A
        // 0002  E0               ENQUEUE A         ; Q: [10]
        // 0003  74      1E       MOV  A, #30       ; Load 30 into A
        // 0005  E0               ENQUEUE A         ; Q: [10, 30]
        // 0006  74      05       MOV  A, #5        ; Load 5 into A
        // 0008  E0               ENQUEUE A         ; Q: [10, 30, 5]
        // 0009  D0               DEQUEUE A         ; A = 10 (FIFO), Q: [30, 5]
        // 000A  D0               DEQUEUE A         ; A = 30,        Q: [5]
        // 000B  D0               DEQUEUE A         ; A = 5,         Q: []
        // 000C  FF               HALT

        byte[] program = new byte[]{
            0x74, 0x0A,         // MOV A, #10
            (byte)0xE0,         // ENQUEUE A
            0x74, 0x1E,         // MOV A, #30
            (byte)0xE0,         // ENQUEUE A
            0x74, 0x05,         // MOV A, #5
            (byte)0xE0,         // ENQUEUE A
            (byte)0xD0,         // DEQUEUE A  → expect 10
            (byte)0xD0,         // DEQUEUE A  → expect 30
            (byte)0xD0,         // DEQUEUE A  → expect 5
            (byte)0xFF          // HALT
        };

        // ── Initialise simulator ─────────────────────────────────────────
        Registers      regs = new Registers();
        Memory         mem  = new Memory();
        DataStructures ds   = new DataStructures(256, 16);
        PSW            psw  = new PSW();
        CPU            cpu  = new CPU(regs, mem, ds, psw);
        mem.loadProgram(program);

        // ── Expected FIFO dequeue order ───────────────────────────────────
        int[] expected = {10, 30, 5};
        int[] actual   = new int[3];
        int dqCount = 0;

        System.out.println("\nExecution trace:\n");
        System.out.printf("%-6s %-20s %-6s %-8s %-8s%n",
                "Step", "Instruction", "A(hex)", "Q_SIZE", "Notes");
        System.out.println("-".repeat(55));

        int step = 0;
        while (!cpu.isHalt) {
            int pcBefore = cpu.pc;
            int qBefore  = ds.qSize;
            cpu.step();
            step++;

            String note = "";
            if (ds.qSize > qBefore)      note = "Enqueued A=" + regs.A;
            else if (ds.qSize < qBefore) {
                int dequeued = regs.A; // after dequeue A holds dequeued value
                note = "Dequeued → " + dequeued;
                if (dqCount < 3) actual[dqCount++] = dequeued;
            }
            System.out.printf("%-6d %-20s 0x%02X   %-8d %s%n",
                    step, pcBefore == 0 ? "see listing" : "",
                    regs.A, ds.qSize, note);
        }

        // ── Verification table ────────────────────────────────────────────
        System.out.println("\nFIFO Ordering Verification:");
        System.out.printf("%-10s %-10s %-10s %-8s%n", "Dequeue#", "Expected", "Actual", "Result");
        System.out.println("-".repeat(42));
        int pass = 0;
        for (int i = 0; i < expected.length; i++) {
            boolean ok = (i < actual.length) && (actual[i] == expected[i]);
            if (ok) pass++;
            System.out.printf("%-10d %-10d %-10d %-8s%n",
                    i+1, expected[i], (i < actual.length ? actual[i] : -1),
                    ok ? "PASS" : "FAIL");
        }
        System.out.println();
        System.out.printf("Queue empty after all dequeues: %b%n", ds.isQueueEmpty());
        System.out.printf("%d/%d dequeue operations verified FIFO order.%n", pass, expected.length);
        System.out.println(pass == expected.length
                ? "\nQueue validation PASSED ✓ — FIFO ordering correct."
                : "\nQueue validation FAILED — check above.");
    }
}
