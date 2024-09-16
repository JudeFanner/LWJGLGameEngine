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
            //System.err.println("Invalid shader program ID: " + shaderProgram);
            return;
        }
        glUseProgram(shaderProgram);
        //System.out.println("Using shader program: " + shaderProgram);
    }

    public void printActiveUniforms() {
        IntBuffer numUniforms = BufferUtils.createIntBuffer(1);
        glGetProgramiv(shaderProgram, GL_ACTIVE_UNIFORMS, numUniforms);
        int uniformCount = numUniforms.get(0);
        //System.out.println("Number of active uniforms: " + uniformCount);

        IntBuffer size = BufferUtils.createIntBuffer(1);
        IntBuffer type = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < uniformCount; i++) {
            String name = glGetActiveUniform(shaderProgram, i, size, type);
            int location = glGetUniformLocation(shaderProgram, name);
            //System.out.println("Uniform #" + i + " Name: " + name + " Type: " + type.get(0) + " Size: " + size.get(0) + " Location: " + location);
        }
    }

    public void reloadShaders() {
        try {
            //System.out.println("reloading shaders...");
            int newProgram = createShaderProgram();
            glDeleteProgram(shaderProgram);
            shaderProgram = newProgram;

            printActiveUniforms();

            useShaderProgram();
            //System.out.println("shaders reloaded successfully.");
        } catch (Exception e) {
            //System.err.println("failed to reload shaders: " + e.getMessage());
        }
    }

    private int createShaderProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, "vertex.glsl");
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, "fragment.glsl");

        int program = glCreateProgram();
        if (program == 0) {
            //System.err.println("Failed to create shader program");
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

        //System.out.println("Created shader program with ID: " + program);
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
            //System.err.println("ERROR::PROGRAM_LINKING_ERROR");
            //System.err.println(log);
            return false;
        }
        return true;
    }

    private int compileShader(int type, String fileName) {
        String source = loadShaderSource(fileName);
        //System.out.println("Compiling " + (type == GL_VERTEX_SHADER ? "vertex" : "fragment") + " shader:");
        //System.out.println(source);

        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        int success = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(shader, GL_INFO_LOG_LENGTH);
            //System.out.println("ERROR::SHADER_COMPILATION_ERROR of type: "
            // + (type == GL_VERTEX_SHADER ? "VERTEX" : "FRAGMENT"));
            //System.out.println(glGetShaderInfoLog(shader, len));
            throw new IllegalStateException("Shader compilation failed");
        }

        return shader;
    }

    private String loadShaderSource(String fileName) {
        try {
            Path path = Paths.get("shaders", fileName);
            byte[] encoded = Files.readAllBytes(path);
            String source = new String(encoded, StandardCharsets.UTF_8);
            //System.out.println("Loaded shader source for " + fileName + ":");
            //System.out.println(source);
            return source;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader file: " + fileName, e);
        }
    }

    private void checkShaderCompileErrors(int shader, String type) {
        int success = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(shader, GL_INFO_LOG_LENGTH);
            //System.out.println("ERROR::SHADER_COMPILATION_ERROR of type: " + type);
            //System.out.println(glGetShaderInfoLog(shader, len));
        }
    }

    public void setUniform(String name, Matrix4f value) {
        int location = glGetUniformLocation(shaderProgram, name);
        if (location != -1) {
            glUniformMatrix4fv(location, false, value.get(new float[16]));
            //System.out.println("Set uniform '" + name + "' at location: " + location);
        } else {
            //System.err.println("Uniform '" + name + "' not found in shader program.");
        }
    }

    public void setLightUniforms(Vector3f lightPos, Vector3f viewPos, Vector3f lightColor, Vector3f objectColor) {
        glUniform3f(glGetUniformLocation(shaderProgram, "lightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform3f(glGetUniformLocation(shaderProgram, "viewPos"), viewPos.x, viewPos.y, viewPos.z);
        glUniform3f(glGetUniformLocation(shaderProgram, "lightColor"), lightColor.x, lightColor.y, lightColor.z);
        glUniform3f(glGetUniformLocation(shaderProgram, "objectColor"), objectColor.x, objectColor.y, objectColor.z);
    }
}
