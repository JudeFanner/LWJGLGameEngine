package main;

import org.joml.Vector3f;
import org.joml.Matrix3f;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Player {
    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

    // Constants
    private static final float DEFAULT_MOVE_SPEED = 7.0f;
    private static final float DEFAULT_SPRINT_SPEED = 7.5f;
    private static final float DEFAULT_JUMP_STRENGTH = 5.0f;
    private static final float DEFAULT_GRAVITY = 9.8f;
    private static final float DEFAULT_AIR_ACCELERATION = 10.0f; // Increased for quicker air control
    private static final float DEFAULT_GROUND_ACCELERATION = 10.0f;
    private static final float DEFAULT_FRICTION = 4.0f;
    private static final float CHEAT_FLY_SPEED_MULTIPLIER = 1.5f;
    private static final float SPEED_LIMIT = 20.0f;
    private static final float GROUND_LEVEL = 0f;
    private static final float AIR_SPEED_CAP = 30.0f;
    private static final float STOP_SPEED = 0.4f;
    private static final float MAX_VELOCITY = 50.0f;
    private static final float EPSILON = 0.001f;

    // Fields
    private Vector3f position;
    private Vector3f velocity;
    private Vector3f lastWishDir;
    private float moveSpeed;
    private float sprintSpeed;
    private float jumpStrength;
    private float gravity;
    private boolean isGrounded;
    private boolean isSprinting;
    private boolean isCheatFlying;
    private float airAcceleration;
    private float groundAcceleration;
    private float friction;

    public Player(Vector3f startPosition) {
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f(0, 0, 0);
        this.lastWishDir = new Vector3f(0, 0, 0);
        this.moveSpeed = DEFAULT_MOVE_SPEED;
        this.sprintSpeed = DEFAULT_SPRINT_SPEED;
        this.jumpStrength = DEFAULT_JUMP_STRENGTH;
        this.gravity = DEFAULT_GRAVITY;
        this.isGrounded = false;
        this.isSprinting = false;
        this.isCheatFlying = false;
        this.airAcceleration = DEFAULT_AIR_ACCELERATION;
        this.groundAcceleration = DEFAULT_GROUND_ACCELERATION;
        this.friction = DEFAULT_FRICTION;

        LOGGER.log(Level.INFO, "Player initialized at position: {0}", this.position);
    }

    public void update(float deltaTime) {
        if (!isValidFloat(deltaTime) || deltaTime <= 0) {
            LOGGER.log(Level.WARNING, "Invalid delta time: {0}. Skipping update.", deltaTime);
            return;
        }

        // Apply gravity
        if (!isCheatFlying) {
            applyGravity(deltaTime);
        }

        updatePosition(deltaTime);
        updateGroundedState();

        LOGGER.log(Level.FINE, "Update - Position: {0}, Velocity: {1}, Grounded: {2}",
                new Object[]{position, velocity, isGrounded});
    }

    private void accelerate(Vector3f wishDir, float wishSpeed, float accel, float deltaTime) {
        float currentSpeed = velocity.dot(wishDir);
        float addSpeed = wishSpeed - currentSpeed;

        if (addSpeed <= 0) {
            return;
        }

        float accelSpeed = Math.min(addSpeed, accel * deltaTime);

        Vector3f accelDir = new Vector3f(wishDir).mul(accelSpeed);
        velocity.add(accelDir);
    }

    public void move(Vector3f wishDir, float deltaTime) {
        if (!isValidFloat(deltaTime)) {
            LOGGER.log(Level.WARNING, "Invalid delta time: {0}. Skipping move.", deltaTime);
            return;
        }

        lastWishDir.set(wishDir);

        // Apply friction
        if (isGrounded) {
            applyFriction(deltaTime);
        }

        // Normalize wishDir if it's not zero
        if (wishDir.lengthSquared() > EPSILON) {
            wishDir.y = 0; // Ensure no vertical component
            wishDir.normalize();
        }

        float wishSpeed = isSprinting ? sprintSpeed : moveSpeed;

        if (isCheatFlying) {
            wishSpeed *= CHEAT_FLY_SPEED_MULTIPLIER;
        }

        // Always call accelerate, even if wishDir is zero
        accelerate(wishDir, wishSpeed, isGrounded ? groundAcceleration : airAcceleration, deltaTime);

        limitVelocity(MAX_VELOCITY);

        LOGGER.log(Level.FINE, "Move input - WishDir: {0}, Grounded: {1}, Velocity before: {2}, Velocity after: {3}",
                new Object[]{wishDir, isGrounded, new Vector3f(velocity), velocity});
    }

    private void applyGravity(float deltaTime) {
        if (!isGrounded && !isCheatFlying) {
            float gravityEffect = gravity * deltaTime;
            if (isValidFloat(gravityEffect)) {
                velocity.y -= gravityEffect;
            } else {
                LOGGER.log(Level.WARNING, "Invalid gravity effect calculated: {0}", gravityEffect);
            }
        }
    }

    private void updatePosition(float deltaTime) {
        if (!velocity.isFinite()) {
            LOGGER.log(Level.SEVERE, "Invalid velocity detected: {0}. Resetting to zero.", velocity);
            velocity.zero();
            return;
        }

        Vector3f movement = new Vector3f(velocity).mul(deltaTime);
        if (movement.isFinite()) {
            position.add(movement);
        } else {
            LOGGER.log(Level.SEVERE, "Invalid movement calculated. Velocity: {0}, Delta time: {1}",
                    new Object[]{velocity, deltaTime});
            // Instead of zeroing velocity, we'll only zero the problematic components
            if (Float.isNaN(velocity.x) || Float.isInfinite(velocity.x)) velocity.x = 0;
            if (Float.isNaN(velocity.y) || Float.isInfinite(velocity.y)) velocity.y = 0;
            if (Float.isNaN(velocity.z) || Float.isInfinite(velocity.z)) velocity.z = 0;
        }

        // Ensure the player doesn't fall through the ground
        if (position.y < GROUND_LEVEL) {
            position.y = GROUND_LEVEL;
            isGrounded = true;
            velocity.y = 0;
        }
    }

    private void applyFriction(float deltaTime) {
        float speed = velocity.length();

        if (speed < EPSILON) {
            return;
        }

        float drop = 0;

        // Apply ground friction
        float control = Math.max(speed, STOP_SPEED);
        drop += control * friction * deltaTime;

        // Scale the velocity
        float newSpeed = Math.max(0, speed - drop);
        if (newSpeed != speed) {
            newSpeed /= speed;
            velocity.mul(newSpeed);
        }
    }

    private void updateGroundedState() {
        boolean wasGrounded = isGrounded;
        isGrounded = position.y <= GROUND_LEVEL && !isCheatFlying;

        if (isGrounded) {
            position.y = GROUND_LEVEL;
            if (velocity.y < 0) {
                velocity.y = 0;
            }
        }

        if (isGrounded && !wasGrounded) {
            LOGGER.log(Level.FINE, "Player landed");
        } else if (!isGrounded && wasGrounded) {
            LOGGER.log(Level.FINE, "Player left ground");
        }
    }

    private void landingImpact() {
        //TODO implement impact events
    }

    private void limitVelocity(float speedLimit) {
        float horizontalSpeedSquared = velocity.x * velocity.x + velocity.z * velocity.z;
        if (horizontalSpeedSquared > speedLimit * speedLimit) {
            float scale = speedLimit / (float)Math.sqrt(horizontalSpeedSquared);
            velocity.x *= scale;
            velocity.z *= scale;
        }
    }

    public void jump() {
        if (isGrounded) {
            velocity.y = jumpStrength;
            isGrounded = false;
        }
    }

    public void toggleSprint() {
        isSprinting = !isSprinting;
    }

    public void toggleCheatFlying() {
        isCheatFlying = !isCheatFlying;
        if (isCheatFlying) {
            velocity.y = 0;
        }
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public void setPosition(Vector3f newPosition) {
        if (isValidVector(newPosition)) {
            this.position.set(newPosition);
        } else {
            LOGGER.log(Level.SEVERE, "Attempted to set invalid position: {0}", newPosition);
        }
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public void setVelocity(Vector3f newVelocity) {
        if (isValidVector(newVelocity)) {
            this.velocity.set(newVelocity);
        } else {
            LOGGER.log(Level.SEVERE, "Attempted to set invalid velocity: {0}", newVelocity);
        }
    }

    private boolean isValidFloat(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private boolean isValidVector(Vector3f vector) {
        return vector != null && vector.isFinite();
    }

    public void setSprinting(boolean sprinting) {
        isSprinting = sprinting;
    }
}