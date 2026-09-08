import java.util.LinkedList;
import java.util.Queue;
        
        
    
public class DataStructures {
    private int[] stack;
    private int sp;
    
    public DataStructures(int size){
        stack=new int[size];
        sp=-1;
    }
    public void push(int value){
        stack[++sp]=value;

    }
    public int pop(){
        return stack[sp--];
    }
    public static void main (String [] args){
        Queue<Integer> queue=new LinkedList<>();
        DataStructures myStack= new DataStructures(3);
        myStack.push(10);
        myStack.push(20);
        System.out.println(myStack.pop());

}
    }