public class DataStructures {
    public int[] stack;
    public int sp;

    public int[] queue;
    public int qFront;
    public int qRear;
    public int qSize;
    public int qCapacity;

    public DataStructures(int stackSize, int queueCapacity) {
        stack = new int[stackSize];
        sp = 0x07; // Standard 8051 reset default offset

        qCapacity = queueCapacity;
        queue = new int[queueCapacity];
        qFront = 0;
        qRear = 0;
        qSize = 0;
    }

    public void reset() {
        sp = 0x07;
        qFront = 0;
        qRear = 0;
        qSize = 0;
    }

    // Call Stack Operations
    public void push(int val) {
        sp++;
        if (sp >= stack.length) throw new RuntimeException("Stack Overflow: SP=" + sp);
        stack[sp] = val & 0xFF;
    }

    public int pop() {
        if (sp < 0x07) throw new RuntimeException("Stack Underflow: SP=" + sp);
        int val = stack[sp];
        sp--;
        return val;
    }

    // Circular Queue Operations
    public void enqueue(int val) {
        if (isQueueFull()) throw new RuntimeException("Circular Queue Overflow");
        queue[qRear] = val & 0xFF;
        qRear = (qRear + 1) % qCapacity;
        qSize++;
    }

    public int dequeue() {
        if (isQueueEmpty()) throw new RuntimeException("Circular Queue Underflow");
        int val = queue[qFront];
        qFront = (qFront + 1) % qCapacity;
        qSize--;
        return val;
    }

    public boolean isQueueEmpty() { return qSize == 0; }
    public boolean isQueueFull()  { return qSize == qCapacity; }
}