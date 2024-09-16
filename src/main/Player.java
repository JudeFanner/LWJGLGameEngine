package main;

import org.joml.Vector3f;
import org.joml.Matrix3f;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Player {
    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

    // Constants
    private static final float DEFAULT_MOVE_SPEED = 5.0f;
    private static final float DEFAULT_SPRINT_SPEED = 7.5f;
    private static final float DEFAULT_JUMP_STRENGTH = 5.0f;
    private static final float DEFAULT_GRAVITY = 9.8f;
    private static final float DEFAULT_AIR_ACCELERATION = 1.0f;
    private static final float DEFAULT_GROUND_ACCELERATION = 10.0f;
    private static final float DEFAULT_FRICTION = 6.0f;
    private static final float CHEAT_FLY_SPEED_MULTIPLIER = 1.5f;
    private static final float SPEED_LIMIT = 20.0f;
    private static final float GROUND_LEVEL = 0f;
    private static final float EPSILON = 1e-6f;

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

        if (!isCheatFlying) {
            applyGravity(deltaTime);
        }

        if (isGrounded) {
            applyFriction(deltaTime);
        }

        updatePosition(deltaTime);

        // Update grounded state
        updateGroundedState();

        LOGGER.log(Level.FINE, "Position after update: {0}, Velocity: {1}", new Object[]{position, velocity});
    }

    public void move(Vector3f wishDir, float deltaTime) {
        if (!isValidFloat(deltaTime)) {
            LOGGER.log(Level.WARNING, "Invalid delta time: {0}. Skipping move.", deltaTime);
            return;
        }

        lastWishDir.set(wishDir);

        // If wishDir is zero, we only need to apply gravity and friction
        if (wishDir.lengthSquared() < EPSILON) {
            if (!isCheatFlying) {
                applyGravity(deltaTime);
            }
            if (isGrounded) {
                applyFriction(deltaTime);
            }
            return;
        }

        // wishDir is already in world space, no need to transform
        wishDir.y = 0; // Ensure no vertical component
        wishDir.normalize();

        float wishSpeed = isSprinting ? sprintSpeed : moveSpeed;

        if (isCheatFlying) {
            wishSpeed *= CHEAT_FLY_SPEED_MULTIPLIER;
        }

        if (isGrounded) {
            accelerate(wishDir, wishSpeed, groundAcceleration, deltaTime);
        } else {
            airAccelerate(wishDir, wishSpeed, airAcceleration, deltaTime);
        }

        limitVelocity(wishSpeed * CHEAT_FLY_SPEED_MULTIPLIER);

        LOGGER.log(Level.FINE, "Velocity after move: {0}", velocity);
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
        float speed = new Vector3f(velocity.x, 0, velocity.z).length();
        if (speed > EPSILON) {
            float drop = speed * friction * deltaTime;
            float newSpeed = Math.max(0, speed - drop);
            if (newSpeed > 0) {
                newSpeed /= speed;
                velocity.x *= newSpeed;
                velocity.z *= newSpeed;
            } else {
                velocity.x = 0;
                velocity.z = 0;
            }
        }
    }

    private void updateGroundedState() {
        if (position.y <= GROUND_LEVEL && !isCheatFlying) {
            position.y = GROUND_LEVEL;
            isGrounded = true;
            velocity.y = 0;
        } else {
            isGrounded = false;
        }
    }

    private void accelerate(Vector3f wishDir, float wishSpeed, float accel, float deltaTime) {
        float currentSpeed = new Vector3f(velocity.x, 0, velocity.z).dot(wishDir);
        float addSpeed = wishSpeed - currentSpeed;

        if (addSpeed <= 0) {
            return;
        }

        float accelSpeed = Math.min(accel * deltaTime * wishSpeed, addSpeed);

        velocity.x += wishDir.x * accelSpeed;
        velocity.z += wishDir.z * accelSpeed;
    }

    private void airAccelerate(Vector3f wishDir, float wishSpeed, float accel, float deltaTime) {
        float wishSpd = Math.min(wishSpeed, SPEED_LIMIT);

        Vector3f horizontalVelocity = new Vector3f(velocity.x, 0, velocity.z);
        float currentSpeed = horizontalVelocity.dot(wishDir);
        float addSpeed = wishSpd - currentSpeed;

        if (addSpeed <= 0) {
            return;
        }

        float accelSpeed = Math.min(addSpeed, accel * wishSpd * deltaTime);

        velocity.x += wishDir.x * accelSpeed;
        velocity.z += wishDir.z * accelSpeed;
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
        if (isGrounded || isCheatFlying) {
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