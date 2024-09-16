package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL20.*;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



public class ShaderHandler {
    private int shaderProgram;

    public ShaderHandler() {
        this.shaderProgram = createShaderProgram();
        if (this.shaderProgram == 0) {
            throw new RuntimeException("Failed to create shader program");
        }
        printActiveUniforms();
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    public void useShaderProgram() {
        if (shaderProgram <= 0) {
            return;
        }
        glUseProgram(shaderProgram);
    }

    public void printActiveUniforms() {
        IntBuffer numUniforms = BufferUtils.createIntBuffer(1);
        glGetProgramiv(shaderProgram, GL_ACTIVE_UNIFORMS, numUniforms);
        int uniformCount = numUniforms.get(0);

        IntBuffer size = BufferUtils.createIntBuffer(1);
        IntBuffer type = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < uniformCount; i++) {
            String name = glGetActiveUniform(shaderProgram, i, size, type);
            int location = glGetUniformLocation(shaderProgram, name);
        }
    }

    public void reloadShaders() {
        try {
            int newProgram = createShaderProgram();
            glDeleteProgram(shaderProgram);
            shaderProgram = newProgram;

            printActiveUniforms();

            useShaderProgram();
        } catch (Exception e) {
        }
    }

    private int createShaderProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, "vertex.glsl");
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, "fragment.glsl");

        int program = glCreateProgram();
        if (program == 0) {
            return 0;
        }

        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (!checkShaderLinkErrors(program)) {
            glDeleteProgram(program);
            return 0;
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private int linkProgram(int vertexShader, int fragmentShader) {
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        checkShaderLinkErrors(program);

        return program;
    }

    private boolean checkShaderLinkErrors(int program) {
        IntBuffer success = BufferUtils.createIntBuffer(1);
        glGetProgramiv(program, GL_LINK_STATUS, success);
        if (success.get(0) == GL_FALSE) {
            int len = glGetProgrami(program, GL_INFO_LOG_LENGTH);
            String log = glGetProgramInfoLog(program, len);
            return false;
        }
        return true;
    }

    private int compileShader(int type, String fileName) {
        String source = loadShaderSource(fileName);

        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        int success = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(shader, GL_INFO_LOG_LENGTH);
            throw new IllegalStateException("Shader compilation failed");
        }

        return shader;
    }

    private String loadShaderSource(String fileName) {
        try {
            Path path = Paths.get("shaders", fileName);
            byte[] encoded = Files.readAllBytes(path);
            String source = new String(encoded, StandardCharsets.UTF_8);
            return source;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader file: " + fileName, e);
        }
    }

    private void checkShaderCompileErrors(int shader, String type) {
        int success = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(shader, GL_INFO_LOG_LENGTH);
        }
    }

    public void setUniform(String name, Matrix4f value) {
        int location = glGetUniformLocation(shaderProgram, name);
        if (location != -1) {
            glUniformMatrix4fv(location, false, value.get(new float[16]));
        } else {
            throw new IllegalStateException((name + ": Uniform location is " +
                    "invalid"));
        }
    }

    public void setLightUniforms(Vector3f lightPos, Vector3f viewPos, Vector3f lightColor, Vector3f objectColor) {
        glUniform3f(glGetUniformLocation(shaderProgram, "lightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform3f(glGetUniformLocation(shaderProgram, "viewPos"), viewPos.x, viewPos.y, viewPos.z);
        glUniform3f(glGetUniformLocation(shaderProgram, "lightColor"), lightColor.x, lightColor.y, lightColor.z);
        glUniform3f(glGetUniformLocation(shaderProgram, "objectColor"), objectColor.x, objectColor.y, objectColor.z);
    }

    public void setFogUniforms(Vector3f fogColor,
                               float fogStart,
                               float fogEnd)
    {
        glUniform3f(glGetUniformLocation(shaderProgram, "fogColor"),
                fogColor.x,
                fogColor.y,
                fogColor.z);
        glUniform1f(glGetUniformLocation(shaderProgram, "fogStart"), fogStart);
        glUniform1f(glGetUniformLocation(shaderProgram, "fogEnd"), fogEnd);


    }
}
