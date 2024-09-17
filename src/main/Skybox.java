package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Skybox {
    private static final float[] VERTICES = {
            // ... (same vertices as before)
    };

    private int vaoId;
    private int vboId;
    private int textureId;
    private ShaderHandler shaderHandler;

    public Skybox(ShaderHandler shaderHandler, int textureId) {
        this.shaderHandler = shaderHandler;
        this.textureId = textureId;
        setupMesh();
    }

    private void setupMesh() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer verticesBuffer = stack.mallocFloat(VERTICES.length);
            verticesBuffer.put(VERTICES).flip();

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);

            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        }
    }

    public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        shaderHandler.useSkyboxShaderProgram();

        // Remove translation from the view matrix
        Matrix4f viewMatrixNoTranslation = new Matrix4f(viewMatrix).setTranslation(0, 0, 0);

        shaderHandler.setSkyboxUniform("viewMatrix", viewMatrixNoTranslation);
        shaderHandler.setSkyboxUniform("projectionMatrix", projectionMatrix);

        glBindVertexArray(vaoId);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, textureId);

        glDrawArrays(GL_TRIANGLES, 0, 36);

        glBindVertexArray(0);
    }

    public void setFogColor(Vector3f fogColor) {
        shaderHandler.setSkyboxUniform("fogColor", fogColor);
    }

    public void setSkyColor(Vector3f skyColor) {
        shaderHandler.setSkyboxUniform("skyColor", skyColor);
    }

    public void setFogStartEnd(float fogStart, float fogEnd) {
        shaderHandler.setSkyboxUniform("fogStart", fogStart);
        shaderHandler.setSkyboxUniform("fogEnd", fogEnd);
    }

    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}
