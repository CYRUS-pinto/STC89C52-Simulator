public class CPU {
    public Registers regs;
    public Memory mem;
    public DataStructures ds;
    public PSW psw;

    public int pc = 0;
    public boolean isHalt = false;
    private int currOpcode = 0;
    private String currInstrcName;
    private String statechangelog;
    public String fetchLog = "";
    public String decodeLog = "";

    public int getCurrOpcode() { return currOpcode; }
    public String getCurrInstrcName() { return currInstrcName; }
    public String getStatechangelog() { return statechangelog; }

    public CPU(Registers regs, Memory mem, DataStructures ds, PSW psw) {
        this.regs = regs;
        this.mem = mem;
        this.ds = ds;
        this.psw = psw;
    }

    private String toHex(int value, int digits) {
        return String.format("%0" + digits + "X", value);
    }

    public void step() {
        if (isHalt) {
            return;
        }
        System.out.println("PC: " + toHex(pc, 4));
        fetch();
        System.out.println("Fetched !: (check byte " + toHex(currOpcode, 2) + ") from mem");
        decode();
        System.out.println("Decoded !: (check byte " + toHex(currOpcode, 2) + ") from mem");
        execute();
        System.out.println("Executed: " + currInstrcName + " (opcode " + toHex(currOpcode, 2) + ")");
        System.out.println("State: " + statechangelog);
    }

    private void fetch() {
        int fetchPC = pc;
        currOpcode = mem.readCode(pc);
        pc++;
        fetchLog = "PC=0x" + toHex(fetchPC, 4) + " -> Opcode 0x" + toHex(currOpcode, 2);
    }

    private void decode() {
        switch (currOpcode) {
            case 0x00:
                currInstrcName = "NOP";
                break;
            case 0x74:
                currInstrcName = "MOV A, #data";
                break;
            case 0x75:
                currInstrcName = "MOV direct, #data";
                break;
            case 0x24:
                currInstrcName = "ADD A, #data";
                break;
            case 0x94:
                currInstrcName = "SUBB A, #data";
                break;
            case 0xA4:
                currInstrcName = "MUL AB";
                break;
            case 0x84:
                currInstrcName = "DIV AB";
                break;
            case 0x04:
                currInstrcName = "INC A";
                break;
            case 0x14:
                currInstrcName = "DEC A";
                break;
            case 0x54:
                currInstrcName = "ANL A, #data";
                break;
            case 0x44:
                currInstrcName = "ORL A, #data";
                break;
            case 0x64:
                currInstrcName = "XRL A, #data";
                break;
            case 0x60:
                currInstrcName = "JZ rel";
                break;
            case 0x70:
                currInstrcName = "JNZ rel";
                break;
            case 0x80:
                currInstrcName = "SJMP rel";
                break;
            case 0xC0:
                currInstrcName = "PUSH direct";
                break;
            case 0xE0:
                currInstrcName = "Enqueue";
                break;
            case 0xD0:
                currInstrcName = "Dequeue";
                break;
            case 0xFF:
                currInstrcName = "HALT";
                break;
            default:
                currInstrcName = "Unknown";
                break;
        }
        decodeLog = currInstrcName + " [0x" + toHex(currOpcode, 2) + "]";
    }

    private int fetchOper() {
        int operand = mem.readCode(pc);
        pc++;
        return operand;
    }

    private void updateAcc(int newValue) {
        int before = regs.A;
        regs.A = newValue & 0xFF;
        psw.updateParity(regs.A);
        statechangelog = "A : " + toHex(before, 2) + " -> " + toHex(regs.A, 2);
    }

    private void execute() {
        switch (currOpcode) {
            case 0x00: // NOP
                statechangelog = "NOP";
                break;

            case 0x74: // MOV A, #data
                updateAcc(fetchOper());
                break;

            case 0x75: { // MOV direct, #data
                int dir = fetchOper();
                int val = fetchOper();
                if (dir == 0xF0) {
                    regs.B = val & 0xFF;
                    statechangelog = "B : -> " + toHex(regs.B, 2);
                } else if (dir == 0xE0) {
                    updateAcc(val);
                } else {
                    mem.writeRAM(dir, val);
                    statechangelog = "RAM[0x" + toHex(dir, 2) + "] : -> " + toHex(val, 2);
                }
                break;
            }

            case 0x24: { // ADD A, #data
                int before = regs.A;
                int operand = fetchOper();
                int result = before + operand;
                psw.updateAfterAdd(before, operand, result);
                updateAcc(result);
                statechangelog += " (CY=" + psw.isCy() + " OV=" + psw.isOv() + ")";
                break;
            }

            case 0x94: { // SUBB A, #data
                int before = regs.A;
                int operand = fetchOper();
                int borrow = psw.isCy() ? 1 : 0;
                int result = before - operand - borrow;
                psw.setCy(result < 0);
                boolean sign1 = (before & 0x80) != 0;
                boolean sign2 = (operand & 0x80) != 0;
                boolean signR = ((result & 0xFF) & 0x80) != 0;
                psw.setOv((sign1 != sign2) && (sign1 != signR));
                updateAcc(result);
                statechangelog += " (CY=" + psw.isCy() + " OV=" + psw.isOv() + ")";
                break;
            }

            case 0xA4: { // MUL AB
                int prod = regs.A * regs.B;
                updateAcc(prod & 0xFF);
                regs.B = (prod >> 8) & 0xFF;
                psw.setOv(prod > 0xFF);
                psw.setCy(false);
                statechangelog = "MUL AB: A=" + toHex(regs.A, 2) + " B=" + toHex(regs.B, 2) + " OV=" + psw.isOv();
                break;
            }

            case 0x84: { // DIV AB
                if (regs.B == 0) {
                    psw.setOv(true);
                    psw.setCy(false);
                    statechangelog = "DIV AB: Divide by zero (OV=true)";
                } else {
                    int quot = regs.A / regs.B;
                    int rem = regs.A % regs.B;
                    updateAcc(quot);
                    regs.B = rem & 0xFF;
                    psw.setOv(false);
                    psw.setCy(false);
                    statechangelog = "DIV AB: A=" + toHex(regs.A, 2) + " B=" + toHex(regs.B, 2);
                }
                break;
            }

            case 0x04: // INC A
                updateAcc(regs.A + 1);
                break;

            case 0x14: // DEC A
                updateAcc(regs.A - 1);
                break;

            case 0x54: // ANL A, #data
                updateAcc(regs.A & fetchOper());
                break;

            case 0x44: // ORL A, #data
                updateAcc(regs.A | fetchOper());
                break;

            case 0x64: // XRL A, #data
                updateAcc(regs.A ^ fetchOper());
                break;

            case 0x60: { // JZ rel (Jump if Accumulator == 0)
                int offset = fetchOper();
                if (offset > 127) offset -= 256;
                if (regs.A == 0) {
                    pc += offset;
                    statechangelog = "JZ taken -> PC=" + toHex(pc, 4);
                } else {
                    statechangelog = "JZ not taken (A=" + toHex(regs.A, 2) + ")";
                }
                break;
            }

            case 0x70: { // JNZ rel (Jump if Accumulator != 0)
                int offset = fetchOper();
                if (offset > 127) offset -= 256;
                if (regs.A != 0) {
                    pc += offset;
                    statechangelog = "JNZ taken -> PC=" + toHex(pc, 4);
                } else {
                    statechangelog = "JNZ not taken (A=00)";
                }
                break;
            }

            case 0x80: { // SJMP rel
                int offset = fetchOper();
                if (offset > 127) offset -= 256;
                pc += offset;
                if (offset == -2) {
                    // Standard 8051 idiom SJMP $ (80 FE) self-loop halt
                    isHalt = true;
                    statechangelog = "PC : " + toHex(pc, 4) + " (SJMP $ Halted)";
                } else {
                    statechangelog = "PC : " + toHex(pc, 4);
                }
                break;
            }

            case 0xC0: { // PUSH direct
                int dir = fetchOper();
                int val = (dir == 0xE0) ? regs.A : (dir == 0xF0) ? regs.B : mem.readRAM(dir);
                ds.push(val);
                mem.writeRAM(ds.sp, val);
                statechangelog = "PUSH 0x" + toHex(val, 2) + " to Stack (SP=" + toHex(ds.sp, 2) + ")";
                break;
            }

            case 0xE0: // Enqueue A
                ds.enqueue(regs.A);
                statechangelog = "Enqueued A=" + regs.A;
                break;

            case 0xD0: { // Dequeue into A
                int dequeued = ds.dequeue();
                updateAcc(dequeued);
                statechangelog += " (Dequeued " + dequeued + ")";
                break;
            }

            case 0xFF: // HALT
                isHalt = true;
                statechangelog = "CPU Halted";
                break;

            default:
                statechangelog = "Unknown opcode " + toHex(currOpcode, 2);
                isHalt = true;
                break;
        }
    }
}
