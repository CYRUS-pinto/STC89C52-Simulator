import java.nio.charset.StandardCharsets;

/**
 * CRC24b — Chrisel Lobo (25190110)
 * Core Process Subsystem — Week 4 Allocated Algorithm
 *
 * Algorithm Specification:
 * - Standard: 3GPP LTE (3GPP TS 36.212 Section 5.1.1) CRC-24B
 * - Polynomial: g_CRC24B(D) = D^24 + D^23 + D^6 + D^5 + D + 1
 * - Generator Hex: 0x800063 (binary: 1000 0000 0000 0000 0110 0011)
 * - Initial Value: 0x000000
 * - RefIn: false, RefOut: false, XorOut: 0x000000
 *
 * 8-Bit Hardware Register Mapping on STC89C52:
 * Because the 8051 / STC89C52 architecture has an 8-bit ALU and 8-bit registers,
 * the 24-bit remainder cannot fit into a single register. The Core Process
 * slices the 24-bit remainder into three 8-bit bytes mapped directly into
 * General Purpose Register Bank 0 (or active bank):
 *   - High Byte (bits 23..16) -> R5 (e.g. 0x23)
 *   - Mid Byte  (bits 15..8)  -> R6 (e.g. 0xEF)
 *   - Low Byte  (bits 7..0)   -> R7 (e.g. 0x52)
 *   - Accumulator A also receives Low Byte (bits 7..0) for immediate ALU operations.
 *
 * Verified Standard Test Vector:
 *   Input ASCII: "123456789"
 *   Expected Hex Checksum: 0x23EF52 (R5=0x23, R6=0xEF, R7=0x52)
 */
public class CRC24b {
    public static final int POLYNOMIAL = 0x800063;
    public static final int INITIAL_VALUE = 0x000000;
    public static final int CRC_MASK = 0xFFFFFF;

    /**
     * Calculates the CRC-24b checksum over an array of bytes.
     * Implements bit-by-bit polynomial division adhering to 3GPP LTE-B specs.
     */
    public static int calculate(byte[] data) {
        if (data == null || data.length == 0) {
            return INITIAL_VALUE;
        }

        int crc = INITIAL_VALUE;
        for (byte b : data) {
            crc ^= ((b & 0xFF) << 16);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x800000) != 0) {
                    crc = ((crc << 1) ^ POLYNOMIAL) & CRC_MASK;
                } else {
                    crc = (crc << 1) & CRC_MASK;
                }
            }
        }
        return crc & CRC_MASK;
    }

    /**
     * Convenience method to calculate CRC-24b from an ASCII or UTF-8 string.
     */
    public static int calculate(String text) {
        if (text == null || text.isEmpty()) {
            return INITIAL_VALUE;
        }
        return calculate(text.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Returns the High Byte (bits 23..16) mapped to Register R5.
     */
    public static int getHighByte(int crc) {
        return (crc >> 16) & 0xFF;
    }

    /**
     * Returns the Mid Byte (bits 15..8) mapped to Register R6.
     */
    public static int getMidByte(int crc) {
        return (crc >> 8) & 0xFF;
    }

    /**
     * Returns the Low Byte (bits 7..0) mapped to Register R7 and Accumulator A.
     */
    public static int getLowByte(int crc) {
        return crc & 0xFF;
    }

    /**
     * Formats 24-bit integer into canonical hex string "0xXXXXXX".
     */
    public static String formatHex(int crc) {
        return String.format("0x%06X", crc & CRC_MASK);
    }

    /**
     * Maps the 24-bit CRC into STC89C52 registers (R5, R6, R7, and Accumulator A).
     */
    public static void applyToRegisters(Registers regs, PSW psw, int crc) {
        int activeBank = psw.getRs0() | (psw.getRs1() << 1);
        int high = getHighByte(crc);
        int mid = getMidByte(crc);
        int low = getLowByte(crc);

        regs.setR(5, activeBank, high);
        regs.setR(6, activeBank, mid);
        regs.setR(7, activeBank, low);
        regs.A = low;
        psw.updateParity(regs.A);
    }
}
