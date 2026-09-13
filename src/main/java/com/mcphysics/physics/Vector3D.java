package com.mcphysics.physics;

import org.bukkit.util.Vector;

public final class Vector3D {

    public static final Vector3D ZERO = new Vector3D(0, 0, 0);
    public static final Vector3D UP = new Vector3D(0, 1, 0);
    public static final Vector3D GRAVITY = new Vector3D(0, -9.81, 0);

    private final double x;
    private final double y;
    private final double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D(Vector vector) {
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector3D add(Vector3D other) {
        return new Vector3D(x + other.x, y + other.y, z + other.z);
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(x - other.x, y - other.y, z - other.z);
    }

    public Vector3D multiply(double scalar) {
        return new Vector3D(x * scalar, y * scalar, z * scalar);
    }

    public Vector3D divide(double scalar) {
        if (Math.abs(scalar) < 1e-10) {
            return ZERO;
        }
        return new Vector3D(x / scalar, y / scalar, z / scalar);
    }

    public double dot(Vector3D other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3D cross(Vector3D other) {
        return new Vector3D(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double magnitudeSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3D normalize() {
        double mag = magnitude();
        if (mag < 1e-10) {
            return ZERO;
        }
        return divide(mag);
    }

    public Vector3D lerp(Vector3D target, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Vector3D(
                x + (target.x - x) * t,
                y + (target.y - y) * t,
                z + (target.z - z) * t
        );
    }

    public Vector3D reflect(Vector3D normal) {
        double dotProduct = dot(normal) * 2;
        return subtract(normal.multiply(dotProduct));
    }

    public Vector3D abs() {
        return new Vector3D(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public double distanceTo(Vector3D other) {
        return subtract(other).magnitude();
    }

    public double distanceSquaredTo(Vector3D other) {
        return subtract(other).magnitudeSquared();
    }

    public Vector3D clamp(double maxLength) {
        double mag = magnitude();
        if (mag <= maxLength || mag < 1e-10) {
            return this;
        }
        return normalize().multiply(maxLength);
    }

    public Vector toBukkit() {
        return new Vector(x, y, z);
    }

    public static Vector3D fromBukkit(Vector vector) {
        return new Vector3D(vector);
    }

    public static Vector3D min(Vector3D a, Vector3D b) {
        return new Vector3D(
                Math.min(a.x, b.x),
                Math.min(a.y, b.y),
                Math.min(a.z, b.z)
        );
    }

    public static Vector3D max(Vector3D a, Vector3D b) {
        return new Vector3D(
                Math.max(a.x, b.x),
                Math.max(a.y, b.y),
                Math.max(a.z, b.z)
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector3D other)) return false;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        long h = Double.doubleToLongBits(x);
        h = 31 * h + Double.doubleToLongBits(y);
        h = 31 * h + Double.doubleToLongBits(z);
        return (int) (h ^ (h >>> 32));
    }

    @Override
    public String toString() {
        return String.format("Vector3D(%.3f, %.3f, %.3f)", x, y, z);
    }
}
