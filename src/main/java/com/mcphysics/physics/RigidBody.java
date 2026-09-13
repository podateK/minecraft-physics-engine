package com.mcphysics.physics;

import com.mcphysics.physics.collision.SphereCollider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.UUID;

public final class RigidBody {

    private final UUID id;
    private Vector3D position;
    private Vector3D velocity;
    private Vector3D angularVelocity;
    private Vector3D acceleration;
    private double mass;
    private double inverseMass;
    private double restitution;
    private double friction;
    private boolean isStatic;
    private boolean isKinematic;
    private boolean useGravity;
    private boolean isAlive;
    private double linearDamping;
    private double angularDamping;
    private SphereCollider collider;
    private Object attachedEntity;
    private World world;

    public RigidBody(Vector3D position, double mass) {
        this.id = UUID.randomUUID();
        this.position = position;
        this.velocity = Vector3D.ZERO;
        this.angularVelocity = Vector3D.ZERO;
        this.acceleration = Vector3D.ZERO;
        this.mass = mass;
        this.inverseMass = mass > 0 ? 1.0 / mass : 0.0;
        this.restitution = 0.3;
        this.friction = 0.5;
        this.isStatic = mass <= 0;
        this.isKinematic = false;
        this.useGravity = !isStatic;
        this.isAlive = true;
        this.linearDamping = 0.01;
        this.angularDamping = 0.05;
    }

    public void applyForce(Vector3D force) {
        if (isStatic || !isAlive) return;
        velocity = velocity.add(force.divide(mass));
    }

    public void applyImpulse(Vector3D impulse) {
        if (isStatic || !isAlive) return;
        velocity = velocity.add(impulse.multiply(inverseMass));
    }

    public void applyImpulseAtPoint(Vector3D impulse, Vector3D contactPoint) {
        if (isStatic || !isAlive) return;
        velocity = velocity.add(impulse.multiply(inverseMass));
        Vector3D r = contactPoint.subtract(position);
        angularVelocity = angularVelocity.add(r.cross(impulse).multiply(inverseMass));
    }

    public void integrate(double dt) {
        if (isStatic || isKinematic || !isAlive) return;

        velocity = velocity.add(acceleration.multiply(dt));
        velocity = velocity.multiply(1.0 - linearDamping * dt);
        position = position.add(velocity.multiply(dt));

        angularVelocity = angularVelocity.multiply(1.0 - angularDamping * dt);
        acceleration = Vector3D.ZERO;
    }

    public void syncWithEntity() {
        if (attachedEntity instanceof Entity entity && entity.isValid()) {
            entity.teleport(position.toBukkit().toLocation(
                    world != null ? world : entity.getWorld()
            ));
            entity.setVelocity(velocity.toBukkit());
        }
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        if (isStatic) {
            this.inverseMass = 0;
            this.velocity = Vector3D.ZERO;
            this.angularVelocity = Vector3D.ZERO;
        } else {
            this.inverseMass = mass > 0 ? 1.0 / mass : 0;
        }
    }

    public void destroy() {
        this.isAlive = false;
        if (attachedEntity instanceof Entity entity && entity.isValid()) {
            entity.remove();
        }
    }

    public UUID getId() {
        return id;
    }

    public Vector3D getPosition() {
        return position;
    }

    public void setPosition(Vector3D position) {
        this.position = position;
    }

    public Vector3D getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3D velocity) {
        this.velocity = velocity;
    }

    public Vector3D getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(Vector3D angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public Vector3D getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(Vector3D acceleration) {
        this.acceleration = acceleration;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
        this.inverseMass = mass > 0 ? 1.0 / mass : 0;
        this.isStatic = mass <= 0;
    }

    public double getInverseMass() {
        return inverseMass;
    }

    public double getRestitution() {
        return restitution;
    }

    public void setRestitution(double restitution) {
        this.restitution = Math.max(0, Math.min(1, restitution));
    }

    public double getFriction() {
        return friction;
    }

    public void setFriction(double friction) {
        this.friction = Math.max(0, Math.min(1, friction));
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isKinematic() {
        return isKinematic;
    }

    public void setKinematic(boolean kinematic) {
        this.isKinematic = kinematic;
    }

    public boolean isUseGravity() {
        return useGravity;
    }

    public void setUseGravity(boolean useGravity) {
        this.useGravity = useGravity;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public double getLinearDamping() {
        return linearDamping;
    }

    public void setLinearDamping(double linearDamping) {
        this.linearDamping = linearDamping;
    }

    public double getAngularDamping() {
        return angularDamping;
    }

    public void setAngularDamping(double angularDamping) {
        this.angularDamping = angularDamping;
    }

    public SphereCollider getCollider() {
        return collider;
    }

    public void setCollider(SphereCollider collider) {
        this.collider = collider;
    }

    public Object getAttachedEntity() {
        return attachedEntity;
    }

    public void setAttachedEntity(Object attachedEntity) {
        this.attachedEntity = attachedEntity;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Location toLocation(World world) {
        return position.toBukkit().toLocation(world);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RigidBody other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
