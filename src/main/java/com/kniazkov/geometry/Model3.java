package com.kniazkov.geometry;

import java.util.List;
import java.util.ArrayList;

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

    /**
     * Строит сечение модели горизонтальной плоскостью z = layerZ.
     *
     * Возвращает список двумерных отрезков в плоскости XY.
     *
     * В список попадают только нормальные сечения треугольников,
     * которые дают ровно две точки пересечения с плоскостью.
     *
     * Случаи, когда пересечение вырождается в одну точку, игнорируются.
     * Случаи, когда треугольник целиком лежит в плоскости, тоже пока игнорируются,
     * потому что они требуют отдельной политики обработки.
     */
    public List<Segment2> sliceAt(double layerZ) {
        List<Segment2> result = new ArrayList<>();

        for (Triangle3 triangle : triangles) {
            List<Point3> intersection = triangle.intersectWithHorizontalPlane(layerZ);

            /*
                Обычный полезный случай: плоскость пересекает треугольник отрезком.
             */
            if (intersection.size() == 2) {
                Point3 p1 = intersection.get(0);
                Point3 p2 = intersection.get(1);

                result.add(new Segment2(
                    new Point2(p1.x, p1.y),
                    new Point2(p2.x, p2.y)
                ));
            }

            // intersection.size() == 0 -> нет пересечения
            // intersection.size() == 1 -> касание вершиной, игнорируем
            // intersection.size() == 3 -> треугольник лежит в плоскости, пока игнорируем
        }

        return result;
    }
}
