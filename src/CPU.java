public class CPU {
    public Registers registers=new Registers();
    public Memory memory=new Memory();
    public PSW psw=new PSW();
    public DataStructures datastructures=new DataStructures(128, 16);

    public void step() {
        int opcode=memory.readROM(registers.PC);
        registers.PC=(registers.PC + 1) & 0x1FFF; 
        execute(opcode);
    }

    public void execute(int opcode) {
        switch (opcode) {
            case 0x00:
                break;
            case 0x04: 
                registers.A=(registers.A + 1) & 0xFF;
                psw.updateParity(registers.A);
                break;
            case 0x14: 
                registers.A=(registers.A - 1) & 0xFF;
                psw.updateParity(registers.A);
                break;
            case 0x74: 
                int data=memory.readROM(registers.PC);
                registers.PC=(registers.PC + 1) & 0x1FFF;
                registers.A=data & 0xFF;
                psw.updateParity(registers.A);
                break;
            case 0xC0: 
                int pushAddr=memory.readROM(registers.PC);
                registers.PC=(registers.PC + 1) & 0x1FFF;
                int pushVal=memory.readRAM(pushAddr);
                datastructures.push(pushVal);
                break;
            case 0xD0: 
                int popAddr=memory.readROM(registers.PC);
                registers.PC=(registers.PC + 1) & 0x1FFF;
                int popVal=datastructures.pop();
                memory.writeRAM(popAddr, popVal);
                break;
            case 0xFF: 
                break;
            default:
                throw new IllegalArgumentException("Unknown opcode: 0x" + Integer.toHexString(opcode));
            }
    }
}