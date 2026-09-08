public class Memory {
 private byte[] codeMemory = new  byte[8192];
 
 private byte[] internalRAM = new byte[256];

 public int readRAM(int address) {
    return internalRAM[address] & 0xFF;
 }

 public void writeRAM(int address, int value){
    internalRAM[address]=(byte) value;
 }

 public int readCode(int address) {
    return codeMemory[address] & 0xFF;
 }
}
