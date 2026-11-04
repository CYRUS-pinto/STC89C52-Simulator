public class Memory{
    private final int[] codeMemory=new int[8192];
    private final int[] internalRAM=new int[256];

public int readROM(int address){
    if (address<0||address>=8192){
        throw new IllegalArgumentException("ROM address out of bounds:"+address);
    }
    return codeMemory[address] & 0xFF;
}
public void writeROM(int address,int data){
    if (address<0||address>=8192){
        throw new IllegalArgumentException("ROM address out of bounds:"+address);
    }
    codeMemory[address]=data & 0xFF;
}
public int readRAM(int address){
    if(address<0||address>=256){
        throw new IllegalArgumentException("RAM address out of bounds:"+address);
    }
    return internalRAM[address] & 0xFF;
}
public void writeRAM(int address,int data){
    if (address<0||address>=256){
        throw new IllegalArgumentException("RAM address out of bounds:"+address);
    }
    internalRAM[address]=data &0xFF;
}
public void loadProgram(int[] program) {
        java.util.Arrays.fill(codeMemory, 0);
        for (int i=0; i<program.length && i<8192; i++) {
            codeMemory[i]=program[i] & 0xFF;
        }
    }
}
