public class PSW {
    private boolean cy;
    private boolean ac;
    private boolean f0;
    private boolean ov;
    private boolean p;
    private int rs1;
    private int rs0;

    public PSW() {
        reset();
    }

    public void reset() {
        cy = false;
        ac = false;
        f0 = false;
        ov = false;
        p = false;
        rs1 = 0;
        rs0 = 0;
    }

    public boolean isCy() { return cy; }
    public boolean isAc() { return ac; }
    public boolean isF0() { return f0; }
    public boolean isOv() { return ov; }
    public boolean isP()  { return p; }
    public int getRs1()   { return rs1; }
    public int getRs0()   { return rs0; }

    public void setBankSelectBits(int bank) {
        rs1 = (bank >> 1) & 0x01;
        rs0 = bank & 0x01;
    }

    public void updateAfterAdd(int operand1, int operand2, int result) {
        // Carry Flag: result overflows past 8 bits (> 255)
        cy = result > 0xFF;

        // Auxiliary Carry: carry out of nibble (bit 3 to bit 4)
        ac = ((operand1 & 0x0F) + (operand2 & 0x0F)) > 0x0F;

        // Overflow Flag: signed two's complement overflow
        boolean sign1 = (operand1 & 0x80) != 0;
        boolean sign2 = (operand2 & 0x80) != 0;
        boolean signR = ((result & 0xFF) & 0x80) != 0;
        ov = (sign1 == sign2) && (sign1 != signR);
    }

    public void updateParity(int value) {
        int bits = 0;
        int val = value & 0xFF;
        while (val > 0) {
            bits += (val & 1);
            val >>= 1;
        }
        p = (bits % 2 != 0);
    }
}