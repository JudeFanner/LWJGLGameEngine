package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

public class MeshRenderer {
    private Mesh mesh;
    private boolean isEnabled;
    private Matrix4f modelMatrix;
    private Vector3f position;
    private Vector3f rotation;
    private Vector3f scale;

    public MeshRenderer(Mesh mesh) {
        this.mesh = mesh;
        this.isEnabled = true;
        this.modelMatrix = new Matrix4f().identity();
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.scale = new Vector3f(1, 1, 1);
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void setModelMatrix(Matrix4f modelMatrix) {
        this.modelMatrix = modelMatrix;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    public void update(float deltaTime) {
        // update the model matrix based on position, rotation, and scale
        modelMatrix.identity()
                .translate(position)
                .rotateX((float) Math.toRadians(rotation.x))
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateZ((float) Math.toRadians(rotation.z))
                .scale(scale);

        // you can add other update logic here, such as animations
        // for example:
        // rotation.y += 45 * deltaTime; // rotate 45 degrees per second around y-axis
    }

    public void render(int shaderProgram) {
        if (isEnabled) {
            int modelLoc = glGetUniformLocation(shaderProgram, "model");
            glUniformMatrix4fv(modelLoc, false, modelMatrix.get(new float[16]));
            mesh.render();
        }
    }

    public Matrix4f getModelMatrix() {
        return new Matrix4f(modelMatrix);
    }
}