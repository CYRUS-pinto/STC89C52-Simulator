import java.nio.charset.StandardCharsets;

/**
 * CRC-24b Implementation (3GPP LTE / STC89C52 Simulator)
 *
 * Team 7 (Bondass) Allocated Algorithm:
 * Generator Polynomial: g_CRC24B(D) = D^24 + D^23 + D^6 + D^5 + D + 1
 *
 * Hex Generator Representation:
 * - D^24 is implicit (top bit 24)
 * - D^23 = 0x800000
 * - D^6  = 0x000040
 * - D^5  = 0x000020
 * - D^1  = 0x000002
 * - 1    = 0x000001
 * Sum = 0x800063
 *
 * Microcontroller 8-bit Register Mapping:
 * - R5 : High Byte (bits 23..16) -> CRC_HI
 * - R6 : Mid Byte  (bits 15..8)  -> CRC_MID
 * - R7 : Low Byte  (bits 7..0)   -> CRC_LO
 * - Accumulator A also mirrors Low Byte (bits 7..0)
 */
public class CRC24b {

    public static final int POLYNOMIAL_24B = 0x800063;
    public static final int MASK_24BIT     = 0xFFFFFF;

    /**
     * Standard 3GPP LTE-B CRC-24 calculation over raw byte array.
     * Initial shift-register value: 0x000000
     */
    public static int calculate(byte[] data) {
        if (data == null) return 0;
        int crc = 0x000000;

        for (byte b : data) {
            // XOR incoming byte into the high byte (bits 23..16)
            crc ^= ((b & 0xFF) << 16);

            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 0x800000) != 0) {
                    crc = ((crc << 1) ^ POLYNOMIAL_24B) & MASK_24BIT;
                } else {
                    crc = (crc << 1) & MASK_24BIT;
                }
            }
        }

        return crc & MASK_24BIT;
    }

    /**
     * Convenience method to calculate CRC-24b from an ASCII string.
     */
    public static int calculateString(String text) {
        if (text == null) return 0;
        return calculate(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convenience method to calculate CRC-24b from hex string (e.g. "01020304").
     */
    public static int calculateHex(String hex) {
        if (hex == null || hex.trim().isEmpty()) return 0;
        String clean = hex.replaceAll("\\s+", "").toUpperCase();
        if (clean.startsWith("0X")) clean = clean.substring(2);
        if (clean.length() % 2 != 0) clean = "0" + clean;

        byte[] bytes = new byte[clean.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return calculate(bytes);
    }

    /**
     * Splits 24-bit CRC integer into 8051 8-bit registers [R5, R6, R7].
     * R5 = bits 23..16 (High)
     * R6 = bits 15..8  (Mid)
     * R7 = bits 7..0   (Low)
     */
    public static int[] to8051Registers(int crc24) {
        int r5 = (crc24 >> 16) & 0xFF;
        int r6 = (crc24 >> 8)  & 0xFF;
        int r7 = crc24         & 0xFF;
        return new int[]{ r5, r6, r7 };
    }

    /**
     * Combines three 8051 registers back into a single 24-bit integer.
     */
    public static int from8051Registers(int r5, int r6, int r7) {
        return (((r5 & 0xFF) << 16) | ((r6 & 0xFF) << 8) | (r7 & 0xFF)) & MASK_24BIT;
    }

    /**
     * Encodes a payload by computing its CRC-24b remainder and appending
     * the 3 remainder bytes (R5, R6, R7) to form the transmitted codeword T(x).
     */
    public static byte[] encode(byte[] payload) {
        if (payload == null) payload = new byte[0];
        int crc = calculate(payload);
        byte[] codeword = new byte[payload.length + 3];
        System.arraycopy(payload, 0, codeword, 0, payload.length);
        codeword[payload.length]     = (byte) ((crc >> 16) & 0xFF); // R5
        codeword[payload.length + 1] = (byte) ((crc >> 8) & 0xFF);  // R6
        codeword[payload.length + 2] = (byte) (crc & 0xFF);         // R7
        return codeword;
    }

    /**
     * Decodes and validates a received codeword by calculating the CRC over
     * the entire codeword (data + parity).
     * By polynomial algebra T(x) mod G(x) == 0:
     * - Returns 0x000000 (Syndrome = 0) if the codeword is clean and valid.
     * - Returns non-zero syndrome if errors are detected.
     */
    public static int decode(byte[] codeword) {
        return calculate(codeword);
    }

    /**
     * Injects a single-bit error at the specified byte and bit position.
     */
    public static byte[] inject1BitError(byte[] data, int byteIdx, int bitIdx) {
        if (data == null || data.length == 0) return data;
        byte[] corrupted = data.clone();
        int safeByte = Math.min(Math.max(0, byteIdx), corrupted.length - 1);
        corrupted[safeByte] ^= (byte) (1 << (bitIdx % 8));
        return corrupted;
    }

    /**
     * Injects a 2-bit burst error at adjacent bits or specified positions.
     */
    public static byte[] inject2BitError(byte[] data, int byteIdx, int bitIdx) {
        if (data == null || data.length == 0) return data;
        byte[] corrupted = data.clone();
        int safeByte = Math.min(Math.max(0, byteIdx), corrupted.length - 1);
        // Flip bitIdx and bitIdx+1 (burst error)
        corrupted[safeByte] ^= (byte) (1 << (bitIdx % 8));
        corrupted[safeByte] ^= (byte) (1 << ((bitIdx + 1) % 8));
        return corrupted;
    }

    /**
     * Simulates 8051 multi-byte register shift step-by-step.
     * Models the hardware ALU Carry bit (CY) across R7 -> R6 -> R5.
     */
    public static void step8051Hardware(int[] regs, int inputByte) {
        // regs[0] = R5 (HI), regs[1] = R6 (MID), regs[2] = R7 (LO)
        regs[0] = (regs[0] ^ (inputByte & 0xFF)) & 0xFF;

        for (int bit = 0; bit < 8; bit++) {
            // Check top bit of R5 (bit 7)
            boolean carryOut = (regs[0] & 0x80) != 0;

            // Shift 24-bit chain left by 1 bit using hardware carry propagation
            int cyMidToHi = (regs[1] & 0x80) != 0 ? 1 : 0;
            int cyLoToMid = (regs[2] & 0x80) != 0 ? 1 : 0;

            regs[0] = ((regs[0] << 1) | cyMidToHi) & 0xFF;
            regs[1] = ((regs[1] << 1) | cyLoToMid) & 0xFF;
            regs[2] = (regs[2] << 1) & 0xFF;

            // If MSB was 1, XOR with 0x800063 (R5 ^= 0x80, R6 ^= 0x00, R7 ^= 0x63)
            if (carryOut) {
                regs[0] ^= 0x80;
                regs[1] ^= 0x00;
                regs[2] ^= 0x63;
            }
        }
    }

    /**
     * Error-detection verification:
     * Flips one bit in the input data and computes the new CRC.
     * Returns true if the corrupted CRC differs from the original (detecting the error).
     */
    public static boolean verifyErrorDetection(byte[] data, int byteIndex, int bitIndex) {
        if (data == null || data.length == 0) return true;
        int originalCrc = calculate(data);

        // Create corrupted copy
        byte[] corrupted = data.clone();
        int safeByteIdx = Math.min(byteIndex, corrupted.length - 1);
        corrupted[safeByteIdx] ^= (byte) (1 << (bitIndex % 8));

        int corruptedCrc = calculate(corrupted);
        return originalCrc != corruptedCrc;
    }
}
