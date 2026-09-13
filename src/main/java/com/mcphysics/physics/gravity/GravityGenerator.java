package com.mcphysics.physics.gravity;

import com.mcphysics.physics.RigidBody;
import com.mcphysics.physics.Vector3D;

public final class GravityGenerator {

    private double gravitationalConstant;
    private Vector3D direction;
    private double strength;

    public GravityGenerator(double strength) {
        this.strength = strength;
        this.direction = new Vector3D(0, -1, 0);
        this.gravitationalConstant = 6.674;
    }

    public void applyGravity(RigidBody body) {
        if (body.isStatic() || !body.isUseGravity() || !body.isAlive()) return;
        Vector3D gravityForce = direction.multiply(strength * body.getMass());
        body.applyForce(gravityForce);
    }

    public void applyGravitationalAttraction(RigidBody body, Vector3D source) {
        if (body.isStatic() || !body.isAlive()) return;
        Vector3D direction = source.subtract(body.getPosition());
        double distanceSq = direction.magnitudeSquared();
        if (distanceSq < 1) return;
        double distance = Math.sqrt(distanceSq);
        Vector3D forceDirection = direction.divide(distance);
        double forceMagnitude = gravitationalConstant * body.getMass() / distanceSq;
        body.applyForce(forceDirection.multiply(forceMagnitude));
    }

    public void applyRadialGravity(RigidBody body, Vector3D center, double radius, double strength) {
        if (body.isStatic() || !body.isAlive()) return;
        double distance = body.getPosition().distanceTo(center);
        if (distance > radius || distance < 0.01) return;
        double factor = 1.0 - (distance / radius);
        Vector3D direction = center.subtract(body.getPosition()).normalize();
        body.applyForce(direction.multiply(strength * factor * body.getMass()));
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public Vector3D getDirection() {
        return direction;
    }

    public void setDirection(Vector3D direction) {
        this.direction = direction.normalize();
    }

    public double getGravitationalConstant() {
        return gravitationalConstant;
    }

    public void setGravitationalConstant(double gravitationalConstant) {
        this.gravitationalConstant = gravitationalConstant;
    }
}
