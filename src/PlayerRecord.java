public class PlayerRecord {
    private int totalAccumulatedScore;
    private int totalWins;

    public PlayerRecord() {
        this.totalAccumulatedScore = 0;
        this.totalWins = 0;
    }

    public void addScore(int score) {
        this.totalAccumulatedScore += score;
    }

    public void addWin() {
        this.totalWins++;
    }

    public int getTotalScore() { return totalAccumulatedScore; }
    public int getTotalWins() { return totalWins; }
}