public class Registers {
    public int A=0;
    public int B=0;
    public int[] R=new int[8];
    public int PC=0x0000;

public void reset(){
    A=0;
    B=0;
    for(int i=0;i<8;i++){
        R[i]=0;
    }
    PC=0x0000;
}
public void setPC(int address) {
    this.PC = address & 0x1FFF; 
}
public int getR(int index){
    if(index<0||index>7){
        throw new IllegalArgumentException("Register index must be between 0 and 7");
    }
    return R[index] & 0xFF;
}
public void setR(int index, int value){
    if(index<0||index>7){
        throw new IllegalArgumentException("Register index must be between 0 and 7");
    }
    R[index]=value & 0xFF;
}
}