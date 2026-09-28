public class Registers {
    public int A = 0; // Accumulator — most arithmetic goes through A
    public int B = 0; // B register — used as secondary operand for MUL/DIV

    // 4 register banks, each with 8 general-purpose registers (R0–R7)
    public int[][] R = new int[4][8];

    public int getR(int index, int bank) {
        return R[bank][index];
    }

    public void setR(int index, int bank, int val) {
        // 0xFF mask keeps the integer behaving like real 8-bit hardware.
        R[bank][index] = val & 0xFF;
    }
}