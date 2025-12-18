import java.awt.Color;

// Implements Comparable agar bisa diurutkan di Leaderboard (PriorityQueue)
public class Player implements Comparable<Player> {
    private String name;
    private int position;
    private Color color;

    // Tambahan untuk fitur Score & Record
    private int currentSessionScore;
    private PlayerRecord record;

    public Player(String name, Color color, PlayerRecord record) {
        this.name = name;
        this.position = 1;
        this.color = color;
        this.record = record;
        this.currentSessionScore = 0;
    }

    // Method untuk menambah skor (dipanggil saat kena bintang)
    public void addScore(int points) {
        this.currentSessionScore += points;
        this.record.addScore(points); // Simpan ke riwayat abadi juga
    }

    // Method untuk menang
    public void addWin() {
        this.record.addWin();
    }

    // --- GETTERS & SETTERS ---
    public String getName() { return name; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public Color getColor() { return color; }

    // Method yang tadi Error (Sekarang sudah ada)
    public int getCurrentScore() { return currentSessionScore; }

    // Ambil total kemenangan dari record
    public int getTotalWins() { return record.getTotalWins(); }

    // Logika Ranking untuk Leaderboard:
    // 1. Posisi tertinggi (paling dekat finish)
    // 2. Jika posisi sama, lihat Score tertinggi
    @Override
    public int compareTo(Player other) {
        if (this.position != other.position) {
            return Integer.compare(other.position, this.position); // Descending Position
        } else {
            return Integer.compare(other.currentSessionScore, this.currentSessionScore); // Descending Score
        }
    }
}