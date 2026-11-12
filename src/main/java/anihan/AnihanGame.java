package anihan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Anihan - a small, dependency-free 2D farming prototype.
 * Java 17+, Swing, AWT and Graphics2D only.
 */
public class AnihanGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Anihan - Harvest");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new GamePanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    enum CropType {
        PALAY("Palay", "Rice", new Color(224, 190, 78), 2, 8),
        UBE("Ube", "Purple yam", new Color(160, 105, 205), 3, 14),
        KAMOTE("Kamote", "Sweet potato", new Color(218, 125, 65), 2, 11),
        PINEAPPLE("Pineapple", "Pineapple", new Color(235, 181, 54), 4, 24);

        final String label, description;
        final Color color;
        final int daysToGrow, sellPrice;

        CropType(String label, String description, Color color, int daysToGrow, int sellPrice) {
            this.label = label;
            this.description = description;
            this.color = color;
            this.daysToGrow = daysToGrow;
            this.sellPrice = sellPrice;
        }
    }

    enum TileType { GRASS, PATH, WATER, TREE, HOUSE, MARKET, SOIL }

    static class Crop {
        final CropType type;
        int age;
        boolean wateredToday;

        Crop(CropType type) { this.type = type; }
        boolean mature() { return age >= type.daysToGrow; }
        void newDay() {
            if (wateredToday && !mature()) age++;
            wateredToday = false;
        }
    }

    static class FarmTile {
        TileType terrain = TileType.GRASS;
        boolean tilled;
        Crop crop;
    }

    static class GamePanel extends JPanel implements ActionListener, KeyListener {
        private static final int TILE = 48;
        private static final int COLS = 18;
        private static final int ROWS = 12;
        private static final int WIDTH = COLS * TILE;
        private static final int HEIGHT = ROWS * TILE + 112;

        private final FarmTile[][] world = new FarmTile[ROWS][COLS];
        private final Timer timer = new Timer(16, this);
        private final EnumMap<CropType, Integer> seeds = new EnumMap<>(CropType.class);
        private final EnumMap<CropType, Integer> harvest = new EnumMap<>(CropType.class);
        private int playerX = 5, playerY = 7;
        private int selectedSeed = 0;
        private CropType[] cropChoices = CropType.values();
        private int day = 1;
        private int coins = 80;
        private long lastDayTick = System.currentTimeMillis();
        private boolean inventoryOpen;
        private String message = "Welcome home, magsasaka! Your farm is waiting.";
        private long messageUntil = System.currentTimeMillis() + 5000;
        private int walkFrame;
        private boolean moving;
        private long lastMove = 0;

        GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            addKeyListener(this);
            for (CropType type : CropType.values()) {
                seeds.put(type, 5);
                harvest.put(type, 0);
            }
            buildWorld();
            timer.start();
        }

        private void buildWorld() {
            for (int y = 0; y < ROWS; y++) {
                for (int x = 0; x < COLS; x++) world[y][x] = new FarmTile();
            }
            // A winding dirt path through the farm.
            for (int x = 0; x < COLS; x++) world[5][x].terrain = TileType.PATH;
            for (int y = 5; y < 10; y++) world[y][8].terrain = TileType.PATH;
            for (int x = 8; x < 13; x++) world[9][x].terrain = TileType.PATH;

            // House, market, pond and tree line.
            world[2][3].terrain = TileType.HOUSE;
            world[3][3].terrain = TileType.HOUSE;
            world[2][14].terrain = TileType.MARKET;
            world[3][14].terrain = TileType.MARKET;
            for (int x = 1; x < 5; x++) world[10][x].terrain = TileType.WATER;
            for (int x = 0; x < COLS; x++) {
                world[0][x].terrain = TileType.TREE;
                world[11][x].terrain = TileType.TREE;
            }
            int[][] trees = {{1,2},{6,2},{10,2},{16,2},{2,8},{5,9},{11,7},{16,8},{3,4},{17,5},{15,9}};
            for (int[] p : trees) world[p[1]][p[0]].terrain = TileType.TREE;
            // A small ready-to-use tutorial plot.
            world[7][4].tilled = true;
            world[7][4].terrain = TileType.SOIL;
        }

        @Override public void actionPerformed(ActionEvent e) {
            long now = System.currentTimeMillis();
            if (now - lastDayTick >= 90000) {
                nextDay();
                lastDayTick = now;
            }
            if (moving && now - lastMove > 120) {
                walkFrame = (walkFrame + 1) % 2;
                lastMove = now;
            }
            repaint();
        }

        private void nextDay() {
            day++;
            for (FarmTile[] row : world) for (FarmTile tile : row)
                if (tile.crop != null) tile.crop.newDay();
            say("Day " + day + " begins. Crops grow when watered before sleeping.");
        }

        private void say(String text) {
            message = text;
            messageUntil = System.currentTimeMillis() + 3500;
        }

        private Point facingTile() {
            // Work on the tile immediately in front of the player.
            return new Point(playerX, playerY + 1);
        }

        private FarmTile facingFarmTile() {
            Point p = facingTile();
            if (p.x < 0 || p.x >= COLS || p.y < 0 || p.y >= ROWS) return null;
            return world[p.y][p.x];
        }

        private void till() {
            FarmTile tile = facingFarmTile();
            if (tile == null || tile.terrain == TileType.TREE || tile.terrain == TileType.WATER
                    || tile.terrain == TileType.HOUSE || tile.terrain == TileType.MARKET) {
                say("There is no workable soil in front of you."); return;
            }
            if (tile.crop != null) { say("A crop is already growing here."); return; }
            tile.tilled = true;
            tile.terrain = TileType.SOIL;
            say("Soil prepared. Press Q to plant a seed.");
        }

        private void plant() {
            FarmTile tile = facingFarmTile();
            CropType type = cropChoices[selectedSeed];
            if (tile == null || !tile.tilled || tile.crop != null) {
                say("Till an empty grass tile first with SPACE."); return;
            }
            if (seeds.get(type) <= 0) { say("No " + type.label + " seeds left."); return; }
            seeds.put(type, seeds.get(type) - 1);
            tile.crop = new Crop(type);
            say(type.label + " planted. Water it with R, then sleep to advance the day.");
        }

        private void water() {
            FarmTile tile = facingFarmTile();
            if (tile == null || tile.crop == null) { say("There is no crop in front of you."); return; }
            if (tile.crop.mature()) { say("That crop is ready to harvest."); return; }
            tile.crop.wateredToday = true;
            say(tile.crop.type.label + " watered. It can grow when the next day begins.");
        }

        private void interact() {
            if (Math.abs(playerX - 14) <= 1 && Math.abs(playerY - 3) <= 2) {
                int total = 0, earned = 0;
                for (CropType type : CropType.values()) {
                    int amount = harvest.get(type);
                    total += amount;
                    earned += amount * type.sellPrice;
                    harvest.put(type, 0);
                }
                if (total == 0) say("The palengke stall is open. Bring harvested crops here to sell.");
                else {
                    coins += earned;
                    say("Sold " + total + " harvest item(s) for " + earned + " coins!");
                }
                return;
            }
            FarmTile tile = facingFarmTile();
            if (tile != null && tile.crop != null) {
                Crop crop = tile.crop;
                if (!crop.mature()) {
                    say(crop.type.label + " is growing (" + crop.age + "/" + crop.type.daysToGrow + " days).");
                    return;
                }
                harvest.put(crop.type, harvest.get(crop.type) + 1);
                tile.crop = null;
                tile.tilled = true;
                say("Harvested " + crop.type.label + "! Visit the palengke to sell it.");
                return;
            }
            say("Nothing to interact with. Visit the palengke stall or work on the plot ahead.");
        }

        private void move(int dx, int dy) {
            int nx = playerX + dx, ny = playerY + dy;
            if (nx < 0 || nx >= COLS || ny < 1 || ny >= ROWS - 1) return;
            TileType t = world[ny][nx].terrain;
            if (t == TileType.TREE || t == TileType.WATER || t == TileType.HOUSE || t == TileType.MARKET) return;
            playerX = nx; playerY = ny;
            moving = true;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawWorld(g2);
            drawHud(g2);
            if (inventoryOpen) drawInventory(g2);
            if (System.currentTimeMillis() - lastDayTick > 60000) drawDusk(g2);
            g2.dispose();
        }

        private void drawWorld(Graphics2D g) {
            for (int y = 0; y < ROWS; y++) for (int x = 0; x < COLS; x++) {
                int px = x * TILE, py = y * TILE;
                FarmTile tile = world[y][x];
                drawGround(g, px, py, x, y, tile);
                if (tile.terrain == TileType.TREE) drawTree(g, px, py);
                if (tile.terrain == TileType.HOUSE && x == 3 && y == 2) drawHouse(g, px, py);
                if (tile.terrain == TileType.MARKET && x == 14 && y == 2) drawMarket(g, px, py);
                if (tile.crop != null) drawCrop(g, px, py, tile.crop);
            }
            drawPlayer(g, playerX * TILE, playerY * TILE);
        }

        private void drawGround(Graphics2D g, int x, int y, int tx, int ty, FarmTile tile) {
            Color base = switch (tile.terrain) {
                case PATH -> new Color(177, 133, 78);
                case WATER -> new Color(58, 142, 177);
                case SOIL -> new Color(112, 75, 48);
                default -> new Color(88, 151, 76);
            };
            g.setColor(base); g.fillRect(x, y, TILE, TILE);
            g.setColor(new Color(255,255,255,18));
            g.drawLine(x + 5, y + 8, x + 12, y + 8);
            g.drawLine(x + 29, y + 31, x + 37, y + 31);
            if (tile.tilled) {
                g.setColor(new Color(74, 48, 33));
                for (int i = 0; i < 4; i++) g.drawLine(x + 7, y + 13 + i * 7, x + 41, y + 13 + i * 7);
            }
            g.setColor(new Color(24, 62, 40, 45)); g.drawRect(x, y, TILE, TILE);
        }

        private void drawTree(Graphics2D g, int x, int y) {
            g.setColor(new Color(91, 57, 36)); g.fillRoundRect(x+19,y+23,11,25,5,5);
            g.setColor(new Color(28, 91, 54)); g.fillOval(x+2,y+1,43,39);
            g.setColor(new Color(43, 125, 65)); g.fillOval(x+8,y-4,31,34);
            g.setColor(new Color(79, 155, 75)); g.fillOval(x+12,y+2,13,12);
            g.setColor(new Color(20, 70, 43, 90)); g.drawOval(x+2,y+1,43,39);
        }

        private void drawHouse(Graphics2D g, int x, int y) {
            g.setColor(new Color(116, 75, 45)); g.fillRect(x+5,y+19,39,28);
            g.setColor(new Color(89, 49, 38));
            Polygon roof = new Polygon(new int[]{x+1,x+24,x+47}, new int[]{y+22,y+1,y+22},3);
            g.fillPolygon(roof);
            g.setColor(new Color(202, 158, 93)); g.fillRect(x+20,y+29,10,18);
            g.setColor(new Color(244, 208, 132)); g.drawLine(x+7,y+24,x+42,y+24);
            g.setColor(new Color(231, 190, 124)); g.drawString("BAHAY KUBO",x-2,y+59);
        }

        private void drawMarket(Graphics2D g, int x, int y) {
            g.setColor(new Color(91, 56, 35)); g.fillRect(x+5,y+21,39,25);
            g.setColor(new Color(197, 72, 54));
            Polygon awning = new Polygon(new int[]{x+2,x+45,x+40,x+8},new int[]{y+8,y+8,y+23,y+23},4);
            g.fillPolygon(awning);
            g.setColor(new Color(239, 194, 113)); g.drawLine(x+5,y+25,x+44,y+25);
            g.setColor(new Color(255, 235, 184)); g.drawString("PALENGKE",x-4,y+59);
        }

        private void drawCrop(Graphics2D g, int x, int y, Crop crop) {
            int stage = Math.min(3, crop.age * 4 / crop.type.daysToGrow);
            int h = 9 + stage * 7;
            g.setColor(new Color(48, 104, 47)); g.fillRect(x+23,y+34-h/2,3,h);
            g.setColor(new Color(65, 160, 73));
            g.fillOval(x+10,y+29-h/2,17,10);
            g.fillOval(x+22,y+22-h/2,16,10);
            if (stage >= 2) {
                g.setColor(crop.type.color);
                if (crop.type == CropType.PALAY) {
                    for (int i=0;i<4;i++) g.fillOval(x+23+i*3,y+14-h/2+i*3,5,7);
                } else if (crop.type == CropType.PINEAPPLE) {
                    g.fillOval(x+16,y+15,17,21);
                    g.setColor(new Color(47,130,58));
                    for(int i=0;i<5;i++) g.drawLine(x+24,y+16,x+17+i*4,y+5);
                } else g.fillOval(x+17,y+17-h/2,17,17);
            }
            if (crop.mature()) {
                g.setColor(new Color(255, 242, 168));
                g.drawOval(x+12,y+7,25,32);
            }
            if (crop.wateredToday) {
                g.setColor(new Color(104, 197, 239));
                g.fillOval(x+37,y+5,5,8);
            }
        }

        private void drawPlayer(Graphics2D g, int x, int y) {
            // Simple original placeholder farmer sprite made from shapes; replace with sprite sheets later.
            g.setColor(new Color(24, 54, 42, 65)); g.fillOval(x+10,y+37,29,9);
            g.setColor(new Color(190, 128, 79)); g.fillOval(x+14,y+8,21,21);
            g.setColor(new Color(42, 89, 55)); g.fillRect(x+11,y+25,27,19);
            g.setColor(new Color(87, 54, 34)); g.fillRect(x+12,y+40,9,7); g.fillRect(x+28,y+40,9,7);
            g.setColor(new Color(218, 176, 101)); g.fillOval(x+9,y+3,31,10);
            g.setColor(new Color(87, 54, 34)); g.fillRect(x+16,y+15,3,3); g.fillRect(x+29,y+15,3,3);
            if (walkFrame == 1 && moving) g.fillRect(x+23,y+42,8,6);
            moving = false;
        }

        private void drawHud(Graphics2D g) {
            int y = ROWS * TILE;
            g.setColor(new Color(22, 27, 31)); g.fillRect(0,y,WIDTH,112);
            g.setColor(new Color(50, 60, 61)); g.drawLine(0,y,WIDTH,y);
            g.setFont(new Font("SansSerif",Font.BOLD,16));
            g.setColor(new Color(245, 218, 157)); g.drawString("ANIHAN",16,y+24);
            g.setFont(new Font("SansSerif",Font.PLAIN,13));
            g.setColor(Color.WHITE); g.drawString("DAY " + day + "  •  " + (isNight() ? "GABI" : "UMAGA"),120,y+22);
            g.setColor(new Color(246, 204, 104)); g.drawString("₱ " + coins,WIDTH-95,y+22);
            g.setColor(new Color(196, 204, 197));
            g.drawString("SPACE Till   Q Plant   R Water   E Harvest / Sell   H Sleep   I Bag",16,y+48);
            for (int i=0;i<4;i++) {
                int bx=16+i*78, by=y+59;
                g.setColor(i==selectedSeed ? new Color(105, 133, 76) : new Color(45, 55, 51));
                g.fillRoundRect(bx,by,68,42,9,9);
                g.setColor(i==selectedSeed ? new Color(235, 223, 173) : new Color(105, 118, 108));
                g.drawRoundRect(bx,by,68,42,9,9);
                g.setColor(Color.WHITE); g.drawString((i+1)+" "+cropChoices[i].label, bx+7,by+17);
                g.setColor(new Color(186, 215, 165)); g.drawString("x"+seeds.get(cropChoices[i]),bx+7,by+34);
            }
            if (System.currentTimeMillis() < messageUntil) {
                g.setColor(new Color(10, 15, 15, 220)); g.fillRoundRect(390,y+58,WIDTH-405,43,10,10);
                g.setColor(new Color(244, 232, 199)); g.setFont(new Font("SansSerif",Font.PLAIN,12));
                drawWrapped(g,message,400,y+76,WIDTH-425);
            }
        }

        private void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth) {
            FontMetrics fm=g.getFontMetrics();
            String line="";
            for(String word:text.split(" ")) {
                String test=line.isEmpty()?word:line+" "+word;
                if(fm.stringWidth(test)>maxWidth && !line.isEmpty()) {
                    g.drawString(line,x,y); y+=15; line=word;
                } else line=test;
            }
            if(!line.isEmpty()) g.drawString(line,x,y);
        }

        private void drawInventory(Graphics2D g) {
            int x=WIDTH/2-190,y=HEIGHT/2-135;
            g.setColor(new Color(15,20,22,242)); g.fillRoundRect(x,y,380,270,18,18);
            g.setColor(new Color(178,148,91)); g.drawRoundRect(x,y,380,270,18,18);
            g.setFont(new Font("SansSerif",Font.BOLD,20)); g.setColor(new Color(245,218,157));
            g.drawString("BAYONG / INVENTORY",x+22,y+34);
            g.setFont(new Font("SansSerif",Font.PLAIN,15)); g.setColor(Color.WHITE);
            int row=0;
            for(CropType type:CropType.values()) {
                int yy=y+75+row*38;
                g.setColor(type.color); g.fillOval(x+25,yy-12,18,18);
                g.setColor(Color.WHITE);
                g.drawString(type.label+"  |  Seeds: "+seeds.get(type)+"  |  Harvest: "+harvest.get(type),x+58,yy+2);
                row++;
            }
            g.setColor(new Color(180,190,180)); g.drawString("Press I to close",x+22,y+244);
        }

        private boolean isNight() {
            long elapsed=System.currentTimeMillis()-lastDayTick;
            return elapsed > 65000;
        }

        private void drawDusk(Graphics2D g) {
            g.setColor(new Color(17, 22, 61, 78));
            g.fillRect(0,0,WIDTH,ROWS*TILE);
            g.setColor(new Color(235,220,164)); g.fillOval(WIDTH-85,18,24,24);
        }

        @Override public void keyPressed(KeyEvent e) {
            int key=e.getKeyCode();
            switch(key) {
                case KeyEvent.VK_W, KeyEvent.VK_UP -> move(0,-1);
                case KeyEvent.VK_S, KeyEvent.VK_DOWN -> move(0,1);
                case KeyEvent.VK_A, KeyEvent.VK_LEFT -> move(-1,0);
                case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> move(1,0);
                case KeyEvent.VK_SPACE -> till();
                case KeyEvent.VK_Q -> plant();
                case KeyEvent.VK_R -> water();
                case KeyEvent.VK_E -> interact();
                case KeyEvent.VK_H -> { nextDay(); lastDayTick=System.currentTimeMillis(); }
                case KeyEvent.VK_I -> inventoryOpen=!inventoryOpen;
                case KeyEvent.VK_1 -> selectedSeed=0;
                case KeyEvent.VK_2 -> selectedSeed=1;
                case KeyEvent.VK_3 -> selectedSeed=2;
                case KeyEvent.VK_4 -> selectedSeed=3;
                default -> { }
            }
            repaint();
        }
        @Override public void keyReleased(KeyEvent e) { moving=false; }
        @Override public void keyTyped(KeyEvent e) { }
    }
}
