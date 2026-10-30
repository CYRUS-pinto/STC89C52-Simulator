public class CoreProcess{
    public CPU cpu=new CPU();
    public boolean isRunning=false;
    public boolean isHalted=false;
    
    public void run(){
    isRunning=true;
    while(isRunning && !isHalted){
        cpu.step();
    }
}

public void pause(){
    isRunning=false;
}
public void reset(){
    isRunning=false;
    isHalted=false;
    cpu.registers.reset();
    cpu.psw.reset();
    cpu.datastructures.reset();
}
}
