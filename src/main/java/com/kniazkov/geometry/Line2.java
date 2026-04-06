package com.kniazkov.geometry;

import java.util.Optional;

/**
 * Прямая в двумерном пространстве.
 *
 * Прямая хранится в нормализованном виде:
 *     ax + by + c = 0
 *
 * Коэффициенты a и b образуют единичный нормальный вектор,
 * поэтому signed distance до точки равен значению выражения
 * ax + by + c.
 */
public class Line2 {
    public final double a;
    public final double b;
    public final double c;


    /**
     * Создает прямую по коэффициентам общего уравнения.
     *
     * Коэффициенты нормализуются так, чтобы длина вектора (a, b) была равна 1.
     */
    public Line2(double a, double b, double c) {
        double length = Math.hypot(a, b);
        if (length <= Point2.EPSILON) {
            throw new IllegalArgumentException("Line normal must not be zero");
        }

        this.a = a / length;
        this.b = b / length;
        this.c = c / length;
    }

    /**
     * Строит прямую по двум различным точкам.
     */
    public static Line2 fromPoints(Point2 p1, Point2 p2) {
        Vector2 direction = p2.subtract(p1);
        double length = direction.length();

        if (length <= Point2.EPSILON) {
            throw new IllegalArgumentException("Cannot build line from identical points");
        }

        /*
            Левая единичная нормаль к направлению (dx, dy):
            (-dy, dx)
         */
        double a = -direction.y / length;
        double b = direction.x / length;
        double c = -(a * p1.x + b * p1.y);

        return new Line2(a, b, c);
    }

    /**
     * Возвращает true, если точка лежит на прямой с учетом допуска.
     */
    public boolean containsPoint(Point2 point) {
        return Math.abs(signedDistanceTo(point)) <= Point2.EPSILON;
    }

    /**
     * Возвращает ориентированное расстояние от прямой до точки.
     *
     * Положительное значение означает, что точка лежит
     * по направлению нормали (a, b),
     * отрицательное - с противоположной стороны.
     */
    public double signedDistanceTo(Point2 point) {
        return a * point.x + b * point.y + c;
    }

    /**
     * Возвращает обычное расстояние от прямой до точки.
     */
    public double distanceTo(Point2 point) {
        return Math.abs(signedDistanceTo(point));
    }

    /**
     * Возвращает параллельную прямую, смещенную на указанное расстояние
     * вдоль нормали (a, b).
     *
     * Положительное расстояние сдвигает прямую в сторону нормали,
     * отрицательное - в противоположную сторону.
     */
    public Line2 offset(double distance) {
        return new Line2(a, b, c - distance);
    }

    /**
     * Возвращает точку пересечения с другой прямой.
     *
     * Если прямые параллельны или почти параллельны, возвращается Optional.empty().
     */
    public Optional<Point2> intersect(Line2 other) {
        double determinant = a * other.b - other.a * b;

        if (Math.abs(determinant) <= Point2.EPSILON) {
            return Optional.empty();
        }

        double x = (b * other.c - other.b * c) / determinant;
        double y = (other.a * c - a * other.c) / determinant;

        return Optional.of(new Point2(x, y));
    }

    /**
     * Возвращает направляющий вектор прямой.
     *
     * Для нормали (a, b) один из направляющих векторов равен (b, -a).
     */
    public Vector2 toDirectionVector() {
        return new Vector2(b, -a);
    }

    @Override
    public String toString() {
        return a + "x + " + b + "y + " + c + " = 0";
    }
}
