public class Registers {
    public int A = 0;
    public int[][] R = new int[4][8];
    public int getR(int index, int bank) { return R[bank][index]; }
    public void setR(int index, int bank, int val) { R[bank][index] = val & 0xFF; }
}