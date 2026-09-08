public class CPU {
    public Registers regs;
    public Memory mem;
    public DataStructures ds;
    public int pc=0;
    public boolean isHalt=false;
    private int currOpcode=0;
    private String currInstrcName;
    private String statechangelog;
    public CPU(Registers regs, Memory mem, DataStructures ds){
        this.regs=regs;
        this.mem=mem;
        this.ds=ds;
    }
private String toHex(int value, int digits) {
    return String.format("%0" + digits + "X", value);
}
    public void step(){
        if(isHalt){
            return;//exit 
        }
        System.out.println("PC: " + toHex(pc, 4));
        fetch();
        System.out.println("Fetched !: (check byte" + toHex(currOpcode, 2) + ") from mem");
        decode();
        System.out.println("Decoded !: (check byte" + toHex(currOpcode, 2) + ") from mem");
        execute();
        System.out.println("Decoded: " + currInstrcName +"(opcode" + toHex(currOpcode, 2) + ")");
        System.out.println("State:"+ statechangelog);
    }
private void fetch(){
    currOpcode=mem.readCode(pc);
    pc++;
}
private void decode(){
    switch(currOpcode){
        case 0x74:
            currInstrcName="MOV A, #data";
            break;
        case 0x24:
            currInstrcName="ADD A, #data";
            break;
        case 0x04:
            currInstrcName="INC A";
            break;
        case 0x54:
            currInstrcName="ANL A, #data";
            break;
        case 0x80:
            currInstrcName="SJMP rel";
            break;
        case 0xE0:
            currInstrcName="Enqueue";
            break;
        case 0xD0:
            currInstrcName="Dequeue";
            break;
        case 0xFF:
            currInstrcName="HALT";
            break;
        default:
            currInstrcName="Unknown";
            break;
    }
}
private int fetchOper() {
        int operand = mem.readCode(pc);
        pc++;
        return operand;
    }
    private void updateAcc(int newValue) {
        int before = regs.A;
        regs.A = newValue & 0xFF; 
        statechangelog = "A : " + toHex(before, 2) + " -> " + toHex(regs.A, 2);
    }
private void execute(){
    switch(currOpcode){
        case 0x74:
           updateAcc(fetchOper());
            break;
        case 0x24:
            updateAcc(regs.A + fetchOper());
            break;
        case 0x04:
            updateAcc(regs.A + 1);
            break;
        case 0x54:
            updateAcc(regs.A & fetchOper());
            break;
        case 0x80:
            int offset = fetchOper();
            if (offset>127) offset -=256;
            pc += offset;
            statechangelog = "PC : " + toHex(pc, 4);
            break;
        case 0xE0:
            ds.enqueue(regs.A);
            statechangelog = "Enqueued A="+ regs.A;
            break;
        case 0xD0:
            int dequeued= ds.dequeue();
            updateAcc(dequeued);
            statechangelog = statechangelog + " (Dequeued " + dequeued + ")";
            break;
        case 0xFF:
            isHalt=true;
            statechangelog = "CPU Halted";
            break;
        default:
            statechangelog = "Unknown opcode"+ toHex(currOpcode, 2);
            isHalt=true;
            break;
    }

}