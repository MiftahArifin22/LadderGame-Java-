import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameBoard extends JPanel {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 90;
    private static final int PADDING = 30;

    private List<Player> players;
    private List<Ladder> ladders;
    private Ladder highlightLadder;
    private Set<Integer> starNodes; // Node yang ada Bintang (Score)

    // Palette Warna
    private final Color COLOR_BG = new Color(30, 40, 50);
    private final Color COLOR_CELL_A = new Color(245, 245, 245);
    private final Color COLOR_CELL_B = new Color(225, 225, 225);
    private final Color COLOR_PRIME_FILL = new Color(240, 230, 255); // Lavender (Ladder Access)
    private final Color COLOR_BONUS_FILL = new Color(255, 250, 200); // Kuning (Bonus Dice)
    private final Color COLOR_TEXT = new Color(60, 60, 60);

    public GameBoard() {
        players = new ArrayList<>();
        ladders = new ArrayList<>();
        starNodes = new HashSet<>();
        setPreferredSize(new Dimension(
                BOARD_SIZE * CELL_SIZE + PADDING * 2,
                BOARD_SIZE * CELL_SIZE + PADDING * 2
        ));
        setBackground(COLOR_BG);
    }

    public void setPlayers(List<Player> players) { this.players = players; }
    public void setLadders(List<Ladder> ladders) { this.ladders = ladders; repaint(); }
    public void setHighlightLadder(Ladder ladder) { this.highlightLadder = ladder; repaint(); }
    public void setStarNodes(Set<Integer> stars) { this.starNodes = stars; repaint(); }

    // Logic Prime untuk Ladder Access
    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int nodeNumber = 64;
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int x, y;
                y = PADDING + row * CELL_SIZE;

                if (row % 2 == 0) x = PADDING + (BOARD_SIZE - 1 - col) * CELL_SIZE;
                else x = PADDING + col * CELL_SIZE;

                // --- 1. GAMBAR CELL ---
                Shape cellShape = new RoundRectangle2D.Double(x + 4, y + 4, CELL_SIZE - 8, CELL_SIZE - 8, 15, 15);

                Color baseColor;
                if (isPrime(nodeNumber)) baseColor = COLOR_PRIME_FILL; // Prime = Ladder Potential
                else if ((row + col) % 2 == 0) baseColor = COLOR_CELL_A;
                else baseColor = COLOR_CELL_B;

                if (nodeNumber % 5 == 0 && nodeNumber != 64) baseColor = COLOR_BONUS_FILL;

                GradientPaint gp = new GradientPaint(x, y, baseColor, x + CELL_SIZE, y + CELL_SIZE, baseColor.darker());
                g2d.setPaint(gp);
                g2d.fill(cellShape);

                g2d.setColor(new Color(0,0,0,40));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.draw(cellShape);

                // --- 2. KONTEN CELL ---
                if (nodeNumber == 64) drawLabelCenter(g2d, x, y, "FINISH", new Color(231, 76, 60));
                else if (nodeNumber == 1) drawLabelCenter(g2d, x, y, "START", new Color(46, 204, 113));
                else {
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
                    g2d.setColor(COLOR_TEXT);
                    g2d.drawString(String.valueOf(nodeNumber), x + 15, y + 35);

                    // GAMBAR BINTANG HANYA JIKA ADA DI SET SCORE NODES
                    if (starNodes.contains(nodeNumber)) {
                        drawShinyStar(g2d, x + CELL_SIZE - 30, y + 25, 12, 24);
                    }

                    if (nodeNumber % 5 == 0) {
                        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
                        g2d.drawString("🎲", x + CELL_SIZE - 38, y + CELL_SIZE - 15);
                    }
                }
                nodeNumber--;
            }
        }

        for (Ladder ladder : ladders) drawRealisticLadder(g2d, ladder);
        for (int i = 0; i < players.size(); i++) drawPlayerToken(g2d, players.get(i), i);
    }

    private void drawShinyStar(Graphics2D g2d, double cx, double cy, double innerRadius, double outerRadius) {
        Shape star = createStarShape(cx, cy, innerRadius, outerRadius, 5, Math.toRadians(-18));

        Point2D center = new Point2D.Double(cx, cy);
        float[] dist = {0.0f, 0.5f, 1.0f};
        Color[] colors = {new Color(255, 255, 220), new Color(255, 215, 0), new Color(218, 165, 32)};

        RadialGradientPaint rgp = new RadialGradientPaint(center, (float)outerRadius, dist, colors);

        g2d.setPaint(rgp);
        g2d.fill(star);
        g2d.setColor(new Color(184, 134, 11));
        g2d.setStroke(new BasicStroke(1f));
        g2d.draw(star);
    }

    private Shape createStarShape(double centerX, double centerY, double innerRadius, double outerRadius, int numRays, double startAngleRad) {
        Path2D path = new Path2D.Double();
        double deltaAngleRad = Math.PI / numRays;
        for (int i = 0; i < numRays * 2; i++) {
            double angleRad = startAngleRad + i * deltaAngleRad;
            double ca = Math.cos(angleRad);
            double sa = Math.sin(angleRad);
            double relX = ca * (i % 2 == 0 ? outerRadius : innerRadius);
            double relY = sa * (i % 2 == 0 ? outerRadius : innerRadius);
            if (i == 0) path.moveTo(centerX + relX, centerY + relY);
            else path.lineTo(centerX + relX, centerY + relY);
        }
        path.closePath();
        return path;
    }

    private void drawLabelCenter(Graphics2D g2d, int x, int y, String text, Color color) {
        g2d.setColor(color);
        g2d.fillRoundRect(x+8, y+28, CELL_SIZE-16, 34, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 15));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = x + (CELL_SIZE - fm.stringWidth(text)) / 2;
        g2d.drawString(text, tx, y + 50);
    }

    private void drawRealisticLadder(Graphics2D g2d, Ladder ladder) {
        Point p1 = getCoordinatesForPosition(ladder.getFrom());
        Point p2 = getCoordinatesForPosition(ladder.getTo());
        if (p1 == null || p2 == null) return;
        int x1 = p1.x + CELL_SIZE/2; int y1 = p1.y + CELL_SIZE/2;
        int x2 = p2.x + CELL_SIZE/2; int y2 = p2.y + CELL_SIZE/2;
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int width = 15;
        int dx = (int) (width * Math.sin(angle));
        int dy = (int) (width * Math.cos(angle));

        if (highlightLadder == ladder) {
            g2d.setColor(new Color(46, 204, 113, 150));
            g2d.setStroke(new BasicStroke(35, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(x1, y1, x2, y2);
        }
        g2d.setColor(new Color(0,0,0,60));
        g2d.setStroke(new BasicStroke(9));
        g2d.drawLine(x1 - dx + 6, y1 + dy + 6, x2 - dx + 6, y2 + dy + 6);
        g2d.drawLine(x1 + dx + 6, y1 - dy + 6, x2 + dx + 6, y2 - dy + 6);
        g2d.setColor(new Color(101, 67, 33));
        g2d.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1 - dx, y1 + dy, x2 - dx, y2 + dy);
        g2d.drawLine(x1 + dx, y1 - dy, x2 + dx, y2 - dy);
        g2d.setColor(new Color(160, 82, 45));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(x1 - dx, y1 + dy, x2 - dx, y2 + dy);
        g2d.drawLine(x1 + dx, y1 - dy, x2 + dx, y2 - dy);
        int steps = 10;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int cx = (int) (x1 + t * (x2 - x1)); int cy = (int) (y1 + t * (y2 - y1));
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(7));
            g2d.drawLine(cx - dx, cy + dy, cx + dx, cy - dy);
            g2d.setColor(new Color(205, 133, 63));
            g2d.fillOval(cx - dx - 3, cy + dy - 3, 6, 6);
            g2d.fillOval(cx + dx - 3, cy - dy - 3, 6, 6);
        }
    }

    private void drawPlayerToken(Graphics2D g2d, Player p, int index) {
        Point coords = getCoordinatesForPosition(p.getPosition());
        if (coords == null) return;
        int offset = (index * 6) - 10;
        int px = coords.x + CELL_SIZE/2 - 18 + offset;
        int py = coords.y + CELL_SIZE/2 - 22 + offset;
        int size = 36;
        g2d.setColor(new Color(0,0,0,80));
        g2d.fillOval(px + 3, py + 3, size, size);
        GradientPaint gp = new GradientPaint(px, py, p.getColor().brighter(), px + size, py + size, p.getColor().darker());
        g2d.setPaint(gp);
        g2d.fillOval(px, py, size, size);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(px, py, size, size);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String initial = p.getName().substring(0, 1).toUpperCase();
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(initial, px + (size - fm.stringWidth(initial))/2, py + (size + fm.getAscent())/2 - 3);
    }

    private Point getCoordinatesForPosition(int position) {
        if (position < 1 || position > 64) return null;
        int nodeNumber = 64 - position + 1;
        int row = (nodeNumber - 1) / BOARD_SIZE;
        int col = (nodeNumber - 1) % BOARD_SIZE;
        int x, y;
        y = PADDING + row * CELL_SIZE;
        if (row % 2 == 0) x = PADDING + (BOARD_SIZE - 1 - col) * CELL_SIZE;
        else x = PADDING + col * CELL_SIZE;
        return new Point(x, y);
    }
}