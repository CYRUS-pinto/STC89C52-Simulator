public class TestCRC24b {
    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  STC89C52 SIMULATOR: CRC-24b VERIFICATION SUITE");
        System.out.println("  Allocated Algorithm: g_CRC24B(D) = D^24 + D^23 + D^6 + D^5 + D + 1");
        System.out.println("==================================================================");

        int passed = 0;
        int total = 0;

        // Test 1: Standard LTE vector "123456789"
        total++;
        byte[] v1 = "123456789".getBytes();
        int crc1 = CRC24b.calculate(v1);
        String hex1 = String.format("%06X", crc1);
        boolean pass1 = hex1.equals("23EF52");
        System.out.printf("[TC-01] Vector '123456789' -> Expected: 0x23EF52 | Actual: 0x%s -> %s\n",
                hex1, pass1 ? "PASS" : "FAIL");
        if (pass1) passed++;

        // Test 2: Single byte "A"
        total++;
        byte[] v2 = "A".getBytes();
        int crc2 = CRC24b.calculate(v2);
        String hex2 = String.format("%06X", crc2);
        boolean pass2 = crc2 > 0;
        System.out.printf("[TC-02] Single Byte 'A' -> Checksum: 0x%s -> %s\n",
                hex2, pass2 ? "PASS" : "FAIL");
        if (pass2) passed++;

        // Test 3: 8051 Multi-Register Decomposition [R5, R6, R7]
        total++;
        int[] r = CRC24b.to8051Registers(crc1);
        int reconstructed = CRC24b.from8051Registers(r[0], r[1], r[2]);
        boolean pass3 = (reconstructed == crc1) && (r[0] == 0x23) && (r[1] == 0xEF) && (r[2] == 0x52);
        System.out.printf("[TC-03] 8051 Register Decomposition: R5=0x%02X, R6=0x%02X, R7=0x%02X -> %s\n",
                r[0], r[1], r[2], pass3 ? "PASS" : "FAIL");
        if (pass3) passed++;

        // Test 4: Hardware Carry-Propagation Simulation Equivalence
        total++;
        int[] hwRegs = new int[]{ 0, 0, 0 };
        for (byte b : v1) {
            CRC24b.step8051Hardware(hwRegs, b);
        }
        int hwResult = CRC24b.from8051Registers(hwRegs[0], hwRegs[1], hwRegs[2]);
        boolean pass4 = (hwResult == crc1);
        System.out.printf("[TC-04] Hardware Carry-Propagation Simulation -> 0x%06X (Matches Canon: %s) -> %s\n",
                hwResult, pass4, pass4 ? "PASS" : "FAIL");
        if (pass4) passed++;

        // Test 5: Codeword Encoding & Decoding (Zero Syndrome Verification)
        total++;
        byte[] codeword = CRC24b.encode(v1);
        int syndrome = CRC24b.decode(codeword);
        boolean pass5 = (syndrome == 0x000000);
        System.out.printf("[TC-05] Codeword Encoding & Decoding -> Syndrome: 0x%06X (Valid Packet: %s) -> %s\n",
                syndrome, pass5, pass5 ? "PASS" : "FAIL");
        if (pass5) passed++;

        // Test 6: 1-Bit Transmission Noise Detection
        total++;
        byte[] noisy1 = CRC24b.inject1BitError(codeword, 0, 0); // flip bit 0 of first byte
        int syn1 = CRC24b.decode(noisy1);
        boolean pass6 = (syn1 != 0x000000);
        System.out.printf("[TC-06] 1-Bit Noise Injection -> Syndrome: 0x%06X (Error Detected: %s) -> %s\n",
                syn1, pass6, pass6 ? "PASS" : "FAIL");
        if (pass6) passed++;

        // Test 7: 2-Bit Burst Transmission Noise Detection
        total++;
        byte[] noisy2 = CRC24b.inject2BitError(codeword, 0, 0); // flip bits 0 & 1 of first byte
        int syn2 = CRC24b.decode(noisy2);
        boolean pass7 = (syn2 != 0x000000);
        System.out.printf("[TC-07] 2-Bit Burst Noise Injection -> Syndrome: 0x%06X (Burst Detected: %s) -> %s\n",
                syn2, pass7, pass7 ? "PASS" : "FAIL");
        if (pass7) passed++;

        // Test 8: Empty payload returns 0
        total++;
        int crcEmpty = CRC24b.calculate(new byte[0]);
        boolean pass8 = (crcEmpty == 0);
        System.out.printf("[TC-08] Empty Payload Checksum: 0x%06X -> %s\n",
                crcEmpty, pass8 ? "PASS" : "FAIL");
        if (pass8) passed++;

        System.out.println("==================================================================");
        System.out.printf("Results: %d PASSED, %d FAILED out of %d tests\n", passed, (total - passed), total);
        if (passed == total) {
            System.out.println("ALL CRC-24b TESTS PASSED SUCCESSFULLY! (100%)\n");
        }
    }
}
