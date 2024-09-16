package main;

import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private long window;
    private boolean cursorDisabled = true;
    private Player player;
    private CameraHandler cameraHandler;

    public InputHandler(long window, Player player, CameraHandler cameraHandler) {
        this.window = window;
        this.player = player;
        this.cameraHandler = cameraHandler;
        setupCallbacks();
    }

    private void setupCallbacks() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (cursorDisabled) {
                cameraHandler.processMouseMovement(xpos, ypos);
            }
        });

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                cursorDisabled = !cursorDisabled;
                glfwSetInputMode(window, GLFW_CURSOR, cursorDisabled ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
            }
            if (key == GLFW_KEY_SPACE && action == GLFW_PRESS) {
                player.jump();
            }
        });
    }

    public void processInput() {
        Vector3f moveDirection = new Vector3f(0, 0, 0);

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            moveDirection.x += 1;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            moveDirection.x -= 1;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            moveDirection.z -= 1;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            moveDirection.z += 1;

        // Rotate the move direction based on the camera's yaw
        float yaw = (float) Math.toRadians(cameraHandler.getYaw());
        float rotatedX = moveDirection.x * (float) Math.cos(yaw) - moveDirection.z * (float) Math.sin(yaw);
        float rotatedZ = moveDirection.x * (float) Math.sin(yaw) + moveDirection.z * (float) Math.cos(yaw);
        moveDirection.x = rotatedX;
        moveDirection.z = rotatedZ;

        // Only call move if there's actual input
        if (moveDirection.lengthSquared() > 0) {
            player.move(moveDirection);
        } else {
            // If no input, set horizontal velocity to zero
            Vector3f currentVelocity = player.getVelocity();
            player.setVelocity(new Vector3f(0, currentVelocity.y, 0));
        }

        //System.out.println("Input processed - Move direction: " +
        // moveDirection);
    }

    public boolean isCursorDisabled() {
        return cursorDisabled;
    }
}