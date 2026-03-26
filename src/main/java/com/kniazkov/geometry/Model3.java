package com.kniazkov.geometry;

import java.util.List;

/**
 * Трехмерная модель, представленная набором треугольников.
 */
public class Model3 {
    public final List<Triangle3> triangles;

    public final double minX;
    public final double minY;
    public final double minZ;

    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public Model3(List<Triangle3> triangles) {
        if (triangles == null || triangles.isEmpty()) {
            throw new IllegalArgumentException("Model must contain at least one triangle");
        }

        this.triangles = List.copyOf(triangles);

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Triangle3 triangle : triangles) {
            minX = Math.min(minX, Math.min(triangle.a.x, Math.min(triangle.b.x, triangle.c.x)));
            minY = Math.min(minY, Math.min(triangle.a.y, Math.min(triangle.b.y, triangle.c.y)));
            minZ = Math.min(minZ, Math.min(triangle.a.z, Math.min(triangle.b.z, triangle.c.z)));

            maxX = Math.max(maxX, Math.max(triangle.a.x, Math.max(triangle.b.x, triangle.c.x)));
            maxY = Math.max(maxY, Math.max(triangle.a.y, Math.max(triangle.b.y, triangle.c.y)));
            maxZ = Math.max(maxZ, Math.max(triangle.a.z, Math.max(triangle.b.z, triangle.c.z)));
        }

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
}
