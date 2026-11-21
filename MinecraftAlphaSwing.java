import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

public class MinecraftAlphaSwing extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 300;
    private static final int SCALE = 2;
    private static final int WORLD_X = 64;
    private static final int WORLD_Y = 32;
    private static final int WORLD_Z = 64;

    private static final byte AIR = 0;
    private static final byte GRASS = 1;
    private static final byte DIRT = 2;
    private static final byte STONE = 3;
    private static final byte WOOD = 4;
    private static final byte LEAVES = 5;
    private static final byte COBBLESTONE = 6;

    private final byte[][][] world = new byte[WORLD_X][WORLD_Y][WORLD_Z];
    private final BufferedImage screen;
    private final int[] pixels;

    private double posX = 32.5, posY = 22.0, posZ = 32.5;
    private double velY = 0;
    private double yaw = 0;   
    private double pitch = 0; 
    private boolean onGround = false;
    private byte selectedBlock = COBBLESTONE;
    private final boolean[] keys = new boolean[256];
    
    private Robot robot;
    private boolean mouseLocked = false;
    private int centerX, centerY;

    public MinecraftAlphaSwing() {
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();

        generateWorld();

        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void generateWorld() {
        Random rand = new Random(1337);
        double seed1 = rand.nextDouble() * 100;
        double seed2 = rand.nextDouble() * 100;

        for (int x = 0; x < WORLD_X; x++) {
            for (int z = 0; z < WORLD_Z; z++) {
                double h = 10
                        + Math.sin(x * 0.12 + seed1) * 3.0
                        + Math.cos(z * 0.12 + seed2) * 3.0
                        + Math.sin((x + z) * 0.07) * 2.5;
                int height = Math.max(4, Math.min(WORLD_Y - 8, (int) h));

                for (int y = 0; y <= height; y++) {
                    if (y == height) {
                        world[x][y][z] = GRASS;
                    } else if (y > height - 3) {
                        world[x][y][z] = DIRT;
                    } else {
                        world[x][y][z] = STONE;
                    }
                }
                if (x > 3 && x < WORLD_X - 4 && z > 3 && z < WORLD_Z - 4 && rand.nextInt(45) == 0) {
                    buildTree(x, height + 1, z);
                }
            }
        }
    }

    private void buildTree(int x, int y, int z) {
        int trunkHeight = 4;
        if (y + trunkHeight + 2 >= WORLD_Y) return;

        for (int i = 0; i < trunkHeight; i++) {
            world[x][y + i][z] = WOOD;
        }
        for (int lx = x - 2; lx <= x + 2; lx++) {
            for (int lz = z - 2; lz <= z + 2; lz++) {
                for (int ly = y + trunkHeight - 1; ly <= y + trunkHeight + 1; ly++) {
                    if (Math.abs(lx - x) == 2 && Math.abs(lz - z) == 2 && ly == y + trunkHeight + 1) continue;
                    if (world[lx][ly][lz] == AIR) {
                        world[lx][ly][lz] = LEAVES;
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;

        while (true) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            while (delta >= 1) {
                updatePlayer();
                delta--;
            }

            renderWorld();
            repaint();

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}
        }
    }

    private void updatePlayer() {
        double speed = 0.11;
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        double moveX = 0;
        double moveZ = 0;

        if (keys[KeyEvent.VK_W]) {
            moveX += sin;
            moveZ += cos;
        }
        if (keys[KeyEvent.VK_S]) {
            moveX -= sin;
            moveZ -= cos;
        }
        if (keys[KeyEvent.VK_A]) {
            moveX -= cos;
            moveZ += sin;
        }
        if (keys[KeyEvent.VK_D]) {
            moveX += cos;
            moveZ -= sin;
        }

        double len = Math.hypot(moveX, moveZ);
        if (len > 0) {
            moveX = (moveX / len) * speed;
            moveZ = (moveZ / len) * speed;
        }
        if (!collides(posX + moveX, posY, posZ)) posX += moveX;
        if (!collides(posX, posY, posZ + moveZ)) posZ += moveZ;
        
        if (keys[KeyEvent.VK_SPACE] && onGround) {
            velY = 0.23;
            onGround = false;
        }

        velY -= 0.015; 
        if (!collides(posX, posY + velY, posZ)) {
            posY += velY;
            onGround = false;
        } else {
            if (velY < 0) onGround = true;
            velY = 0;
        }
        if (posY < 1) {
            posY = 25;
            velY = 0;
        }
    }

    private boolean collides(double x, double y, double z) {
        double r = 0.28; 
        double h = 1.65; 

        for (double dx : new double[]{-r, r}) {
            for (double dz : new double[]{-r, r}) {
                for (double dy : new double[]{0.0, 0.8, h}) {
                    int bx = (int) Math.floor(x + dx);
                    int by = (int) Math.floor(y + dy);
                    int bz = (int) Math.floor(z + dz);
                    if (getBlock(bx, by, bz) != AIR) return true;
                }
            }
        }
        return false;
    }

    private byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= WORLD_X || y < 0 || y >= WORLD_Y || z < 0 || z >= WORLD_Z) {
            return AIR;
        }
        return world[x][y][z];
    }

    private void renderWorld() {
        double eyeX = posX;
        double eyeY = posY + 1.5; 
        double eyeZ = posZ;
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        
        int skyColor = 0x87CEEB; 

        for (int py = 0; py < HEIGHT; py++) {
            double v = (HEIGHT / 2.0 - py) / (HEIGHT * 0.75);

            for (int px = 0; px < WIDTH; px++) {
                double u = (px - WIDTH / 2.0) / (HEIGHT * 0.75);
                double rx = u;
                double ry = v;
                double rz = 1.0;
                double ry1 = ry * cosPitch + rz * sinPitch;
                double rz1 = -ry * sinPitch + rz * cosPitch;
                double dx = rx * cosYaw + rz1 * sinYaw;
                double dy = ry1;
                double dz = -rx * sinYaw + rz1 * cosYaw;
                double invLen = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx *= invLen;
                dy *= invLen;
                dz *= invLen;
                
                int[] hit = raycast(eyeX, eyeY, eyeZ, dx, dy, dz, 36.0);
                if (hit != null) {
                    pixels[py * WIDTH + px] = getBlockColor(getBlock(hit[0], hit[1], hit[2]), hit[3], hit[0], hit[1], hit[2]);
                } else {
                    pixels[py * WIDTH + px] = skyColor;
                }
            }
        }
    }

    private int getBlockColor(byte block, int side, int x, int y, int z) {
        int color;
        switch (block) {
            case GRASS: 
                color = (side == 1) ? 0x7FB238 : 0x866043; 
                break;
            case DIRT: 
                color = 0x866043; 
                break;
            case STONE: 
                color = 0x7F7F7F; 
                break;
            case WOOD: 
                color = (side == 1 || side == -1) ? 0xBD9B63 : 0x6E512E; 
                break;
            case LEAVES: 
                color = 0x3A852B; 
                break;
            case COBBLESTONE: 
                color = 0x6E6E6E; 
                break;
            default: 
                color = 0x777777; 
                break;
        }

        if (side == 2) {
            color = darken(color, 0.75);
        } else if (side == 0) {
            color = darken(color, 0.60);
        } else if (side == -1 || side == 3) {
            color = darken(color, 0.50);
        }

        return color;
    }

    private int darken(int hexColor, double factor) {
        int r = (int) (((hexColor >> 16) & 0xFF) * factor);
        int g = (int) (((hexColor >> 8) & 0xFF) * factor);
        int b = (int) ((hexColor & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private int[] raycast(double ox, double oy, double oz, double dx, double dy, double dz, double maxDist) {
        int mapX = (int) Math.floor(ox);
        int mapY = (int) Math.floor(oy);
        int mapZ = (int) Math.floor(oz);

        double deltaDistX = (dx == 0) ? 1e30 : Math.abs(1.0 / dx);
        double deltaDistY = (dy == 0) ? 1e30 : Math.abs(1.0 / dy);
        double deltaDistZ = (dz == 0) ? 1e30 : Math.abs(1.0 / dz);

        int stepX = (dx < 0) ? -1 : 1;
        int stepY = (dy < 0) ? -1 : 1;
        int stepZ = (dz < 0) ? -1 : 1;

        double sideDistX = (dx < 0) ? (ox - mapX) * deltaDistX : (mapX + 1.0 - ox) * deltaDistX;
        double sideDistY = (dy < 0) ? (oy - mapY) * deltaDistY : (mapY + 1.0 - oy) * deltaDistY;
        double sideDistZ = (dz < 0) ? (oz - mapZ) * deltaDistZ : (mapZ + 1.0 - oz) * deltaDistZ;

        double dist = 0;
        int nx = 0, ny = 0, nz = 0;
        int side = 0;

        while (dist < maxDist) {
            if (sideDistX < sideDistY) {
                if (sideDistX < sideDistZ) {
                    dist = sideDistX;
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    nx = -stepX; ny = 0; nz = 0;
                    side = 0;
                } else {
                    dist = sideDistZ;
                    sideDistZ += deltaDistZ;
                    mapZ += stepZ;
                    nx = 0; ny = 0; nz = -stepZ;
                    side = 2;
                }
            } else {
                if (sideDistY < sideDistZ) {
                    dist = sideDistY;
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    nx = 0; ny = -stepY; nz = 0;
                    side = 1;
                } else {
                    dist = sideDistZ;
                    sideDistZ += deltaDistZ;
                    mapZ += stepZ;
                    nx = 0; ny = 0; nz = -stepZ;
                    side = 2;
                }
            }

            if (getBlock(mapX, mapY, mapZ) != AIR) {
                return new int[]{mapX, mapY, mapZ, side, nx, ny, nz};
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(screen, 0, 0, getWidth(), getHeight(), null);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        
        g.setColor(Color.WHITE);
        g.drawLine(cx - 8, cy, cx + 8, cy);
        g.drawLine(cx, cy - 8, cx, cy + 8);
        
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("Bloco [1-5]: " + getBlockName(selectedBlock), 12, 22);
  
        if (!mouseLocked) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(cx - 160, cy + 20, 320, 30);
            g.setColor(Color.YELLOW);
            g.drawString("Clique na tela para capturar o mouse", cx - 150, cy + 40);
        }
    }

    private String getBlockName(byte id) {
        return switch (id) {
            case GRASS -> "Grama";
            case DIRT -> "Terra";
            case STONE -> "Pedra";
            case WOOD -> "Madeira";
            default -> "Pedregulho";
        };
    }
        
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = true;
        if (code == KeyEvent.VK_1) selectedBlock = COBBLESTONE;
        if (code == KeyEvent.VK_2) selectedBlock = GRASS;
        if (code == KeyEvent.VK_3) selectedBlock = DIRT;
        if (code == KeyEvent.VK_4) selectedBlock = STONE;
        if (code == KeyEvent.VK_5) selectedBlock = WOOD;
        if (code == KeyEvent.VK_ESCAPE) {
            mouseLocked = false;
            setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!mouseLocked) {
            mouseLocked = true;
            BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            setCursor(Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank"));
            recenterMouse();
            return;
        }
        
        double dx = Math.sin(yaw) * Math.cos(pitch);
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);
        int[] hit = raycast(posX, posY + 1.5, posZ, dx, dy, dz, 6.0);
        
        if (hit != null) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                world[hit[0]][hit[1]][hit[2]] = AIR;
            } else if (SwingUtilities.isRightMouseButton(e)) {
                int bx = hit[0] + hit[4];
                int by = hit[1] + hit[5];
                int bz = hit[2] + hit[6];
                if (bx >= 0 && bx < WORLD_X && by >= 0 && by < WORLD_Y && bz >= 0 && bz < WORLD_Z) {
                    world[bx][by][bz] = selectedBlock;
                    if (collides(posX, posY, posZ)) {
                        world[bx][by][bz] = AIR;
                    }
                }
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!mouseLocked || robot == null) return;
        Point loc = getLocationOnScreen();
        centerX = loc.x + getWidth() / 2;
        centerY = loc.y + getHeight() / 2;
        int dx = e.getXOnScreen() - centerX;
        int dy = e.getYOnScreen() - centerY;
          
        if (dx != 0 || dy != 0) {
            yaw += dx * 0.003;
            pitch -= dy * 0.003;
            pitch = Math.max(-Math.PI / 2 + 0.05, Math.min(Math.PI / 2 - 0.05, pitch));
            recenterMouse();
        }
    }

    private void recenterMouse() {
        if (robot != null && isShowing()) {
            Point loc = getLocationOnScreen();
            robot.mouseMove(loc.x + getWidth() / 2, loc.y + getHeight() / 2);
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Minecraft Alpha - Java Swing");
        MinecraftAlphaSwing game = new MinecraftAlphaSwing();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        new Thread(game).start();
    }
}
