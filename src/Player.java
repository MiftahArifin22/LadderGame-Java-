import java.awt.Color;

public class Player implements Comparable<Player> {
    private String name;
    private int position;
    private Color color;
    private int currentSessionScore; // Score game saat ini (bisa reset)
    private PlayerRecord record;     // Data abadi (Wins & Total Score)

    public Player(String name, Color color, PlayerRecord record) {
        this.name = name;
        this.position = 1;
        this.color = color;
        this.record = record;
        this.currentSessionScore = 0;
    }

    public void addScore(int points) {
        this.currentSessionScore += points;
        this.record.addScore(points); // Tambah ke total akumulasi database
    }

    public void addWin() {
        this.record.addWin();
    }

    // Method untuk reset score sesi ini saja (visual game), tapi Total Record aman
    public void setCurrentScore(int score) {
        this.currentSessionScore = score;
    }

    public String getName() { return name; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public Color getColor() { return color; }

    // --- UPDATE: Mengambil Score Akumulasi (Semua Game) ---
    public int getTotalAccumulatedScore() {
        return record.getTotalScore();
    }

    public int getCurrentSessionScore() {
        return currentSessionScore;
    }

    public int getTotalWins() { return record.getTotalWins(); }

    // --- UPDATE LOGIKA RANKING (PENTING) ---
    @Override
    public int compareTo(Player other) {
        // 1. PRIORITAS UTAMA: Siapa yang paling sering menang (Total Wins)
        int winCompare = Integer.compare(other.getTotalWins(), this.getTotalWins());
        if (winCompare != 0) return winCompare;

        // 2. PRIORITAS KEDUA: Siapa yang punya Total Score Akumulasi terbanyak
        int scoreCompare = Integer.compare(other.getTotalAccumulatedScore(), this.getTotalAccumulatedScore());
        if (scoreCompare != 0) return scoreCompare;

        // 3. PRIORITAS KETIGA: Siapa yang posisinya paling depan di game saat ini
        return Integer.compare(other.position, this.position);
    }
}