package main;

import org.joml.Matrix3f;
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

    public Vector3f processInput(float deltaTime, ShaderHandler shader) {
        Vector3f wishDir = new Vector3f(0, 0, 0);

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) wishDir.z -= 1;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) wishDir.z += 1;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) wishDir.x -= 1;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) wishDir.x += 1;

        // Normalize wishDir if it's not zero
        if (wishDir.lengthSquared() > 0) {
            wishDir.normalize();
        }

        // Rotate wishDir based on camera's orientation
        Vector3f forward = new Vector3f(cameraHandler.getCameraFront()).mul(1, 0, 1).normalize();
        Vector3f right = new Vector3f(forward).cross(0, 1, 0).normalize();
        Vector3f rotatedWishDir = new Vector3f();
        rotatedWishDir.add(new Vector3f(forward).mul(-wishDir.z)); // Invert the z-component here
        rotatedWishDir.add(new Vector3f(right).mul(wishDir.x));
        wishDir.set(rotatedWishDir);

        // Handle sprinting
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            player.setSprinting(true);
        } else {
            player.setSprinting(false);
        }

        // Handle flying (if implemented)
        if (glfwGetKey(window, GLFW_KEY_F) == GLFW_PRESS) {
            player.toggleCheatFlying();
        }

        // Handle jumping
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            player.jump();
        }

        System.out.println("Input processed - Wish direction: " + wishDir);

        return wishDir;
    }

    public boolean isCursorDisabled() {
        return cursorDisabled;
    }
}