import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class Main extends JFrame {

    // --- CONFIG AUDIO ---
    // 1. Musik Latar (Looping)
    private static final String BGM_PATH = "Ular Tangga - Modern Stage - Mohamad Rifandi Lihawa - SoundLoadMate.com.wav";

    // 2. Efek Suara LANGKAH (Pendek: 'click' / 'tap')
    private static final String STEP_SFX_PATH = "Jalan.wav";

    // 3. Efek Suara ROLL DICE
    private static final String ROLL_SFX_PATH = "roll-dice.wav";

    // --- DATA STORE ---
    private static Map<String, PlayerRecord> globalRecords = new HashMap<>();

    private GameBoard gameBoard;
    private JButton playButton;
    private JButton rollDiceButton;

    // Visual Components
    private DicePanel diceVisualPanel;
    private JLabel diceStatusLabel;
    private JPanel scoreboardPanel;
    private JLabel currentPlayerLabel;

    // Game Logic Data
    private List<Player> players;
    private Queue<Player> playerQueue;
    private PriorityQueue<Player> leaderboardQueue;
    private Player currentPlayer;
    private boolean gameStarted = false;
    private Random random;
    private boolean isAnimating = false;
    private List<Ladder> ladders;
    private Set<Integer> scoreNodes;

    // Audio Clips
    private Clip backgroundMusic;

    // Warna UI
    private final Color COLOR_BG_DARK = new Color(30, 40, 50);
    private final Color COLOR_PANEL_BG = new Color(44, 62, 80);
    private final Color COLOR_ACCENT_ORANGE = new Color(230, 126, 34);
    private final Color COLOR_ACCENT_BLUE = new Color(52, 152, 219);
    private final Color COLOR_GREEN = new Color(46, 204, 113);
    private final Color COLOR_RED = new Color(231, 76, 60);

    public Main() {
        random = new Random();
        players = new ArrayList<>();
        playerQueue = new LinkedList<>();
        ladders = new ArrayList<>();
        scoreNodes = new HashSet<>();
        leaderboardQueue = new PriorityQueue<>();

        initializeUI();

        // Mulai Musik Background
        playBackgroundMusic();
    }

    // --- AUDIO METHODS ---

    private void playBackgroundMusic() {
        try {
            File soundFile = new File(BGM_PATH);
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioIn);
                FloatControl gainControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f);
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundMusic.start();
            }
        } catch (Exception e) {
            System.out.println("Error playing BGM: " + e.getMessage());
        }
    }

    // Suara Langkah (Pendek)
    private void playStepSound() {
        try {
            File soundFile = new File(STEP_SFX_PATH);
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            }
        } catch (Exception e) {}
    }

    // Suara Roll Dadu (4 Detik)
    private void playRollSound() {
        try {
            File soundFile = new File(ROLL_SFX_PATH);
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } else {
                System.out.println("Roll SFX file not found at: " + ROLL_SFX_PATH);
            }
        } catch (Exception e) {
            System.out.println("Error playing Roll SFX: " + e.getMessage());
        }
    }

    private void generateLadders() {
        ladders.clear();
        Set<Integer> usedPositions = new HashSet<>();
        while (ladders.size() < 5) {
            int startNode = random.nextInt(49) + 2;
            if (usedPositions.contains(startNode)) continue;
            int jump = random.nextInt(16) + 10;
            int endNode = startNode + jump;
            if (endNode > 63) continue;
            if (usedPositions.contains(endNode)) continue;
            ladders.add(new Ladder(startNode, endNode));
            usedPositions.add(startNode);
            usedPositions.add(endNode);
        }
    }

    private void generateScoreNodes() {
        scoreNodes.clear();
        while (scoreNodes.size() < 10) {
            int node = random.nextInt(62) + 2;
            scoreNodes.add(node);
        }
        gameBoard.setStarNodes(scoreNodes);
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    private void initializeUI() {
        setTitle("🎲 LADDER GAMES: Ultimate Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(COLOR_BG_DARK);

        gameBoard = new GameBoard();

        add(createTopPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(gameBoard);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(COLOR_BG_DARK);
        add(scrollPane, BorderLayout.CENTER);

        add(createRightPanel(), BorderLayout.EAST);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(39, 174, 96), getWidth(), 0, new Color(46, 204, 113));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel title = new JLabel("🪜 LADDER GAMES 🎲");
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        panel.add(title);
        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_PANEL_BG);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        panel.setPreferredSize(new Dimension(380, 0));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255,255,255,20));
        card.setBorder(new LineBorder(new Color(255,255,255,50), 1, true));
        card.setMaximumSize(new Dimension(340, 140));

        JLabel lblTurn = new JLabel("CURRENT TURN");
        lblTurn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTurn.setForeground(new Color(200, 200, 200));
        lblTurn.setAlignmentX(Component.CENTER_ALIGNMENT);

        currentPlayerLabel = new JLabel("Waiting...");
        currentPlayerLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        currentPlayerLabel.setForeground(Color.WHITE);
        currentPlayerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(20));
        card.add(lblTurn);
        card.add(Box.createVerticalStrut(10));
        card.add(currentPlayerLabel);
        card.add(Box.createVerticalStrut(20));

        playButton = createStyledButton("START NEW GAME", COLOR_ACCENT_BLUE);
        playButton.addActionListener(e -> startGame());

        rollDiceButton = createStyledButton("ROLL DICE 🎲", COLOR_ACCENT_ORANGE);
        rollDiceButton.setEnabled(false);
        rollDiceButton.addActionListener(e -> rollDice());

        JPanel diceBox = new JPanel();
        diceBox.setLayout(new BoxLayout(diceBox, BoxLayout.Y_AXIS));
        diceBox.setOpaque(false);

        diceVisualPanel = new DicePanel();
        diceVisualPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceStatusLabel = new JLabel(" ");
        diceStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        diceStatusLabel.setForeground(Color.WHITE);
        diceStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceBox.add(diceVisualPanel);
        diceBox.add(Box.createVerticalStrut(10));
        diceBox.add(diceStatusLabel);

        scoreboardPanel = new JPanel();
        scoreboardPanel.setLayout(new BoxLayout(scoreboardPanel, BoxLayout.Y_AXIS));
        scoreboardPanel.setBackground(COLOR_PANEL_BG);

        JLabel sbTitle = new JLabel("LIVE LEADERBOARD");
        sbTitle.setForeground(new Color(236, 240, 241));
        sbTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sbTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(card);
        panel.add(Box.createVerticalStrut(30));
        panel.add(playButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(rollDiceButton);
        panel.add(Box.createVerticalStrut(30));
        panel.add(diceBox);
        panel.add(Box.createVerticalStrut(30));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL));
        panel.add(Box.createVerticalStrut(15));
        panel.add(sbTitle);
        panel.add(Box.createVerticalStrut(15));
        panel.add(scoreboardPanel);

        return panel;
    }

    private class DicePanel extends JPanel {
        private int value = 1;

        public DicePanel() {
            setPreferredSize(new Dimension(100, 100));
            setMaximumSize(new Dimension(100, 100));
            setOpaque(false);
        }

        public void setValue(int value) {
            this.value = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(5, 5, 90, 90, 20, 20);
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(5, 5, 90, 90, 20, 20);

            g2d.setColor(Color.BLACK);
            int dotSize = 18;
            int center = 50 - dotSize/2;
            int left = 25 - dotSize/2;
            int right = 75 - dotSize/2;
            int top = 25 - dotSize/2;
            int bottom = 75 - dotSize/2;

            if (value == 1 || value == 3 || value == 5) {
                g2d.fillOval(center, center, dotSize, dotSize);
            }
            if (value >= 2) {
                g2d.fillOval(left, top, dotSize, dotSize);
                g2d.fillOval(right, bottom, dotSize, dotSize);
            }
            if (value >= 4) {
                g2d.fillOval(right, top, dotSize, dotSize);
                g2d.fillOval(left, bottom, dotSize, dotSize);
            }
            if (value == 6) {
                g2d.fillOval(left, center, dotSize, dotSize);
                g2d.fillOval(right, center, dotSize, dotSize);
            }
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isArmed()) g.setColor(bg.darker());
                else g.setColor(bg);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setMaximumSize(new Dimension(340, 55));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void updateScoreboard() {
        scoreboardPanel.removeAll();
        leaderboardQueue.clear();
        leaderboardQueue.addAll(players);

        List<Player> tempSorted = new ArrayList<>();
        while(!leaderboardQueue.isEmpty()) tempSorted.add(leaderboardQueue.poll());
        leaderboardQueue.addAll(tempSorted);

        int rank = 1;
        for (Player p : tempSorted) {
            JPanel row = new JPanel(new GridLayout(1, 2));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(340, 50));
            row.setBorder(new EmptyBorder(5,5,5,5));

            JPanel leftInfo = new JPanel(new GridLayout(2, 1));
            leftInfo.setOpaque(false);
            JLabel nameLbl = new JLabel("#" + rank + " " + p.getName());
            nameLbl.setForeground(Color.WHITE);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            JLabel winsLbl = new JLabel("Total Wins: " + p.getTotalWins());
            winsLbl.setForeground(Color.GRAY);
            winsLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            leftInfo.add(nameLbl); leftInfo.add(winsLbl);

            JPanel rightInfo = new JPanel(new GridLayout(2, 1));
            rightInfo.setOpaque(false);
            JLabel nodeLbl = new JLabel("Node: " + p.getPosition());
            nodeLbl.setForeground(COLOR_GREEN);
            nodeLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nodeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
            JLabel scoreLbl = new JLabel("Score: " + p.getCurrentScore());
            scoreLbl.setForeground(COLOR_ACCENT_ORANGE);
            scoreLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            scoreLbl.setHorizontalAlignment(SwingConstants.RIGHT);
            rightInfo.add(nodeLbl); rightInfo.add(scoreLbl);

            row.add(leftInfo); row.add(rightInfo);
            scoreboardPanel.add(row);
            scoreboardPanel.add(new JSeparator());
            rank++;
        }
        scoreboardPanel.revalidate();
        scoreboardPanel.repaint();
    }

    private void startGame() {
        String input = JOptionPane.showInputDialog(this, "How many players? (2-4):");
        if (input == null) return;
        try {
            int n = Integer.parseInt(input);
            if (n < 2 || n > 4) {
                JOptionPane.showMessageDialog(this, "Please enter 2-4 players!");
                return;
            }

            players.clear();
            Color[] colors = {new Color(231, 76, 60), new Color(52, 152, 219), new Color(241, 196, 15), new Color(155, 89, 182)};

            for(int i=0; i<n; i++) {
                String name = JOptionPane.showInputDialog(this, "Enter name for Player " + (i+1) + ":");
                if (name == null || name.trim().isEmpty()) name = "Player " + (i+1);
                else name = name.trim();

                if (!globalRecords.containsKey(name)) globalRecords.put(name, new PlayerRecord());
                PlayerRecord record = globalRecords.get(name);
                players.add(new Player(name, colors[i], record));
            }

            generateLadders();
            generateScoreNodes();

            playerQueue.clear();
            playerQueue.addAll(players);

            gameStarted = true;
            playButton.setEnabled(false);
            rollDiceButton.setEnabled(true);

            currentPlayer = playerQueue.poll();
            currentPlayerLabel.setText(currentPlayer.getName());
            currentPlayerLabel.setForeground(currentPlayer.getColor());

            diceVisualPanel.setValue(1);
            diceStatusLabel.setText("");

            gameBoard.setPlayers(players);
            gameBoard.setLadders(ladders);
            updateScoreboard();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number!");
        }
    }

    // --- LOGIKA UTAMA: ROLL DICE 4 DETIK & AUDIO ---
    private void rollDice() {
        if (!gameStarted || isAnimating) return;
        rollDiceButton.setEnabled(false);
        isAnimating = true;

        // 1. Mainkan suara dadu (durasi 4 detik)
        playRollSound();

        diceStatusLabel.setText("Rolling...");
        diceStatusLabel.setForeground(Color.WHITE);

        // 2. Timer Animasi Dadu Berputar
        // Interval 80ms (sangat cepat mengacak visual dadu)
        Timer diceAnimTimer = new Timer(80, null);
        long startTime = System.currentTimeMillis();

        diceAnimTimer.addActionListener(e -> {
            // Tampilkan angka acak selama animasi
            diceVisualPanel.setValue(random.nextInt(6) + 1);

            // 3. Stop setelah 4 Detik (4000 ms) sesuai durasi lagu
            if (System.currentTimeMillis() - startTime > 4000) {
                diceAnimTimer.stop();
                finalizeDiceRoll(); // Lanjut ke logika
            }
        });
        diceAnimTimer.start();
    }

    // Method dipanggil setelah 4 detik (setelah lagu selesai)
    private void finalizeDiceRoll() {
        int diceVal = random.nextInt(6) + 1;
        boolean isBackward = random.nextDouble() < 0.20;

        // Tampilkan hasil akhir
        diceVisualPanel.setValue(diceVal);

        if (isBackward) {
            diceStatusLabel.setText("BACKWARD! 🔻");
            diceStatusLabel.setForeground(COLOR_RED);
        } else {
            diceStatusLabel.setText("FORWARD! 🔼");
            diceStatusLabel.setForeground(COLOR_GREEN);
        }

        boolean hasPrimePower = isPrime(currentPlayer.getPosition());
        List<Integer> path = calculatePath(currentPlayer.getPosition(), diceVal, hasPrimePower, isBackward);
        animatePath(path);
    }

    private List<Integer> calculatePath(int startPos, int steps, boolean canUseLadder, boolean isBackward) {
        List<Integer> path = new ArrayList<>();
        int currentPos = startPos;
        int stepsRemaining = steps;

        while (stepsRemaining > 0) {
            if (isBackward) {
                currentPos--;
                if (currentPos < 1) { currentPos = 1; break; }
            } else {
                currentPos++;
                if (currentPos > 64) { currentPos = 64; break; }
            }

            path.add(currentPos);
            stepsRemaining--;

            if (!isBackward) {
                Ladder ladder = getLadderAt(currentPos);
                if (ladder != null && canUseLadder) {
                    currentPos = ladder.getTo();
                    path.add(currentPos);
                }
            }
        }
        return path;
    }

    private Ladder getLadderAt(int pos) {
        for (Ladder l : ladders) if (l.getFrom() == pos) return l;
        return null;
    }

    private void animatePath(List<Integer> path) {
        final int[] index = {0};
        Timer timer = new Timer(300, null);

        timer.addActionListener(e -> {
            if (index[0] < path.size()) {
                int nextNode = path.get(index[0]);
                int prevNode = currentPlayer.getPosition();

                // --- MAINKAN SUARA LANGKAH DI SETIAP PERPINDAHAN ---
                playStepSound();

                if (Math.abs(nextNode - prevNode) > 1) {
                    Ladder l = getLadderAt(prevNode);
                    if (l != null) gameBoard.setHighlightLadder(l);
                } else {
                    gameBoard.setHighlightLadder(null);
                }

                currentPlayer.setPosition(nextNode);

                if (scoreNodes.contains(nextNode) && nextNode != prevNode) {
                    int points = (random.nextInt(5) + 1) * 10;
                    currentPlayer.addScore(points);
                }

                gameBoard.repaint();
                updateScoreboard();
                index[0]++;
            } else {
                timer.stop();
                gameBoard.setHighlightLadder(null);
                finishTurn();
            }
        });
        timer.start();
    }

    private void finishTurn() {
        int pos = currentPlayer.getPosition();

        if (pos == 64) {
            currentPlayer.addWin();
            JOptionPane.showMessageDialog(this, "🎉 " + currentPlayer.getName() + " WINS! 🎉\nFinal Score: " + currentPlayer.getCurrentScore());
            gameStarted = false;
            playButton.setEnabled(true);
            isAnimating = false;
            updateScoreboard();
            return;
        }

        if (pos % 5 == 0 && pos != 64) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "🎲 BONUS ROLL! You landed on " + pos + ".\nRoll again?",
                    "Bonus", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                isAnimating = false;
                rollDiceButton.setEnabled(true);
                return;
            }
        }

        playerQueue.add(currentPlayer);
        currentPlayer = playerQueue.poll();
        currentPlayerLabel.setText(currentPlayer.getName());
        currentPlayerLabel.setForeground(currentPlayer.getColor());
        diceStatusLabel.setText("");

        isAnimating = false;
        rollDiceButton.setEnabled(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}