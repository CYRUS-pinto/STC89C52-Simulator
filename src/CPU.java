public class CPU {
    public Registers regs;
    public Memory mem;
    public DataStructures ds;
    public int pc=0;
    public boolean isHalt=false;
    private int currOpcode=0;
    private String currInstrcName;
    private String statechangelog;
    
}