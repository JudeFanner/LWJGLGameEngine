package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.NULL;


public class Engine {
    private long window;
    private int width = 1280;
    private int height = 720;

    private Player player;
    private final float deltaTime = 0.0f;
    private final float lastFrame = 0.0f;
    private MeshHandler meshHandler;
    private DebugOverlay debugOverlay;

    private GLFWErrorCallback errorCallback;

    private ShaderHandler shaderHandler;
    private InputHandler inputHandler;
    private CameraHandler cameraHandler;
    private Skybox skybox;
    private int skyboxTexture;

    Vector3f lightPos = new Vector3f(5.0f, 5.0f, 5.0f);
    Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);
    Vector3f objectColor = new Vector3f(0.5f, 0.5f, 0.5f);


    Vector4f clearColor = new Vector4f(51/255f, 76/255f, 75/255f, 1.0f);
    Vector3f fogColor = new Vector3f(0.6f, 0.650f, 1.0f);
    Vector3f skyColor = new Vector3f(72f/255f, 124f/255f, 229f/255f);

    private boolean debugMode = false; // set to true to enable debug mode
    private float debugTimer = 0f;
    private int debugStep = 0;
    private final float DEBUG_DURATION = 2.0f;
    private final float DEBUG_STEP_DURATION = 0.5f;

    private Matrix4f projectionMatrix;

    private float fogStart = 6.0f;
    private float fogEnd = 30.0f;
    private static BufferedImage fontImage;
    private static final int CHAR_WIDTH = 8;
    private static final int CHAR_HEIGHT = 12;

    private void init() {
        // set up an error callback
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);

        // initialize glfw
        if (!glfwInit()) {
            throw new IllegalStateException("unable to initialize glfw");
        }

        // glfw config
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);

        // spawn a new window
        window = glfwCreateWindow(width, height, "lwjgl game", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("failed to create the glfw window");
        }

        glfwSetWindowPos(
                window,
                (vidmode.width() - width) / 2,
                (vidmode.height() - height) / 2
        );

        glfwSetFramebufferSizeCallback(window, (window, newWidth, newHeight) -> {
            width = newWidth;
            height = newHeight;
            glViewport(0, 0, width, height);
            updateProjectionMatrix();
        });

        // make the opengl context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // enable v-sync
        glfwShowWindow(window);

        GL.createCapabilities();

        // set the clear color
        glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);

        // enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        // initialize handlers
        shaderHandler = new ShaderHandler();
        player = new Player(new Vector3f(0, 1, 0));
        player.setPosition(new Vector3f(1.0f, 0, 3f));
        player.setVelocity(new Vector3f(0.1f, 0, 0.1f)); // Set a small initial velocity

        debugOverlay = new DebugOverlay(player, this);
        cameraHandler = new CameraHandler();
        inputHandler = new InputHandler(window, player, cameraHandler);

        meshHandler = new MeshHandler();
        meshHandler.loadMeshes();
        meshHandler.addGroundPlane();

        // Initialize projection matrix
        float aspectRatio = (float) width / height;
        projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(45.0f),
                aspectRatio, 0.1f, 100.0f);

        // Load skybox texture (you'll need to implement this method)
        skyboxTexture = loadCubemapTexture(new String[] {
                "right.jpg", "left.jpg",
                "top.jpg", "bottom.jpg",
                "front.jpg", "back.jpg"
        });

        // Create skybox shader program
        shaderHandler.createSkyboxShaderProgram("skybox_vertex.glsl", "skybox_fragment.glsl");

        // Create skybox
        skybox = new Skybox(shaderHandler, skyboxTexture);

        updateProjectionMatrix();
    }

    private int loadCubemapTexture(String[] strings) {
        return 0; //TODO IMPLEMENT
    }

    private void loop() {
        float lastFrame = 0f;

        while (!glfwWindowShouldClose(window)) {
            float currentFrame = (float) glfwGetTime();
            float deltaTime = currentFrame - lastFrame;
            lastFrame = currentFrame;

            Vector3f wishDir = inputHandler.processInput(deltaTime, shaderHandler);

            player.move(wishDir, deltaTime);
            player.update(deltaTime);

            Vector3f playerPosition = player.getPosition();
            cameraHandler.update(playerPosition);

            render();

            if (inputHandler.isDebugMode()) {
                debugOverlay.render();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Update projection matrix (if needed)
        updateProjectionMatrix();

        // Render scene objects
        shaderHandler.useShaderProgram();
        int shaderProgram = shaderHandler.getShaderProgram();

        Matrix4f view = cameraHandler.getViewMatrix(player.getPosition());
        int viewLoc = glGetUniformLocation(shaderProgram, "view");
        glUniformMatrix4fv(viewLoc, false, view.get(new float[16]));

        // Light uniforms setup
        Vector3f lightPos = new Vector3f(5.0f, 5.0f, 5.0f);
        Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);
        Vector3f objectColor = new Vector3f(1.0f, 0.5f, 0.31f);

        shaderHandler.setLightUniforms(
                lightPos,
                cameraHandler.getCameraPos(player.getPosition()),
                lightColor,
                objectColor
        );
        shaderHandler.setFogUniforms(fogColor, fogStart, fogEnd);

        // Render meshes
        meshHandler.renderMeshes(shaderHandler);

        // Render skybox last
        glDepthFunc(GL_LEQUAL);
        shaderHandler.useSkyboxShaderProgram();
        skybox.setFogColor(fogColor);
        skybox.setSkyColor(skyColor);
        skybox.setFogStartEnd(fogStart, fogEnd);
        skybox.render(cameraHandler.getViewMatrix(player.getPosition()), projectionMatrix);
        glDepthFunc(GL_LESS);
    }

    private void updateProjectionMatrix() {
        float aspectRatio = (float) width / height;
        projectionMatrix.identity().perspective((float) Math.toRadians(45.0f),
                aspectRatio, 0.1f, 100.0f);
        int projectionLoc = glGetUniformLocation(shaderHandler.getShaderProgram(), "projection");
        glUniformMatrix4fv(projectionLoc, false, projectionMatrix.get(new float[16]));
    }

    public void run() {
        try {
            init();
            loop();
        } finally {
            // destroy the window
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);

            // terminate glfw
            glfwTerminate();

            // free the error callback
            if (errorCallback != null) {
                errorCallback.free();
            }
        }
    }

    private void runDebugSequence(float deltaTime, Vector3f wishDir) {
        if (debugTimer < DEBUG_STEP_DURATION) {
            wishDir.z = -1;
        } else if (debugTimer < 2 * DEBUG_STEP_DURATION) {
            wishDir.z = 1;
        } else if (debugTimer < 3 * DEBUG_STEP_DURATION) {
            wishDir.x = -1;
        } else if (debugTimer < 4 * DEBUG_STEP_DURATION) {
            wishDir.x = 1;
        } else if (debugTimer < DEBUG_DURATION) {
            player.jump();
        }

        if (wishDir.lengthSquared() > 0) {
            wishDir.normalize();
        }

        System.out.println("debug Step: " + debugStep + ", time: " + debugTimer + ", wishDir: " + wishDir);
    }

    private void loadFontBitmap() {
        try (InputStream is = Engine.class.getResourceAsStream("/font.png")) {
            if (is == null) {
                throw new IOException("Could not find font.png");
            }
            fontImage = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font bitmap", e);
        }
    }

    public static ByteBuffer getFontBitmap(char c) {
        if (fontImage == null) {
            throw new RuntimeException("Font bitmap not loaded");
        }

        int charIndex = (int) c - 32; // Assume space (ASCII 32) is the first character
        if (charIndex < 0 || charIndex >= 96) { // 96 characters in our font
            charIndex = 0; // Default to space for unknown characters
        }

        int row = charIndex / 16;
        int col = charIndex % 16;

        int x = col * CHAR_WIDTH;
        int y = row * CHAR_HEIGHT;

        ByteBuffer buffer = BufferUtils.createByteBuffer(CHAR_WIDTH * CHAR_HEIGHT);

        for (int dy = 0; dy < CHAR_HEIGHT; dy++) {
            for (int dx = 0; dx < CHAR_WIDTH; dx++) {
                int pixel = fontImage.getRGB(x + dx, y + dy);
                int alpha = (pixel >> 24) & 0xFF;
                buffer.put((byte) (alpha > 128 ? 0xFF : 0x00));
            }
        }

        buffer.flip();
        return buffer;
    }

    public static void main(String[] args) {
        new Engine().run();
    }
}