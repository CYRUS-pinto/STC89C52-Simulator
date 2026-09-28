public class Memory {
    private byte[] codeMemory  = new byte[8192];  // 8 KB program (code) memory
    private byte[] internalRAM = new byte[256];   // 256-byte internal data RAM

    // ── Read / Write internal RAM ──────────────────────────────────────────
    public int readRAM(int address) {
        return internalRAM[address] & 0xFF;
    }

    public void writeRAM(int address, int value) {
        internalRAM[address] = (byte) value;
    }

    // ── Read code (program) memory ─────────────────────────────────────────
    public int readCode(int address) {
        return codeMemory[address] & 0xFF;
    }

    // ── Load a bytecode program into code memory starting at address 0 ─────
    // Called by CoreProcess.initialize() every time a program is loaded/reset.
    public void loadProgram(byte[] program) {
        // Clear previous program first so stale bytes don't interfere.
        java.util.Arrays.fill(codeMemory, (byte) 0x00);
        System.arraycopy(program, 0, codeMemory, 0, program.length);
    }

    // ── Dump a window of code memory for display ───────────────────────────
    public String dumpCode(int startAddr, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length && (startAddr + i) < codeMemory.length; i++) {
            if (i % 8 == 0) sb.append(String.format("%04X: ", startAddr + i));
            sb.append(String.format("%02X ", codeMemory[startAddr + i] & 0xFF));
            if ((i + 1) % 8 == 0) sb.append("\n");
        }
        return sb.toString().trim();
    }

    // ── Dump a window of internal RAM for display ──────────────────────────
    public String dumpRAM(int startAddr, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length && (startAddr + i) < internalRAM.length; i++) {
            if (i % 8 == 0) sb.append(String.format("%02X: ", startAddr + i));
            sb.append(String.format("%02X ", internalRAM[startAddr + i] & 0xFF));
            if ((i + 1) % 8 == 0) sb.append("\n");
        }
        return sb.toString().trim();
    }
}

