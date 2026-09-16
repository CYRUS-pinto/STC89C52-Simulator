package src;

public class Registers {
    // 4 banks of 8 registers each (R0 - R7)
    private int[][] banks;
    private int currentBank;

    // Program Status Word (PSW) flags
    private boolean cy; // Carry Flag
    private boolean ac; // Auxiliary Carry Flag
    private boolean f0; // Flag 0
    private int rs1;    // Register Bank Select 1
    private int rs0;    // Register Bank Select 0
    private boolean ov; // Overflow Flag
    private boolean p;  // Parity Flag

    public Registers() {
        banks = new int[4][8];
        currentBank = 0;
        reset();
    }

    public void reset() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                banks[i][j] = 0;
            }
        }
        currentBank = 0;
        cy = false;
        ac = false;
        f0 = false;
        rs1 = 0;
        rs0 = 0;
        ov = false;
        p = false;
    }

    // Read a register from the current active bank
    public int read(int regIndex) {
        validateRegister(regIndex);
        return banks[currentBank][regIndex];
    }

    // Write to a register with 8-bit wrap-around enforcement
    public void write(int regIndex, int value) {
        validateRegister(regIndex);
        banks[currentBank][regIndex] = value & 0xFF;
    }

    // Read from a specific bank directly
    public int readFromBank(int bank, int regIndex) {
        validateBank(bank);
        validateRegister(regIndex);
        return banks[bank][regIndex];
    }

    // Write to a specific bank directly
    public void writeToBank(int bank, int regIndex, int value) {
        validateBank(bank);
        validateRegister(regIndex);
        banks[bank][regIndex] = value & 0xFF;
    }

    public void setSelectBank(int bank) {
        validateBank(bank);
        this.currentBank = bank;
        this.rs1 = (bank >> 1) & 0x01;
        this.rs0 = bank & 0x01;
    }

    public int getCurrentBank() {
        return currentBank;
    }

    // Flag Getters and Setters
    public boolean isCy() { return cy; }
    public void setCy(boolean cy) { this.cy = cy; }

    public boolean isAc() { return ac; }
    public void setAc(boolean ac) { this.ac = ac; }

    public boolean isF0() { return f0; }
    public void setF0(boolean f0) { this.f0 = f0; }

    public boolean isOv() { return ov; }
    public void setOv(boolean ov) { this.ov = ov; }

    public boolean isP() { return p; }
    public void setP(boolean p) { this.p = p; }

    // Update Parity flag automatically based on an 8-bit value
    public void updateParity(int value) {
        int bits = 0;
        int val = value & 0xFF;
        while (val > 0) {
            bits += (val & 1);
            val >>= 1;
        }
        this.p = (bits % 2 != 0);
    }

    private void validateRegister(int regIndex) {
        if (regIndex < 0 || regIndex > 7) {
            throw new IllegalArgumentException("Register index must be between 0 and 7.");
        }
    }

    private void validateBank(int bank) {
        if (bank < 0 || bank > 3) {
            throw new IllegalArgumentException("Bank index must be between 0 and 3.");
        }
    }
}
