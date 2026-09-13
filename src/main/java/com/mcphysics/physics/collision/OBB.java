package com.mcphysics.physics.collision;

import com.mcphysics.physics.Vector3D;

public final class OBB {

    private Vector3D center;
    private Vector3D halfExtents;
    private Vector3D[] axes;

    public OBB(Vector3D center, Vector3D halfExtents, Vector3D[] axes) {
        this.center = center;
        this.halfExtents = halfExtents;
        this.axes = axes;
    }

    public static OBB fromAABB(Vector3D min, Vector3D max) {
        Vector3D center = min.add(max).multiply(0.5);
        Vector3D halfExtents = max.subtract(min).multiply(0.5);
        Vector3D[] axes = {Vector3D.UP, new Vector3D(1, 0, 0), new Vector3D(0, 0, 1)};
        return new OBB(center, halfExtents, axes);
    }

    public boolean intersects(OBB other) {
        Vector3D[] testAxes = new Vector3D[15];
        System.arraycopy(axes, 0, testAxes, 0, 3);
        System.arraycopy(other.axes, 0, testAxes, 3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 3; j < 6; j++) {
                testAxes[6 + (i * 3) + (j - 3)] = axes[i].cross(other.axes[j - 3]);
            }
        }
        for (Vector3D axis : testAxes) {
            if (axis.magnitudeSquared() < 1e-10) continue;
            axis = axis.normalize();
            if (separatingAxis(axis, this, other)) {
                return false;
            }
        }
        return true;
    }

    public boolean intersectsSphere(SphereCollider sphere) {
        Vector3D closest = closestPoint(sphere.getCenter());
        Vector3D diff = sphere.getCenter().subtract(closest);
        return diff.magnitudeSquared() <= sphere.getRadius() * sphere.getRadius();
    }

    public Vector3D closestPoint(Vector3D point) {
        Vector3D local = point.subtract(center);
        Vector3D result = center;
        for (int i = 0; i < 3; i++) {
            double projection = local.dot(axes[i]);
            double clamped = Math.max(-halfExtents.getComponent(i), Math.min(halfExtents.getComponent(i), projection));
            result = result.add(axes[i].multiply(clamped));
        }
        return result;
    }

    public CollisionManifold testCollision(OBB other) {
        if (!intersects(other)) return null;
        Vector3D penetration = computePenetration(this, other);
        if (penetration.magnitudeSquared() < 1e-10) return null;
        Vector3D contactPoint = closestPoint(other.center).add(other.closestPoint(center)).multiply(0.5);
        return new CollisionManifold(contactPoint, penetration.normalize(), penetration.magnitude());
    }

    private static boolean separatingAxis(Vector3D axis, OBB a, OBB b) {
        double ra = projectOBB(axis, a);
        double rb = projectOBB(axis, b);
        Vector3D d = b.center.subtract(a.center);
        double distance = Math.abs(d.dot(axis));
        return distance > ra + rb;
    }

    private static double projectOBB(Vector3D axis, OBB obb) {
        double sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += Math.abs(obb.axes[i].dot(axis)) * obb.halfExtents.getComponent(i);
        }
        return sum;
    }

    private static Vector3D computePenetration(OBB a, OBB b) {
        Vector3D bestAxis = Vector3D.ZERO;
        double bestDepth = Double.MAX_VALUE;
        Vector3D[] testAxes = new Vector3D[15];
        System.arraycopy(a.axes, 0, testAxes, 0, 3);
        System.arraycopy(b.axes, 0, testAxes, 3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 3; j < 6; j++) {
                Vector3D cross = a.axes[i].cross(b.axes[j - 3]);
                if (cross.magnitudeSquared() > 1e-10) {
                    testAxes[6 + (i * 3) + (j - 3)] = cross.normalize();
                }
            }
        }
        for (Vector3D axis : testAxes) {
            if (axis.magnitudeSquared() < 1e-10) continue;
            double ra = projectOBB(axis, a);
            double rb = projectOBB(axis, b);
            Vector3D d = b.center.subtract(a.center);
            double distance = Math.abs(d.dot(axis));
            double depth = ra + rb - distance;
            if (depth < bestDepth) {
                bestDepth = depth;
                bestAxis = axis;
            }
        }
        return bestAxis.multiply(bestDepth);
    }

    public Vector3D getCenter() {
        return center;
    }

    public void setCenter(Vector3D center) {
        this.center = center;
    }

    public Vector3D getHalfExtents() {
        return halfExtents;
    }

    public Vector3D[] getAxes() {
        return axes;
    }

    public record CollisionManifold(Vector3D contactPoint, Vector3D normal, double penetrationDepth) {
    }
}
