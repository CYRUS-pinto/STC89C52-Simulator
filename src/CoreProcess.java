import java.io.*;
import java.net.*;

public class CoreProcess {
    private static Registers regs;
    private static Memory mem;
    private static DataStructures ds;
    private static PSW psw;
    private static CPU cpu;
    private static byte[] program;

    private static void initialize() {
        regs = new Registers();
        mem = new Memory();
        ds = new DataStructures(256, 16);
        psw = new PSW();
        cpu = new CPU(regs, mem, ds, psw);

        // Standard verification bytecode test program
        program = new byte[] {
            0x74, (byte) 0xFE, // MOV A, #254
            0x24, 0x03,        // ADD A, #3  (Causes Carry)
            0x04,              // INC A
            0x54, 0x0F,        // ANL A, #0x0F
            (byte) 0xE0,       // ENQUEUE A
            0x74, 0x09,        // MOV A, #9
            (byte) 0xE0,       // ENQUEUE A
            (byte) 0xD0,       // DEQUEUE A
            (byte) 0xFF        // HALT
        };
        mem.loadProgram(program);
    }

    public static void main(String[] args) {
        initialize();

        try {
            System.out.println("[Core] Connecting to LoggerProcess on port 5001...");
            Socket loggerSocket = new Socket("127.0.0.1", 5001);
            PrintWriter toLogger = new PrintWriter(loggerSocket.getOutputStream(), true);

            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("[Core] Server listening for UIProcess on port 5000...");
            Socket uiSocket = serverSocket.accept();
            BufferedReader fromUI = new BufferedReader(new InputStreamReader(uiSocket.getInputStream()));
            PrintWriter toUI = new PrintWriter(uiSocket.getOutputStream(), true);

            String command;
            while ((command = fromUI.readLine()) != null) {
                command = command.trim().toUpperCase();

                if (command.equals("QUIT")) {
                    toLogger.println("LOG|SHUTDOWN|CoreProcess termination requested.");
                    break;
                } else if (command.equals("RESET")) {
                    initialize();
                    toUI.println("RESET_DONE");
                    toLogger.println("LOG|RESET|System state reset.");
                } else if (command.equals("STEP")) {
                    if (!cpu.isHalt) {
                        cpu.step();
                        emitState(toUI, toLogger);
                    }
                    if (cpu.isHalt) {
                        toUI.println("HALTED");
                    }
                } else if (command.equals("RUN")) {
                    while (!cpu.isHalt) {
                        cpu.step();
                        emitState(toUI, toLogger);
                    }
                    toUI.println("HALTED");
                }
            }

            uiSocket.close();
            serverSocket.close();
            loggerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void emitState(PrintWriter toUI, PrintWriter toLogger) {
        String state = String.format("STATE|PC=%04X|A=%02X|B=%02X|CY=%b|OV=%b|SP=%02X|Q_SIZE=%d",
                cpu.pc, regs.A, regs.B, psw.isCy(), psw.isOv(), ds.sp, ds.qSize);
        toUI.println(state);
        toLogger.println("LOG|EXEC|PC=" + Integer.toHexString(cpu.pc) + "|A=" + regs.A + "|CY=" + psw.isCy());
    }
}