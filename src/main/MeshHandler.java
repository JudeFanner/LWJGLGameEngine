package main;

import org.joml.Matrix4f;
import org.lwjgl.assimp.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.*;

public class MeshHandler {
    private List<MeshRenderer> meshRenderers;

    public MeshHandler() {
        meshRenderers = new ArrayList<>();
    }

    public void loadMeshes() {
        File modelsDir = new File("models");
        if (modelsDir.exists() && modelsDir.isDirectory()) {
            for (File file : modelsDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".obj")) {
                    try {
                        List<Mesh> loadedMeshes = loadModel(file.getName());
                        for (Mesh mesh : loadedMeshes) {
                            meshRenderers.add(new MeshRenderer(mesh));
                        }
                        //System.out.println("loaded: " + file.getName());
                    } catch (Exception e) {
                        System.err.println("error loading " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    public void addGroundPlane() {
        float[] groundVertices = {
                -50, 0, -50,
                50, 0, -50,
                50, 0, 50,
                -50, 0, 50
        };
        float[] groundTexCoords = {
                0, 0,
                1, 0,
                1, 1,
                0, 1
        };
        float[] groundNormals = {
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0
        };
        int[] groundIndices = {
                0, 1, 2,
                2, 3, 0
        };
        Mesh groundPlane = new Mesh(groundVertices, groundTexCoords, groundNormals, groundIndices);
        meshRenderers.add(new MeshRenderer(groundPlane));
    }

    private List<Mesh> loadModel(String fileName) {
        AIScene scene = Assimp.aiImportFile("models/" + fileName,
                Assimp.aiProcess_Triangulate | Assimp.aiProcess_FlipUVs);

        if (scene == null || scene.mNumMeshes() < 1) {
            throw new RuntimeException("error loading model: " + Assimp.aiGetErrorString());
        }

        List<Mesh> meshes = new ArrayList<>();

        for (int i = 0; i < scene.mNumMeshes(); i++) {
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(i));
            Mesh mesh = processMesh(aiMesh, scene);
            meshes.add(mesh);
        }

        Assimp.aiReleaseImport(scene);

        return meshes;
    }

    private Mesh processMesh(AIMesh aiMesh, AIScene scene) {
        List<Float> vertices = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < aiMesh.mNumVertices(); i++) {
            AIVector3D vertex = aiMesh.mVertices().get(i);
            vertices.add(vertex.x());
            vertices.add(vertex.y());
            vertices.add(vertex.z());

            if (aiMesh.mTextureCoords(0) != null) {
                AIVector3D texCoord = aiMesh.mTextureCoords(0).get(i);
                texCoords.add(texCoord.x());
                texCoords.add(texCoord.y());
            } else {
                texCoords.add(0.0f);
                texCoords.add(0.0f);
            }

            if (aiMesh.mNormals() != null) {
                AIVector3D normal = aiMesh.mNormals().get(i);
                normals.add(normal.x());
                normals.add(normal.y());
                normals.add(normal.z());
            }
        }

        for (int i = 0; i < aiMesh.mNumFaces(); i++) {
            AIFace face = aiMesh.mFaces().get(i);
            for (int j = 0; j < face.mNumIndices(); j++) {
                indices.add(face.mIndices().get(j));
            }
        }

        return new Mesh(listToArray(vertices), listToArray(texCoords), listToArray(normals), listToIntArray(indices));
    }

    private float[] listToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private int[] listToIntArray(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    public void renderMeshes(ShaderHandler shaderHandler) {
        int shaderProgram = shaderHandler.getShaderProgram();
        for (MeshRenderer renderer : meshRenderers) {
            Matrix4f modelMatrix = renderer.getModelMatrix();
            shaderHandler.setUniform("model", modelMatrix);

            //System.out.println("Rendering mesh with model matrix: " +
            // modelMatrix);
            renderer.render(shaderProgram);
        }
    }

    public void setMeshEnabled(int index, boolean enabled) {
        if (index >= 0 && index < meshRenderers.size()) {
            meshRenderers.get(index).setEnabled(enabled);
        }
    }

    public void setMeshModelMatrix(int index, Matrix4f modelMatrix) {
        if (index >= 0 && index < meshRenderers.size()) {
            meshRenderers.get(index).setModelMatrix(modelMatrix);
        }
    }

    public int getMeshCount() {
        return meshRenderers.size();
    }

    public void updateMeshTransformations(float deltaTime) {
        for (MeshRenderer renderer : meshRenderers) {
            renderer.update(deltaTime);
        }
    }
}
