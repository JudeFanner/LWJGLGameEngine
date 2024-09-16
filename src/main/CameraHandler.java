package main;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CameraHandler {
    private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
    private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw, pitch;
    private float lastX = 400, lastY = 300;
    private boolean firstMouse = true;
    private final float sensitivity = 0.1f;

    public CameraHandler() {
        cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
        cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
        yaw = -90.0f;
        pitch = 0.0f;
    }

    public void processMouseMovement(double xpos, double ypos) {
        if (firstMouse) {
            lastX = (float) xpos;
            lastY = (float) ypos;
            firstMouse = false;
        }

        float xoffset = (float) xpos - lastX;
        float yoffset = lastY - (float) ypos;
        lastX = (float) xpos;
        lastY = (float) ypos;

        xoffset *= sensitivity;
        yoffset *= sensitivity;

        yaw += xoffset;
        pitch += yoffset;

        if (pitch > 89.0f)
            pitch = 89.0f;
        if (pitch < -89.0f)
            pitch = -89.0f;

        updateCameraVectors();
    }

    private void updateCameraVectors() {
        Vector3f front = new Vector3f(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
        );
        cameraFront = front.normalize();
    }

    public Matrix4f getViewMatrix(Vector3f playerPosition) {
        Vector3f cameraPos = getCameraPos(playerPosition);
        return new Matrix4f().lookAt(
                cameraPos,
                new Vector3f(cameraPos).add(cameraFront),
                cameraUp
        );
    }

    public void update(Vector3f playerPosition) {
        // Update cameraFront based on yaw and pitch
        cameraFront.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        cameraFront.y = (float) Math.sin(Math.toRadians(pitch));
        cameraFront.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        cameraFront.normalize();

//        System.out.println("Camera Front: " + cameraFront);
//        System.out.println("Player Position: " + playerPosition);
//        System.out.println("Camera Position: " + getCameraPos(playerPosition));
    }

    public Vector3f getCameraPos(Vector3f playerPosition) {
        return new Vector3f(playerPosition).add(0, 1, 0);
    }

    public double getYaw() {
        return yaw;
    }

    public Vector3f getCameraFront() {
        return cameraFront;
    }

    public Matrix3f getRotationMatrix() {
        return new Matrix3f().rotateY(yaw).rotateX(pitch);
    }
}