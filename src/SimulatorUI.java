import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * SimulatorUI — Shashidhara (25190148)
 *
 * Week 4 requirement: UI Process running as a SEPARATE process.
 * - Connects to CoreProcess on localhost:5000 via TCP socket (IPC).
 * - Sends commands: STEP, RUN, RESET, QUIT
 * - Receives STATE| messages and HALTED signal from Core.
 * - Displays: PC, A, B, CY, OV, SP, Q_SIZE, execution trace with
 *   FETCH → DECODE → EXECUTE checkmarks.
 *
 * IPC Mechanism: TCP Sockets.
 * Reason: Same as Core↔Logger — reliable, ordered, process-boundary-safe.
 */
public class SimulatorUI extends JFrame {

    // ── IPC connection fields ─────────────────────────────────────────────
    private Socket        coreSocket;
    private PrintWriter   toCore;
    private BufferedReader fromCore;

    // ── UI Component References ────────────────────────────────────────────

    // Register / state display labels
    private JLabel lblPC    = makeValueLabel("0000");
    private JLabel lblA     = makeValueLabel("00");
    private JLabel lblB     = makeValueLabel("00");
    private JLabel lblCY    = makeValueLabel("0");
    private JLabel lblOV    = makeValueLabel("0");
    private JLabel lblSP    = makeValueLabel("07");
    private JLabel lblQSize = makeValueLabel("0");

    // Fetch → Decode → Execute trace indicators
    private JLabel fetchIndicator   = makeStageLabel();
    private JLabel decodeIndicator  = makeStageLabel();
    private JLabel executeIndicator = makeStageLabel();

    // Execution trace log
    private JTextPane tracePane;
    private StyledDocument traceDoc;

    // Status bar
    private JLabel statusBar;

    // Control buttons
    private JButton btnLoad, btnStep, btnRun, btnReset, btnQuit;

    // Step counter
    private int stepCount = 0;

    // ── Entry point ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Run on the Event Dispatch Thread (Swing rule)
        SwingUtilities.invokeLater(() -> {
            SimulatorUI ui = new SimulatorUI();
            ui.setVisible(true);
        });
    }

    // ── Constructor ────────────────────────────────────────────────────────
    public SimulatorUI() {
        super("STC89C52 Microcontroller Simulator — Team Bondaas");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { quit(); }
        });

        buildUI();
        pack();
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);   // center on screen

        // Connect to CoreProcess in the background so the UI appears immediately
        connectToCore();
    }

    // ── Build the Swing layout ─────────────────────────────────────────────
    private void buildUI() {
        // ── Color palette (dark-mode style) ───────────────────────────────
        Color bg        = new Color(30,  30,  46);
        Color panel     = new Color(45,  45,  62);
        Color accent    = new Color(137, 180, 250);  // blue
        Color green     = new Color(166, 227, 161);
        Color yellow    = new Color(249, 226, 175);
        Color red       = new Color(243, 139, 168);
        Color textFg    = new Color(205, 214, 244);

        getContentPane().setBackground(bg);
        setLayout(new BorderLayout(10, 10));

        // ── TOP TOOLBAR ────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(panel);
        toolbar.setBorder(new EmptyBorder(4, 8, 4, 8));

        btnLoad  = makeButton("⬆  Load",  new Color(98, 160, 234));
        btnStep  = makeButton("⏭  Step",  new Color(166, 227, 161));
        btnRun   = makeButton("▶  Run",   new Color(166, 227, 161));
        btnReset = makeButton("↺  Reset", new Color(249, 226, 175));
        btnQuit  = makeButton("✕  Quit",  new Color(243, 139, 168));

        // Wire button actions
        btnLoad.addActionListener(e -> sendCommand("RESET"));   // Load resets state
        btnStep.addActionListener(e -> sendStep());
        btnRun.addActionListener(e  -> sendRun());
        btnReset.addActionListener(e -> sendCommand("RESET"));
        btnQuit.addActionListener(e  -> quit());

        toolbar.add(btnLoad);
        toolbar.add(btnStep);
        toolbar.add(btnRun);
        toolbar.add(btnReset);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(btnQuit);
        add(toolbar, BorderLayout.NORTH);

        // ── CENTER: register panel + trace panel side by side ─────────────
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setBackground(bg);
        center.setBorder(new EmptyBorder(0, 10, 0, 10));

        // Left: registers + flags + FDE indicators
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(bg);
        leftPanel.setPreferredSize(new Dimension(270, 0));

        leftPanel.add(buildRegistersPanel(panel, accent, textFg));
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(buildFlagsPanel(panel, yellow, textFg));
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(buildDataStructPanel(panel, green, textFg));
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(buildFDEPanel(panel, textFg));
        leftPanel.add(Box.createVerticalGlue());

        // Right: execution trace
        JPanel rightPanel = buildTracePanel(panel, textFg, bg);

        center.add(leftPanel,  BorderLayout.WEST);
        center.add(rightPanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // ── BOTTOM STATUS BAR ──────────────────────────────────────────────
        statusBar = new JLabel("  Connecting to CoreProcess on port 5000...");
        statusBar.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusBar.setForeground(new Color(166, 173, 200));
        statusBar.setBackground(new Color(24, 24, 37));
        statusBar.setOpaque(true);
        statusBar.setBorder(new EmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);
    }

    // ── Panel builders ─────────────────────────────────────────────────────

    private JPanel buildRegistersPanel(Color bg, Color accent, Color fg) {
        JPanel p = createGroupPanel("CPU Registers", bg, accent);
        p.setLayout(new GridLayout(0, 2, 6, 4));

        // Row: name | value
        p.add(makeRegLabel("Program Counter (PC)", fg));  p.add(lblPC);
        p.add(makeRegLabel("Accumulator (A)", fg));        p.add(lblA);
        p.add(makeRegLabel("B Register", fg));             p.add(lblB);

        return wrapInCard("CPU Registers", p, bg, accent);
    }

    private JPanel buildFlagsPanel(Color bg, Color accent, Color fg) {
        JPanel p = createGroupPanel("PSW Flags", bg, accent);
        p.setLayout(new GridLayout(0, 2, 6, 4));

        p.add(makeRegLabel("Carry (CY)", fg));       p.add(lblCY);
        p.add(makeRegLabel("Overflow (OV)", fg));    p.add(lblOV);

        return wrapInCard("PSW / Status Flags", p, bg, accent);
    }

    private JPanel buildDataStructPanel(Color bg, Color accent, Color fg) {
        JPanel p = createGroupPanel("Data Structures", bg, accent);
        p.setLayout(new GridLayout(0, 2, 6, 4));

        p.add(makeRegLabel("Stack Ptr (SP)", fg));   p.add(lblSP);
        p.add(makeRegLabel("Queue Size", fg));        p.add(lblQSize);

        return wrapInCard("Stack & Queue", p, bg, accent);
    }

    private JPanel buildFDEPanel(Color bg, Color fg) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(88, 91, 112), 1, true),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel title = new JLabel("Execution Trace");
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setForeground(new Color(180, 190, 254));
        card.add(title, BorderLayout.NORTH);

        JPanel stages = new JPanel(new GridLayout(3, 2, 6, 4));
        stages.setBackground(bg);

        stages.add(makeRegLabel("FETCH", fg));   stages.add(fetchIndicator);
        stages.add(makeRegLabel("DECODE", fg));  stages.add(decodeIndicator);
        stages.add(makeRegLabel("EXECUTE", fg)); stages.add(executeIndicator);

        card.add(stages, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTracePanel(Color bg, Color fg, Color mainBg) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(mainBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(88, 91, 112), 1, true),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel title = new JLabel("Execution Log");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(new Color(180, 190, 254));
        panel.add(title, BorderLayout.NORTH);

        tracePane = new JTextPane();
        tracePane.setEditable(false);
        tracePane.setBackground(new Color(24, 24, 37));
        tracePane.setForeground(fg);
        tracePane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        traceDoc = tracePane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(tracePane);
        scroll.setPreferredSize(new Dimension(520, 400));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Helper component builders ──────────────────────────────────────────

    private JPanel wrapInCard(String titleText, JPanel inner, Color bg, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(88, 91, 112), 1, true),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setForeground(accent);
        card.add(title, BorderLayout.NORTH);

        inner.setBackground(bg);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel createGroupPanel(String name, Color bg, Color accent) {
        JPanel p = new JPanel();
        p.setBackground(bg);
        return p;
    }

    private static JLabel makeValueLabel(String initial) {
        JLabel l = new JLabel(initial);
        l.setFont(new Font("Monospaced", Font.BOLD, 16));
        l.setForeground(new Color(249, 226, 175));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private static JLabel makeRegLabel(String text, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(fg);
        return l;
    }

    private static JLabel makeStageLabel() {
        JLabel l = new JLabel("–");
        l.setFont(new Font("Monospaced", Font.BOLD, 14));
        l.setForeground(new Color(108, 112, 134));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private static JButton makeButton(String text, Color accent) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(new Color(45, 45, 62));
        btn.setForeground(accent);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accent, 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(accent);
                btn.setForeground(new Color(30, 30, 46));
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(45, 45, 62));
                btn.setForeground(accent);
            }
        });
        return btn;
    }

    // ── IPC: Connect to CoreProcess ────────────────────────────────────────
    private void connectToCore() {
        // Run in background thread so we don't freeze the UI
        new Thread(() -> {
            boolean connected = false;
            int attempts = 0;
            while (!connected && attempts < 30) {
                try {
                    coreSocket = new Socket("127.0.0.1", 5000);
                    toCore   = new PrintWriter(coreSocket.getOutputStream(), true);
                    fromCore = new BufferedReader(
                            new InputStreamReader(coreSocket.getInputStream()));
                    connected = true;
                    SwingUtilities.invokeLater(() -> {
                        setStatus("Connected to CoreProcess ✓  —  Load a program and press Step or Run.");
                        setButtonsEnabled(true);
                    });
                    // Start listening for state updates from Core
                    startCoreListener();
                } catch (IOException ex) {
                    attempts++;
                    final int attemptSnapshot = attempts;
                    SwingUtilities.invokeLater(() ->
                        setStatus("Waiting for CoreProcess... (attempt " + attemptSnapshot + ")"));
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
            if (!connected) {
                SwingUtilities.invokeLater(() ->
                    setStatus("ERROR: Could not connect to CoreProcess on port 5000. Is it running?"));
            }
        }, "UI-ConnectThread").start();
    }

    // ── IPC: Listen for incoming STATE| messages from Core ─────────────────
    private void startCoreListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = fromCore.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> handleCoreMessage(msg));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                    setStatus("Connection to CoreProcess lost: " + e.getMessage()));
            }
        }, "UI-ListenerThread").start();
    }

    // ── Parse a message from CoreProcess ──────────────────────────────────
    // Formats we handle:
    //   STATE|PC=0005|A=08|B=00|CY=false|OV=false|SP=07|Q_SIZE=1
    //   HALTED
    //   RESET_DONE
    private void handleCoreMessage(String msg) {
        if (msg.startsWith("STATE|")) {
            parseState(msg);
        } else if (msg.equals("HALTED")) {
            appendTrace("\n──── HALTED ────\n", "halt");
            setStatus("Execution complete — CPU HALTED. Press Reset to run again.");
            btnRun.setEnabled(false);
            btnStep.setEnabled(false);
        } else if (msg.equals("RESET_DONE")) {
            resetDisplayState();
            appendTrace("── Reset ──  Program reloaded, state cleared.\n", "info");
            setStatus("Reset complete. Press Step or Run.");
            btnRun.setEnabled(true);
            btnStep.setEnabled(true);
        }
    }

    // ── Parse STATE| message and update all labels ─────────────────────────
    // STATE|PC=0005|A=08|B=00|CY=false|OV=false|SP=07|Q_SIZE=1
    private void parseState(String msg) {
        try {
            // Remove "STATE|" prefix and split the rest by "|"
            String[] parts = msg.substring(6).split("\\|");
            String pc = "", a = "", b = "", cy = "", ov = "", sp = "", qsz = "";
            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length < 2) continue;
                switch (kv[0].toUpperCase()) {
                    case "PC":     pc  = kv[1]; break;
                    case "A":      a   = kv[1]; break;
                    case "B":      b   = kv[1]; break;
                    case "CY":     cy  = kv[1]; break;
                    case "OV":     ov  = kv[1]; break;
                    case "SP":     sp  = kv[1]; break;
                    case "Q_SIZE": qsz = kv[1]; break;
                }
            }
            // Update register labels
            lblPC.setText(pc.toUpperCase());
            lblA.setText(a.toUpperCase());
            lblB.setText(b.toUpperCase());
            lblCY.setText(cy.equalsIgnoreCase("true") ? "1" : "0");
            lblOV.setText(ov.equalsIgnoreCase("true") ? "1" : "0");
            lblSP.setText(sp.toUpperCase());
            lblQSize.setText(qsz);

            // Animate FETCH → DECODE → EXECUTE indicators
            animateFDE();

            // Append to trace log
            stepCount++;
            String trace = String.format(
                "Step %03d │ PC=%s A=%s B=%s  CY=%s OV=%s  SP=%s Q=%s\n",
                stepCount, pc.toUpperCase(), a.toUpperCase(), b.toUpperCase(),
                cy.equalsIgnoreCase("true") ? "1" : "0",
                ov.equalsIgnoreCase("true") ? "1" : "0",
                sp.toUpperCase(), qsz);
            appendTrace(trace, "state");

            setStatus("Step " + stepCount + " executed  —  PC=" + pc.toUpperCase()
                       + "  A=" + a.toUpperCase());
        } catch (Exception ex) {
            appendTrace("[UI] Parse error: " + msg + "\n", "err");
        }
    }

    // ── Animate FDE indicators: flash ✓ for each stage ────────────────────
    private void animateFDE() {
        Color pending = new Color(108, 112, 134);
        Color done    = new Color(166, 227, 161);

        fetchIndicator.setText("–");   fetchIndicator.setForeground(pending);
        decodeIndicator.setText("–");  decodeIndicator.setForeground(pending);
        executeIndicator.setText("–"); executeIndicator.setForeground(pending);

        // Use a timer to flash each stage in sequence
        Timer t1 = new Timer(80, e -> { fetchIndicator.setText("✓");   fetchIndicator.setForeground(done); });
        Timer t2 = new Timer(180, e -> { decodeIndicator.setText("✓");  decodeIndicator.setForeground(done); });
        Timer t3 = new Timer(280, e -> { executeIndicator.setText("✓"); executeIndicator.setForeground(done); });
        t1.setRepeats(false); t1.start();
        t2.setRepeats(false); t2.start();
        t3.setRepeats(false); t3.start();
    }

    // ── Reset display state when CPU is reset ─────────────────────────────
    private void resetDisplayState() {
        lblPC.setText("0000");
        lblA.setText("00");
        lblB.setText("00");
        lblCY.setText("0");
        lblOV.setText("0");
        lblSP.setText("07");
        lblQSize.setText("0");
        fetchIndicator.setText("–");
        decodeIndicator.setText("–");
        executeIndicator.setText("–");
        stepCount = 0;
    }

    // ── Send commands to CoreProcess ───────────────────────────────────────
    private void sendCommand(String cmd) {
        if (toCore != null) {
            toCore.println(cmd);
        }
    }

    private void sendStep() {
        setStatus("Stepping...");
        sendCommand("STEP");
    }

    private void sendRun() {
        setStatus("Running full program...");
        btnRun.setEnabled(false);
        btnStep.setEnabled(false);
        // RUN blocks Core until HALT, so send it in a background thread
        new Thread(() -> sendCommand("RUN"), "UI-RunThread").start();
    }

    private void quit() {
        sendCommand("QUIT");
        try { if (coreSocket != null) coreSocket.close(); } catch (IOException ignored) {}
        System.exit(0);
    }

    // ── Append styled text to the trace pane ──────────────────────────────
    // style: "state", "info", "halt", "err"
    private void appendTrace(String text, String style) {
        try {
            SimpleAttributeSet attr = new SimpleAttributeSet();
            switch (style) {
                case "state":
                    StyleConstants.setForeground(attr, new Color(205, 214, 244));
                    break;
                case "info":
                    StyleConstants.setForeground(attr, new Color(137, 180, 250));
                    StyleConstants.setItalic(attr, true);
                    break;
                case "halt":
                    StyleConstants.setForeground(attr, new Color(243, 139, 168));
                    StyleConstants.setBold(attr, true);
                    break;
                case "err":
                    StyleConstants.setForeground(attr, new Color(243, 139, 168));
                    break;
                default:
                    StyleConstants.setForeground(attr, new Color(166, 173, 200));
            }
            traceDoc.insertString(traceDoc.getLength(), text, attr);
            // Auto-scroll to bottom
            tracePane.setCaretPosition(traceDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // ── Helper: status bar ─────────────────────────────────────────────────
    private void setStatus(String msg) {
        statusBar.setText("  " + msg);
    }

    // ── Helper: enable/disable all control buttons ─────────────────────────
    private void setButtonsEnabled(boolean enabled) {
        btnLoad.setEnabled(enabled);
        btnStep.setEnabled(enabled);
        btnRun.setEnabled(enabled);
        btnReset.setEnabled(enabled);
    }
}
