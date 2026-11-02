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
            case 0x24:
                currInstrcName = "ADD A, #data";
                break;
            case 0x04:
                currInstrcName = "INC A";
                break;
            case 0x54:
                currInstrcName = "ANL A, #data";
                break;
            case 0x80:
                currInstrcName = "SJMP rel";
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

            case 0x24: { // ADD A, #data
                int before = regs.A;
                int operand = fetchOper();
                int result = before + operand;
                psw.updateAfterAdd(before, operand, result);
                updateAcc(result);
                statechangelog += " (CY=" + psw.isCy() + " OV=" + psw.isOv() + ")";
                break;
            }

            case 0x04: // INC A
                updateAcc(regs.A + 1);
                break;

            case 0x54: // ANL A, #data
                updateAcc(regs.A & fetchOper());
                break;

            case 0x80: { // SJMP rel
                int offset = fetchOper();
                if (offset > 127) offset -= 256;
                pc += offset;
                statechangelog = "PC : " + toHex(pc, 4);
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