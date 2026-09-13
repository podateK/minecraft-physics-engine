package com.mcphysics.physics.collision;

import com.mcphysics.physics.Vector3D;

public final class SphereCollider {

    private Vector3D center;
    private double radius;

    public SphereCollider(Vector3D center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public boolean intersects(SphereCollider other) {
        double distSq = center.distanceSquaredTo(other.center);
        double radiusSum = radius + other.radius;
        return distSq <= radiusSum * radiusSum;
    }

    public boolean containsPoint(Vector3D point) {
        return center.distanceSquaredTo(point) <= radius * radius;
    }

    public Vector3D getClosestPoint(Vector3D point) {
        Vector3D toPoint = point.subtract(center);
        double dist = toPoint.magnitude();
        if (dist < 1e-10) return center.add(new Vector3D(radius, 0, 0));
        return center.add(toPoint.divide(dist).multiply(radius));
    }

    public SphereCollisionResult testCollision(SphereCollider other) {
        Vector3D diff = other.center.subtract(center);
        double dist = diff.magnitude();
        double penetration = (radius + other.radius) - dist;
        if (penetration <= 0 || dist < 1e-10) return null;
        Vector3D normal = diff.divide(dist);
        Vector3D contactPoint = center.add(normal.multiply(radius));
        return new SphereCollisionResult(normal, penetration, contactPoint);
    }

    public Vector3D getCenter() {
        return center;
    }

    public void setCenter(Vector3D center) {
        this.center = center;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public record SphereCollisionResult(Vector3D normal, double penetrationDepth, Vector3D contactPoint) {
    }
}
