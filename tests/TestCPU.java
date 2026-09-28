/**
 * TestCPU.java — Chrisel Lobo (25190110) / Core team
 *
 * Unit tests for every implemented instruction in the STC89C52 simulator.
 * Run from the project root with:
 *     javac -cp src tests/TestCPU.java && java -cp src:tests TestCPU
 * (Windows)
 *     javac -cp src tests\TestCPU.java && java -cp src;tests TestCPU
 *
 * Test record format required by Week 2 deliverables:
 *   TC-ID | Instruction | Expected | Actual | PASS / FAIL
 */
public class TestCPU {

    // ── Test infrastructure ────────────────────────────────────────────────
    private static int passed = 0;
    private static int failed = 0;

    private static void check(String tcId, String instruction,
                               String expected, String actual) {
        boolean ok = expected.equals(actual);
        System.out.printf("%-8s | %-30s | %-20s | %-20s | %s%n",
                tcId, instruction, expected, actual, ok ? "PASS" : "FAIL <<<");
        if (ok) passed++; else failed++;
    }

    // ── Build a minimal CPU for each test ─────────────────────────────────
    private static CPU newCPU(byte[] program) {
        Registers  regs = new Registers();
        Memory     mem  = new Memory();
        DataStructures ds  = new DataStructures(256, 16);
        PSW        psw  = new PSW();
        mem.loadProgram(program);
        return new CPU(regs, mem, ds, psw);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TESTS
    // ══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("STC89C52 Simulator — CPU Instruction Tests");
        System.out.println("=".repeat(100));
        System.out.printf("%-8s | %-30s | %-20s | %-20s | %s%n",
                "TC-ID", "Instruction", "Expected", "Actual", "Result");
        System.out.println("-".repeat(100));

        // ── TC01: MOV A, #data ─────────────────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, 0x42, (byte)0xFF}); // MOV A, #0x42; HALT
            cpu.step(); // MOV
            check("TC-01", "MOV A, #0x42 → A should be 0x42",
                    "42", String.format("%02X", cpu.regs.A));
        }

        // ── TC02: MOV A, #0x00 (edge case zero) ───────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, 0x00, (byte)0xFF});
            cpu.step();
            check("TC-02", "MOV A, #0x00 → A should be 0x00",
                    "00", String.format("%02X", cpu.regs.A));
        }

        // ── TC03: ADD A, #data — no carry ─────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, 0x10, 0x24, 0x05, (byte)0xFF}); // MOV A,#16; ADD A,#5
            cpu.step(); // MOV A, #0x10
            cpu.step(); // ADD A, #5  → A = 0x15
            check("TC-03", "ADD A, #0x05 (A was 0x10) → A=0x15, CY=0",
                    "15|CY=false",
                    String.format("%02X|CY=%b", cpu.regs.A, cpu.psw.isCy()));
        }

        // ── TC04: ADD A, #data — carry ─────────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, (byte)0xFE, 0x24, 0x03, (byte)0xFF}); // MOV A,#254; ADD A,#3
            cpu.step(); // MOV A, #0xFE = 254
            cpu.step(); // ADD A, #3 → result 257, carry
            check("TC-04", "ADD A, #3 (A=0xFE=254) → A=0x01, CY=1",
                    "01|CY=true",
                    String.format("%02X|CY=%b", cpu.regs.A, cpu.psw.isCy()));
        }

        // ── TC05: INC A ────────────────────────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, 0x07, 0x04, (byte)0xFF}); // MOV A,#7; INC A
            cpu.step(); // MOV
            cpu.step(); // INC → A = 8
            check("TC-05", "INC A (A was 0x07) → A should be 0x08",
                    "08", String.format("%02X", cpu.regs.A));
        }

        // ── TC06: INC A — wrap around 0xFF → 0x00 ─────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, (byte)0xFF, 0x04, (byte)0xFF});
            cpu.step(); // MOV A, #255
            cpu.step(); // INC A → wraps to 0
            check("TC-06", "INC A (A=0xFF) → A should wrap to 0x00",
                    "00", String.format("%02X", cpu.regs.A));
        }

        // ── TC07: ANL A, #data ─────────────────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, (byte)0xAB, 0x54, 0x0F, (byte)0xFF}); // MOV A,#0xAB; ANL A,#0x0F
            cpu.step(); // MOV A, #0xAB = 1010 1011
            cpu.step(); // ANL A, #0x0F → keeps low nibble = 0x0B
            check("TC-07", "ANL A, #0x0F (A=0xAB) → A should be 0x0B",
                    "0B", String.format("%02X", cpu.regs.A));
        }

        // ── TC08: SJMP rel (forward) ────────────────────────────────────────
        {
            //  Addr  Byte   Meaning
            //  0000  0x80   SJMP
            //  0001  0x01   offset +1  → skip 1 byte → land on 0x03
            //  0002  0x04   INC A  (should be skipped)
            //  0003  0xFF   HALT
            CPU cpu = newCPU(new byte[]{(byte)0x80, 0x01, 0x04, (byte)0xFF});
            cpu.step(); // SJMP +1 → PC goes to 0003
            // A should still be 0 (INC was skipped)
            check("TC-08", "SJMP rel +1 skips INC → A should remain 0x00",
                    "00", String.format("%02X", cpu.regs.A));
        }

        // ── TC09: ENQUEUE A ────────────────────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{0x74, 0x55, (byte)0xE0, (byte)0xFF}); // MOV A,#0x55; ENQUEUE
            cpu.step(); // MOV
            cpu.step(); // ENQUEUE
            check("TC-09", "ENQUEUE A (A=0x55) → queue size should be 1",
                    "1", String.valueOf(cpu.ds.qSize));
        }

        // ── TC10: DEQUEUE into A ───────────────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{
                0x74, 0x33,        // MOV A, #0x33
                (byte)0xE0,        // ENQUEUE A
                0x74, 0x00,        // MOV A, #0 (clear A)
                (byte)0xD0,        // DEQUEUE into A
                (byte)0xFF         // HALT
            });
            cpu.step(); // MOV A, #0x33
            cpu.step(); // ENQUEUE
            cpu.step(); // MOV A, #0   (clear A so we can confirm dequeue puts value back)
            cpu.step(); // DEQUEUE → A = 0x33
            check("TC-10", "DEQUEUE into A → A should be 0x33 (FIFO order)",
                    "33", String.format("%02X", cpu.regs.A));
        }

        // ── TC11: HALT stops execution ─────────────────────────────────────
        {
            CPU cpu = newCPU(new byte[]{(byte)0xFF});
            cpu.step(); // HALT
            check("TC-11", "HALT → cpu.isHalt should be true",
                    "true", String.valueOf(cpu.isHalt));
        }

        // ── TC12: Full demo program — end-to-end ───────────────────────────
        // MOV A,#254 → ADD A,#3 (CY) → INC A → ANL A,#0x0F → ENQUEUE → MOV A,#9 → ENQUEUE → DEQUEUE → HALT
        // Expected: A=0x00 (dequeued 0x00), Q_SIZE=1 (second value 9 still in queue)
        {
            CPU cpu = newCPU(new byte[]{
                0x74, (byte)0xFE,  // MOV A, #254
                0x24, 0x03,        // ADD A, #3  → 257 & 0xFF = 1, CY=1
                0x04,              // INC A       → 2
                0x54, 0x0F,        // ANL A, #0x0F→ 0x02 & 0x0F = 0x02
                (byte)0xE0,        // ENQUEUE A   → Q: [2]
                0x74, 0x09,        // MOV A, #9
                (byte)0xE0,        // ENQUEUE A   → Q: [2, 9]
                (byte)0xD0,        // DEQUEUE A   → A = 2  (FIFO), Q: [9]
                (byte)0xFF         // HALT
            });
            while (!cpu.isHalt) cpu.step();
            check("TC-12", "Full demo: DEQUEUE → A=0x02 (FIFO first-in)",
                    "02", String.format("%02X", cpu.regs.A));
            check("TC-13", "Full demo: Q_SIZE=1 after one dequeue",
                    "1", String.valueOf(cpu.ds.qSize));
            check("TC-14", "Full demo: CY=1 after overflow ADD",
                    "true", String.valueOf(cpu.psw.isCy()));
        }

        // ── Summary ────────────────────────────────────────────────────────
        System.out.println("=".repeat(100));
        System.out.printf("Results: %d PASSED, %d FAILED out of %d tests%n",
                passed, failed, passed + failed);
        if (failed == 0) System.out.println("ALL TESTS PASSED ✓");
        else             System.out.println("SOME TESTS FAILED — review above.");
    }
}
