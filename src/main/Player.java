package main;

import org.joml.Vector3f;

public class Player {
    Vector3f position;
    Vector3f velocity;
    float moveSpeed;
    float sprintSpeed;
    float jumpStrength;
    float gravity;
    boolean isGrounded;
    boolean isSprinting;
    boolean isCheatFlying;
    float airAcceleration;
    float groundAcceleration;
    float friction;
    float brakingDeceleration;

    public Player(Vector3f startPosition) {
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f(0, 0, 0);
        this.moveSpeed = 5.0f;
        this.sprintSpeed = 7.5f;
        this.jumpStrength = 5.0f;
        this.gravity = 9.8f;
        this.isGrounded = false;

        this.isSprinting = false;
        this.isCheatFlying = false;
        this.airAcceleration = 10.0f;
        this.groundAcceleration = 10.0f;
        this.friction = 2.0f;
        this.brakingDeceleration = 10.0f;
    }

    void update(float deltaTime) {
        if (!isCheatFlying) {
            applyGravity(deltaTime);
        }

        applyFriction(deltaTime);

        position.add(new Vector3f(velocity).mul(deltaTime));

        if (position.y <= 0 && !isCheatFlying) {
            position.y = 0;
            isGrounded = true;
            velocity.y = 0;
        } else {
            isGrounded = false;
        }
    }

    void applyGravity(float deltaTime) {
        velocity.y -= gravity * deltaTime;
    }

    void applyFriction(float deltaTime) {
        if (isGrounded) {
            float speed = velocity.length();
            if (speed > 0) {
                float drop = speed * friction * deltaTime;
                float newSpeed = Math.max(0, speed - drop);
                velocity.mul(newSpeed / speed);
            }
        }
    }

    public void jump() {
        if (isGrounded) {
            velocity.y = jumpStrength;
        }
    }

    void move(Vector3f direction, float deltaTime) {
        float acceleration = isGrounded ? groundAcceleration : airAcceleration;
        float maxSpeed = isSprinting ? sprintSpeed : moveSpeed;

        if (isCheatFlying) {
            maxSpeed *= 1.5f;
        }

        // Normalize the direction vector
        direction.normalize();

        // Only affect horizontal movement (x and z)
        Vector3f horizontalVelocity = new Vector3f(velocity.x, 0, velocity.z);

        // Calculate the current speed in the movement direction
        float currentSpeed = horizontalVelocity.dot(direction);

        // Calculate the amount of acceleration to apply
        float accelerationThisFrame = acceleration * deltaTime;
        float newSpeed = Math.min(currentSpeed + accelerationThisFrame, maxSpeed);

        // Apply the new speed in the movement direction
        horizontalVelocity.add(new Vector3f(direction).mul(newSpeed - currentSpeed));

        // Update the main velocity vector
        velocity.x = horizontalVelocity.x;
        velocity.z = horizontalVelocity.z;

        if (!isCheatFlying) {
            float speedLimit = 20.0f; // Adjust this value as needed
            float horizontalSpeedSquared = velocity.x * velocity.x + velocity.z * velocity.z;
            if (horizontalSpeedSquared > speedLimit * speedLimit) {
                float scale = speedLimit / (float)Math.sqrt(horizontalSpeedSquared);
                velocity.x *= scale;
                velocity.z *= scale;
            }
        }
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