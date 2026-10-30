public class PSW{
    public boolean CY=false;
    public boolean AC=false;
    public boolean OV=false;
    public boolean P=false;

    public void reset() {
        CY = false;
        AC = false;
        OV = false;
        P = false;
    }
public void updateAddFlags(int accBefore, int operand, int sum){
    this.CY=(sum>0xFF);
    this.AC=((accBefore & 0x0F)+(operand & 0x0F)) >0x0F;
    int result=sum & 0xFF;
    this.OV=(((accBefore^result) & (operand^result) & 0x80) !=0);
    updateParity(result);
}
public void updateParity(int accValue){
    int count=Integer.bitCount(accValue & 0xFF);
    this.P=(count%2 !=0);
}    
}