package com.mcphysics.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class ForceRegistry {

    private final List<ForceEntry> entries = new ArrayList<>();

    public void addForce(RigidBody body, Vector3D force) {
        entries.add(new ForceEntry(body, force));
    }

    public void addForces(RigidBody body, List<Vector3D> forces) {
        for (Vector3D force : forces) {
            entries.add(new ForceEntry(body, force));
        }
    }

    public void clear() {
        entries.clear();
    }

    public void applyForces() {
        for (ForceEntry entry : entries) {
            if (entry.body().isStatic()) continue;
            Vector3D acceleration = entry.force().divide(entry.body().getMass());
            entry.body().setVelocity(entry.body().getVelocity().add(acceleration));
        }
        entries.clear();
    }

    public void forEach(BiConsumer<RigidBody, Vector3D> action) {
        for (ForceEntry entry : entries) {
            action.accept(entry.body(), entry.force());
        }
    }

    public int size() {
        return entries.size();
    }

    public record ForceEntry(RigidBody body, Vector3D force) {
    }
}
