/**
 * TestCRC24b — Chrisel Lobo (25190110)
 * Unit Test Suite for CRC-24b (3GPP LTE) Implementation & STC89C52 Register Slicing
 *
 * Evaluator Verification:
 * - TC-01: Empty payload returns INITIAL_VALUE (0x000000)
 * - TC-02: Single character payload ("A")
 * - TC-03: Official 3GPP LTE Test Vector ("123456789" -> 0x23EF52)
 * - TC-04: Multi-block telecom transmission payload
 * - TC-05: 1-Bit Noise Error Detection (Single-bit corruption alters checksum completely)
 * - TC-06: 8-Bit Register Slicing & Hardware Mapping (R5=0x23, R6=0xEF, R7=0x52, A=0x52)
 */
public class TestCRC24b {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(String tcId, String desc, String expected, String actual) {
        boolean ok = expected.equalsIgnoreCase(actual);
        if (ok) {
            passed++;
            System.out.printf("%-8s | %-44s | %-12s | %-12s | PASS\n", tcId, desc, expected, actual);
        } else {
            failed++;
            System.out.printf("%-8s | %-44s | %-12s | %-12s | FAIL\n", tcId, desc, expected, actual);
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("  STC89C52 SIMULATOR — CRC-24b (3GPP LTE) UNIT TEST SUITE");
        System.out.println("  Owner: Chrisel Lobo (25190110) | Polynomial: D^24 + D^23 + D^6 + D^5 + D + 1 (0x800063)");
        System.out.println("==========================================================================================");
        System.out.printf("%-8s | %-44s | %-12s | %-12s | Result\n", "TC-ID", "Test Description", "Expected", "Actual");
        System.out.println("------------------------------------------------------------------------------------------");

        // TC-01: Empty payload
        int crc01 = CRC24b.calculate("");
        check("TC-01", "Empty string payload (initial CRC)", "0x000000", CRC24b.formatHex(crc01));

        // TC-02: Single byte payload "A"
        int crc02 = CRC24b.calculate("A");
        check("TC-02", "Single byte 'A' (0x41)", "0x0010C2", CRC24b.formatHex(crc02));

        // TC-03: 3GPP LTE standard test vector "123456789"
        int crc03 = CRC24b.calculate("123456789");
        check("TC-03", "3GPP LTE vector '123456789'", "0x23EF52", CRC24b.formatHex(crc03));

        // TC-04: Multi-block telecom payload
        String msg = "STC89C52-PBL-TEAM-BONDAAS";
        int crc04 = CRC24b.calculate(msg);
        check("TC-04", "Multi-block telecom frame", "0x3642A1", CRC24b.formatHex(crc04));

        // TC-05: 1-Bit Noise Error Detection (Avalanche Effect)
        // Flip bit 0 of the 3rd byte ("3" -> '3' ^ 1 = '2', "122456789")
        byte[] noisy = "123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        noisy[2] ^= 0x01; // flip 1 bit
        int crc05 = CRC24b.calculate(noisy);
        boolean altered = (crc05 != crc03);
        check("TC-05", "1-Bit Noise Error Detection (0x23EF52 -> altered)", "true", String.valueOf(altered));

        // TC-06: 8-bit Register Slicing & Hardware Mapping
        Registers regs = new Registers();
        PSW psw = new PSW();
        CRC24b.applyToRegisters(regs, psw, crc03);

        int r5 = regs.getR(5, 0);
        int r6 = regs.getR(6, 0);
        int r7 = regs.getR(7, 0);
        int acc = regs.A;

        boolean mappingOk = (r5 == 0x23) && (r6 == 0xEF) && (r7 == 0x52) && (acc == 0x52);
        check("TC-06", "8-Bit Slicing: R5=23 R6=EF R7=52 A=52", "true", String.valueOf(mappingOk));

        System.out.println("==========================================================================================");
        System.out.printf("Results: %d PASSED, %d FAILED out of %d tests\n", passed, failed, (passed + failed));
        if (failed == 0) {
            System.out.println("ALL CRC-24b VERIFICATION TESTS PASSED \u2713");
        } else {
            System.out.println("SOME TESTS FAILED \u2717");
        }
    }
}
