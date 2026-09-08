public class Registers {
    public int A = 0; // The Accumulator. Most math happens here.
    public int[][] R = new int[4][8]; // 4 banks of 8 registers (R0-R7)
    
    public int getR(int index, int bank) {
        return R[bank][index];
    }
    
    public void setR(int index, int bank, int val) {
        // Engineering lesson: Java integers hold billions. 
        // 0xFF (255) forces the integer to act like 8-bit hardware.
        R[bank][index] = val & 0xFF; 
    }
}