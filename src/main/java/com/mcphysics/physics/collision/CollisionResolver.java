package com.mcphysics.physics.collision;

import com.mcphysics.physics.RigidBody;
import com.mcphysics.physics.Vector3D;

import java.util.List;

public final class CollisionResolver {

    private final double baumgarteFactor;
    private final double slop;

    public CollisionResolver() {
        this.baumgarteFactor = 0.2;
        this.slop = 0.01;
    }

    public CollisionResolver(double baumgarteFactor, double slop) {
        this.baumgarteFactor = baumgarteFactor;
        this.slop = slop;
    }

    public void resolveSphereSphere(RigidBody a, RigidBody b, SphereCollider.SphereCollisionResult collision) {
        if (a.isStatic() && b.isStatic()) return;
        Vector3D normal = collision.normal();
        double penetration = collision.penetrationDepth();
        if (penetration <= 0) return;
        resolveInterpenetration(a, b, normal, penetration);
        resolveVelocity(a, b, normal);
    }

    public void resolveSphereStatic(RigidBody body, Vector3D normal, double penetration) {
        if (body.isStatic()) return;
        resolveInterpenetrationSingle(body, normal, penetration);
        resolveVelocityStatic(body, normal);
    }

    public void resolveInterpenetration(RigidBody a, RigidBody b, Vector3D normal, double penetration) {
        double totalInverseMass = a.getInverseMass() + b.getInverseMass();
        if (totalInverseMass <= 0) return;
        double correction = Math.max(penetration - slop, 0) / totalInverseMass * baumgarteFactor;
        Vector3D correctionVector = normal.multiply(correction);
        if (!a.isStatic()) {
            a.setPosition(a.getPosition().subtract(correctionVector.multiply(a.getInverseMass())));
        }
        if (!b.isStatic()) {
            b.setPosition(b.getPosition().add(correctionVector.multiply(b.getInverseMass())));
        }
    }

    public void resolveInterpenetrationSingle(RigidBody body, Vector3D normal, double penetration) {
        if (body.isStatic() || body.getInverseMass() <= 0) return;
        double correction = Math.max(penetration - slop, 0) * baumgarteFactor;
        body.setPosition(body.getPosition().add(normal.multiply(correction)));
    }

    public void resolveVelocity(RigidBody a, RigidBody b, Vector3D normal) {
        Vector3D relativeVelocity = b.getVelocity().subtract(a.getVelocity());
        double velAlongNormal = relativeVelocity.dot(normal);
        if (velAlongNormal > 0) return;
        double restitution = Math.min(a.getRestitution(), b.getRestitution());
        double impulseScalar = -(1 + restitution) * velAlongNormal;
        impulseScalar /= a.getInverseMass() + b.getInverseMass();
        Vector3D impulse = normal.multiply(impulseScalar);
        if (!a.isStatic()) {
            a.setVelocity(a.getVelocity().subtract(impulse.multiply(a.getInverseMass())));
        }
        if (!b.isStatic()) {
            b.setVelocity(b.getVelocity().add(impulse.multiply(b.getInverseMass())));
        }
        applyFriction(a, b, normal, impulseScalar);
    }

    public void resolveVelocityStatic(RigidBody body, Vector3D normal) {
        Vector3D velocity = body.getVelocity();
        double velAlongNormal = velocity.dot(normal);
        if (velAlongNormal > 0) return;
        double restitution = body.getRestitution();
        double impulseScalar = -(1 + restitution) * velAlongNormal;
        Vector3D impulse = normal.multiply(impulseScalar);
        body.setVelocity(body.getVelocity().add(impulse));
    }

    private void applyFriction(RigidBody a, RigidBody b, Vector3D normal, double normalImpulse) {
        Vector3D relativeVelocity = b.getVelocity().subtract(a.getVelocity());
        Vector3D tangent = relativeVelocity.subtract(normal.multiply(relativeVelocity.dot(normal)));
        double tangentMag = tangent.magnitude();
        if (tangentMag < 1e-10) return;
        tangent = tangent.divide(tangentMag);
        double frictionScalar = -relativeVelocity.dot(tangent);
        frictionScalar /= a.getInverseMass() + b.getInverseMass();
        double mu = Math.sqrt(a.getFriction() * b.getFriction());
        Vector3D frictionImpulse;
        if (Math.abs(frictionScalar) < normalImpulse * mu) {
            frictionImpulse = tangent.multiply(frictionScalar);
        } else {
            frictionImpulse = tangent.multiply(-normalImpulse * mu);
        }
        if (!a.isStatic()) {
            a.setVelocity(a.getVelocity().subtract(frictionImpulse.multiply(a.getInverseMass())));
        }
        if (!b.isStatic()) {
            b.setVelocity(b.getVelocity().add(frictionImpulse.multiply(b.getInverseMass())));
        }
    }

    public void resolveRaycast(RigidBody body, Vector3D rayOrigin, Vector3D rayDirection, Vector3D hitPoint, Vector3D hitNormal) {
        if (body.isStatic()) return;
        double velAlongNormal = body.getVelocity().dot(hitNormal);
        if (velAlongNormal > 0) return;
        Vector3D impulse = hitNormal.multiply(-velAlongNormal * (1 + body.getRestitution()));
        body.setVelocity(body.getVelocity().add(impulse));
        body.setPosition(hitPoint.add(hitNormal.multiply(0.01)));
    }

    public void resolveMultipleCollisions(List<RigidBody> bodies) {
        for (int i = 0; i < bodies.size(); i++) {
            RigidBody a = bodies.get(i);
            if (a.isStatic() || a.getCollider() == null) continue;
            for (int j = i + 1; j < bodies.size(); j++) {
                RigidBody b = bodies.get(j);
                if (b.getCollider() == null) continue;
                SphereCollider.SphereCollisionResult result = a.getCollider().testCollision(b.getCollider());
                if (result != null) {
                    resolveSphereSphere(a, b, result);
                }
            }
        }
    }
}
