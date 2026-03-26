package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Треугольник в трехмерном пространстве.
 */
public class Triangle3 {
    public final Point3 a;
    public final Point3 b;
    public final Point3 c;

    public Triangle3(Point3 a, Point3 b, Point3 c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * Возвращает геометрическую нормаль треугольника.
     *
     * Нормаль вычисляется как векторное произведение:
     * (b - a) x (c - a)
     *
     * Длина нормали зависит от площади треугольника.
     * Если нужен именно единичный вектор, используй normal.normalized().
     */
    public Vector3 normal() {
        Vector3 ab = b.subtract(a);
        Vector3 ac = c.subtract(a);
        return ab.cross(ac);
    }

    /**
     * Возвращает уникальные точки пересечения треугольника с горизонтальной плоскостью z = planeZ.
     *
     * Возможные результаты:
     * - 0 точек: пересечения нет
     * - 1 точка: плоскость касается треугольника в вершине
     * - 2 точки: обычный случай, пересечение является отрезком
     * - 3 точки: треугольник целиком лежит в плоскости
     */
    public List<Point3> intersectWithHorizontalPlane(double planeZ) {
        List<Point3> result = new ArrayList<>();

        /*
            Сначала добавляем вершины, которые уже лежат в плоскости.
         */
        addPointIfOnPlane(result, a, planeZ);
        addPointIfOnPlane(result, b, planeZ);
        addPointIfOnPlane(result, c, planeZ);

        /*
            Затем ищем пересечения плоскости с ребрами.
            Если вершина ребра уже лежит в плоскости, метод ниже ничего не добавит повторно:
            уникальность контролируется отдельно.
         */
        addEdgeIntersection(result, a, b, planeZ);
        addEdgeIntersection(result, b, c, planeZ);
        addEdgeIntersection(result, c, a, planeZ);

        return result;
    }

    /**
     * Добавляет точку в список, если она лежит в плоскости z = planeZ.
     */
    private static void addPointIfOnPlane(List<Point3> result, Point3 p, double planeZ) {
        if (Math.abs(p.z - planeZ) <= Point3.EPSILON) {
            addUniquePoint(result, p);
        }
    }

    /**
     * Ищет пересечение ребра [p1, p2] с горизонтальной плоскостью z = planeZ
     * и при наличии добавляет точку пересечения.
     *
     * Логика такая:
     * - если обе вершины ребра уже лежат в плоскости, здесь ничего не делаем,
     *   потому что они уже были обработаны отдельно
     * - если ребро не пересекает плоскость по z, выходим
     * - иначе линейно интерполируем параметр t и находим точку пересечения
     */
    private static void addEdgeIntersection(List<Point3> result, Point3 p1, Point3 p2, double planeZ) {
        double dz1 = p1.z - planeZ;
        double dz2 = p2.z - planeZ;

        boolean p1OnPlane = Math.abs(dz1) <= Point3.EPSILON;
        boolean p2OnPlane = Math.abs(dz2) <= Point3.EPSILON;

        /*
            Если обе вершины уже лежат в плоскости, все нужные точки уже добавлены.
         */
        if (p1OnPlane && p2OnPlane) {
            return;
        }

        /*
            Если одна вершина лежит в плоскости, она уже добавлена отдельно.
         */
        if (p1OnPlane || p2OnPlane) {
            return;
        }

        /*
            Если обе точки строго по одну сторону плоскости, пересечения нет.
         */
        if ((dz1 < 0.0 && dz2 < 0.0) || (dz1 > 0.0 && dz2 > 0.0)) {
            return;
        }

        /*
            Здесь плоскость проходит между p1 и p2.
            Ищем параметр t в формуле:
              p = p1 + t * (p2 - p1)
            где p.z = planeZ
         */
        double t = (planeZ - p1.z) / (p2.z - p1.z);
        Point3 intersection = new Point3(
                p1.x + (p2.x - p1.x) * t,
                p1.y + (p2.y - p1.y) * t,
                planeZ
        );

        addUniquePoint(result, intersection);
    }

    /**
     * Добавляет точку только если в списке еще нет приблизительно равной точки.
     */
    private static void addUniquePoint(List<Point3> result, Point3 point) {
        for (Point3 existing : result) {
            if (existing.approximatelyEquals(point)) {
                return;
            }
        }
        result.add(point);
    }
}
