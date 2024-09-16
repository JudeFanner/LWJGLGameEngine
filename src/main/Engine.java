package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.NULL;


public class Engine {
    private long window;
    private int width = 800;
    private int height = 600;

    private Player player;
    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;
    private MeshHandler meshHandler;

    private GLFWErrorCallback errorCallback;

    private ShaderHandler shaderHandler;
    private InputHandler inputHandler;
    private CameraHandler cameraHandler;

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
        window = glfwCreateWindow(800, 600, "lwjgl game", NULL, NULL);
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

        // this line is critical for lwjgl's interoperation with glfw's
        // opengl context, or any context that is managed externally.
        GL.createCapabilities();

        // set the clear color
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);

        // enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        // initialize handlers
        shaderHandler = new ShaderHandler();
        player = new Player(new Vector3f(0, 1, 0));
        player.setVelocity(new Vector3f(0.1f, 0, 0.1f)); // Set a small initial velocity

        cameraHandler = new CameraHandler();
        inputHandler = new InputHandler(window, player, cameraHandler);

        //System.out.println("Player position: " + player.getPosition());
        //System.out.println("Camera position: " + cameraHandler.getCameraPos(player.getPosition()));

        meshHandler = new MeshHandler();
        meshHandler.loadMeshes();
        meshHandler.addGroundPlane();

        //System.out.println("number of meshes loaded: " + meshHandler.getMeshCount());

        // set up projection matrix

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f),
                (float) width / (float) height, 0.1f, 100.0f);

        // upload the projection matrix to the shader
        int projectionLoc = glGetUniformLocation(shaderHandler.getShaderProgram(), "projection");
        glUniformMatrix4fv(projectionLoc, false, projection.get(new float[16]));

        updateProjectionMatrix();
    }

    private void loop() {
        float lastFrame = 0f;

        while (!glfwWindowShouldClose(window)) {
            float currentFrame = (float) glfwGetTime();
            float deltaTime = currentFrame - lastFrame;
            lastFrame = currentFrame;

            if (debugMode) {
                runDebugSequence(deltaTime);
            } else {
                // Normal input processing
                inputHandler.processInput();
            }

            // Update game state
            player.update(deltaTime);

            // Update camera
            Vector3f playerPosition = player.getPosition();
            cameraHandler.update(playerPosition);

            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            if (debugMode && debugTimer >= DEBUG_DURATION) {
                glfwSetWindowShouldClose(window, true);
            }
        }
    }

    private boolean debugMode = false;
    private float debugTimer = 0f;
    private int debugStep = 0;
    private final float DEBUG_DURATION = 2.0f;

    private void runDebugSequence(float deltaTime) {
        debugTimer += deltaTime;

        int step = (int) (debugTimer / 0.5f);
        if (step != debugStep) {
            debugStep = step;
            Vector3f moveDirection = new Vector3f();

            switch (debugStep % 5) {
                case 0: moveDirection.set(1, 0, 0); break;  // Move right
                case 1: moveDirection.set(-1, 0, 0); break; // Move left
                case 2: moveDirection.set(0, 0, -1); break; // Move forward
                case 3: moveDirection.set(0, 0, 1); break;  // Move backward
                case 4: player.jump(); break;               // Jump
            }

            if (debugStep % 5 != 4) { // If not jumping
                player.move(moveDirection);
            }

            //System.out.println("Debug step " + debugStep + ": " +
            // (debugStep % 5 == 4 ? "Jump" : "Move " + moveDirection));
        }

        //System.out.println("Debug timer: " + debugTimer);
        //System.out.println("Player position: " + player.getPosition());
        //System.out.println("Player velocity: " + player.getVelocity());
        //System.out.println("Camera position: " + cameraHandler.getCameraPos(player.getPosition()));
        //System.out.println("Camera front: " + cameraHandler.getCameraFront());
        //System.out.println("---");
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shaderHandler.useShaderProgram();
        int shaderProgram = shaderHandler.getShaderProgram();

        Matrix4f view = cameraHandler.getViewMatrix(player.getPosition());
        int viewLoc = glGetUniformLocation(shaderProgram, "view");
        glUniformMatrix4fv(viewLoc, false, view.get(new float[16]));

        //System.out.println("View Matrix: " + view);

        // update projection matrix
        updateProjectionMatrix();

        // light uniforms setup
        Vector3f lightPos = new Vector3f(5.0f, 5.0f, 5.0f);
        Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);
        Vector3f objectColor = new Vector3f(1.0f, 0.5f, 0.31f);
        shaderHandler.setLightUniforms(
                lightPos,
                cameraHandler.getCameraPos(player.getPosition()),
                lightColor,
                objectColor
        );

        // Render meshes
        meshHandler.renderMeshes(shaderHandler);
    }

    private void updateProjectionMatrix() {
        float aspect = (float) width / height;
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f),
                aspect, 0.1f, 100.0f);
        int projectionLoc = glGetUniformLocation(shaderHandler.getShaderProgram(), "projection");
        glUniformMatrix4fv(projectionLoc, false, projection.get(new float[16]));

        //System.out.println("Aspect Ratio: " + aspect);
    }

    Vector3f lightPos = new Vector3f(5.0f, 5.0f, 5.0f);
    Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);

    Vector3f objectColor = new Vector3f(0.5f, 0.5f, 0.5f);

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

    public static void main(String[] args) {
        new Engine().run();
    }
}