package main;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import java.nio.ByteBuffer;

public class DebugOverlay {
    private Player player;
    private Engine engine;
    private long lastFpsTime;
    private int fps;
    private int fpsCount;

    public DebugOverlay(Player player, Engine engine) {
        this.player = player;
        this.engine = engine;
        this.lastFpsTime = System.currentTimeMillis();
    }

    public void render() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL20.glUseProgram(0);  // Use fixed-function pipeline

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 800, 600, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glColor3f(1, 1, 1);

        renderText("FPS: " + calculateFPS(), 10, 20);
        renderText("Position: " + player.getPosition(), 10, 40);
        renderText("Velocity: " + player.getVelocity(), 10, 60);
        renderText("Grounded: " + player.isGrounded(), 10, 80);
        renderText("Sprinting: " + player.isSprinting(), 10, 100);
        renderText("Cheat Flying: " + player.isCheatFlying(), 10, 120);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void renderText(String text, int x, int y) {
        GL11.glRasterPos2i(x, y);
        for (char c : text.toCharArray()) {
            ByteBuffer charBitmap = Engine.getFontBitmap(c);
            GL11.glBitmap(8, 12, 0, 0, 8, 0, charBitmap);
        }
    }

    private int calculateFPS() {
        long currentTime = System.currentTimeMillis();
        fpsCount++;
        if (currentTime - lastFpsTime > 1000) {
            fps = fpsCount;
            fpsCount = 0;
            lastFpsTime = currentTime;
        }
        return fps;
    }
}