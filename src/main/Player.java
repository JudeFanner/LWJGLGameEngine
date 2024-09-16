package main;

import org.joml.Vector3f;

public class Player {
    private Vector3f position;
    private Vector3f velocity;
    private float moveSpeed;
    private float jumpStrength;
    private float gravity;
    private boolean isGrounded;

    public Player(Vector3f startPosition) {
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f(0, 0, 0);
        this.moveSpeed = 5.0f;
        this.jumpStrength = 5.0f;
        this.gravity = 9.8f;
        this.isGrounded = false;
    }

    public void update(float deltaTime) {
        velocity.y -= gravity * deltaTime;
        position.add(new Vector3f(velocity).mul(deltaTime));

        if (position.y <= 0) {
            position.y = 0;
            isGrounded = true;
            velocity.y = 0;
        } else {
            isGrounded = false;
        }

//        System.out.println("Player update - Position: " + position + ", " +
//                "Velocity: " + velocity + ", Grounded: " + isGrounded);
    }

    public void jump() {
        if (isGrounded) {
            velocity.y = jumpStrength;
            //System.out.println("Player jumped - New velocity: " + velocity);
        }
    }

    public void move(Vector3f direction) {
        Vector3f movement = new Vector3f(direction).normalize().mul(moveSpeed);
        velocity.x = movement.x;
        velocity.z = movement.z;
        //System.out.println("Player move - Direction: " + direction + ", New
        // velocity: " + velocity);
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
    }

    public void setVelocity(Vector3f newVelocity) {
        this.velocity.set(newVelocity);
    }
}