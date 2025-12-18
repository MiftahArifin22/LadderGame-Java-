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
    // Pastikan nama file ini SAMA PERSIS dengan file yang ada di folder project kamu
    private static final String SOUND_PATH = "Ular Tangga - Modern Stage - Mohamad Rifandi Lihawa - SoundLoadMate.com.wav";

    // --- DATA STORE (DATABASE SEMENTARA) ---
    // Map ini menjaga data kemenangan tetap ada meskipun game di-reset
    private static Map<String, PlayerRecord> globalRecords = new HashMap<>();

    private GameBoard gameBoard;
    private JButton playButton;
    private JButton rollDiceButton;
    private JLabel diceResultLabel;
    private JLabel diceStatusLabel;
    private JPanel scoreboardPanel;
    private JLabel currentPlayerLabel;

    private List<Player> players;
    private Queue<Player> playerQueue;
    private PriorityQueue<Player> leaderboardQueue;
    private Player currentPlayer;

    private boolean gameStarted = false;
    private Random random;
    private boolean isAnimating = false;
    private List<Ladder> ladders;
    private Set<Integer> scoreNodes;

    private Clip audioClip;

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
    }

    private void playMusic(String filePath) {
        try {
            File musicPath = new File(filePath);
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                audioClip = AudioSystem.getClip();
                audioClip.open(audioInput);
                FloatControl gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f);
                audioClip.start();
                audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                System.err.println("File Audio tidak ditemukan di root folder project.");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        setTitle("🎲 LADDER GAMES: Score Edition");
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

        playMusic(SOUND_PATH);
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

        diceResultLabel = new JLabel("?");
        diceResultLabel.setFont(new Font("Segoe UI", Font.BOLD, 80));
        diceResultLabel.setForeground(Color.WHITE);
        diceResultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceStatusLabel = new JLabel(" ");
        diceStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        diceStatusLabel.setForeground(Color.WHITE);
        diceStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceBox.add(diceResultLabel);
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

        // Refresh Queue agar urutan ranking terupdate sesuai logika compareTo baru
        leaderboardQueue.clear();
        leaderboardQueue.addAll(players);

        List<Player> tempSorted = new ArrayList<>();
        while(!leaderboardQueue.isEmpty()) {
            tempSorted.add(leaderboardQueue.poll());
        }
        // Kembalikan ke queue (opsional jika queue dipakai logika lain)
        leaderboardQueue.addAll(tempSorted);

        int rank = 1;
        for (Player p : tempSorted) {
            JPanel row = new JPanel(new GridLayout(1, 2));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(340, 60)); // Sedikit diperbesar
            row.setBorder(new EmptyBorder(5,5,5,5));

            // --- KIRI: Rank, Nama, Wins ---
            JPanel leftInfo = new JPanel(new GridLayout(3, 1)); // Jadi 3 baris
            leftInfo.setOpaque(false);

            JLabel nameLbl = new JLabel("#" + rank + " " + p.getName());
            nameLbl.setForeground(Color.WHITE);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));

            // Tampilkan TOTAL WINS dengan jelas
            JLabel winsLbl = new JLabel("🏆 Wins: " + p.getTotalWins());
            winsLbl.setForeground(Color.YELLOW); // Warna kuning biar menonjol
            winsLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));

            JLabel posLbl = new JLabel("Node: " + p.getPosition());
            posLbl.setForeground(Color.GRAY);
            posLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            leftInfo.add(nameLbl);
            leftInfo.add(winsLbl);
            leftInfo.add(posLbl);

            // --- KANAN: Total Score & Session Score ---
            JPanel rightInfo = new JPanel(new GridLayout(2, 1));
            rightInfo.setOpaque(false);

            // Tampilkan TOTAL SCORE (Akumulasi Sejarah)
            JLabel totalScoreLbl = new JLabel("Total Score: " + p.getTotalAccumulatedScore());
            totalScoreLbl.setForeground(COLOR_ACCENT_ORANGE);
            totalScoreLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            totalScoreLbl.setHorizontalAlignment(SwingConstants.RIGHT);

            // Tampilkan Score Sesi Ini (Kecil saja)
            JLabel sessionScoreLbl = new JLabel("(This Game: " + p.getCurrentSessionScore() + ")");
            sessionScoreLbl.setForeground(new Color(150, 150, 150));
            sessionScoreLbl.setFont(new Font("Segoe UI", Font.ITALIC, 10));
            sessionScoreLbl.setHorizontalAlignment(SwingConstants.RIGHT);

            rightInfo.add(totalScoreLbl);
            rightInfo.add(sessionScoreLbl);

            row.add(leftInfo);
            row.add(rightInfo);

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

                // CEK APAKAH PEMAIN INI SUDAH PERNAH MAIN SEBELUMNYA
                if (!globalRecords.containsKey(name)) {
                    globalRecords.put(name, new PlayerRecord());
                }
                PlayerRecord record = globalRecords.get(name); // AMBIL REKOR LAMA (TOTAL WINS)

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

            diceResultLabel.setText("?");
            diceStatusLabel.setText("");

            gameBoard.setPlayers(players);
            gameBoard.setLadders(ladders);
            updateScoreboard();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number!");
        }
    }

    // --- [BARU] METHOD UNTUK RESET GAME TAPI PERTAHANKAN ORANGNYA ---
    private void resetGameSamePlayers() {
        for (Player p : players) {
            p.setPosition(1);       // Reset posisi ke 1
            p.setCurrentScore(0);   // Reset score sesi ke 0 (Total Wins tetap aman di Record)
        }

        // Generate ulang papan agar tidak bosan
        generateLadders();
        generateScoreNodes();

        // Reset antrian
        playerQueue.clear();
        playerQueue.addAll(players);

        currentPlayer = playerQueue.poll();
        currentPlayerLabel.setText(currentPlayer.getName());
        currentPlayerLabel.setForeground(currentPlayer.getColor());

        diceResultLabel.setText("?");
        diceStatusLabel.setText("New Round!");

        gameBoard.setLadders(ladders);
        gameBoard.setStarNodes(scoreNodes);
        gameBoard.repaint();
        updateScoreboard();

        gameStarted = true;
        playButton.setEnabled(false);
        rollDiceButton.setEnabled(true);
    }

    private void rollDice() {
        if (!gameStarted || isAnimating) return;
        rollDiceButton.setEnabled(false);
        isAnimating = true;

        int diceVal = random.nextInt(6) + 1;
        boolean isBackward = random.nextDouble() < 0.20;

        diceResultLabel.setText(String.valueOf(diceVal));
        if (isBackward) {
            diceResultLabel.setForeground(COLOR_RED);
            diceStatusLabel.setText("BACKWARD! 🔻");
            diceStatusLabel.setForeground(COLOR_RED);
        } else {
            diceResultLabel.setForeground(COLOR_GREEN);
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

    // --- LOGIKA GAME SELESAI & PLAY AGAIN ---
    private void finishTurn() {
        int pos = currentPlayer.getPosition();

        if (pos == 64) {
            // 1. Tambah Win
            currentPlayer.addWin();

            // 2. Penting: Pastikan Map Global terupdate
            // (Sebenarnya sudah otomatis karena Player merujuk ke object PlayerRecord yang sama,
            // tapi ini untuk memastikan konsistensi UI)

            updateScoreboard(); // Update UI Leaderboard langsung saat menang

            // 3. Mainkan Musik Menang (Opsional)
            // playMusic("win.wav");

            int response = JOptionPane.showConfirmDialog(this,
                    "🎉 CONGRATULATIONS! 🎉\n" +
                            currentPlayer.getName() + " WINS THE ROUND!\n\n" +
                            "🏆 Total Wins: " + currentPlayer.getTotalWins() + "\n" +
                            "⭐ Total Accumulated Score: " + currentPlayer.getTotalAccumulatedScore() + "\n\n" +
                            "Do you want to play again with the same players?",
                    "Round Over",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

            if (response == JOptionPane.YES_OPTION) {
                resetGameSamePlayers();
            } else {
                gameStarted = false;
                playButton.setEnabled(true);
                rollDiceButton.setEnabled(false);
                diceResultLabel.setText("?");
                diceStatusLabel.setText("Finished");
            }

            isAnimating = false;
            return;
        }

        // ... (Kode sisa Bonus Roll dan ganti giliran tidak berubah)
        if (pos % 5 == 0 && pos != 64) {
            // ...
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