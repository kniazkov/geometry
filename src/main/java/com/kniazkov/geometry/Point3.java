package com.kniazkov.geometry;

/**
 * Точка в трехмерном пространстве.
 */
public class Point3 {
    /**
     * Допуск для геометрических сравнений.
     */
    public static final double EPSILON = 1e-9;

    public final double x;
    public final double y;
    public final double z;


    public Point3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Вычитание двух точек дает вектор:
     * направление от other к текущей точке.
     */
    public Vector3 subtract(Point3 other) {
        return new Vector3(
                this.x - other.x,
                this.y - other.y,
                this.z - other.z
        );
    }

    /**
     * Смещает точку на вектор.
     */
    public Point3 add(Vector3 vector) {
        return new Point3(
                this.x + vector.x,
                this.y + vector.y,
                this.z + vector.z
        );
    }

    /**
     * Приближенное сравнение двух точек.
     */
    public boolean approximatelyEquals(Point3 other) {
        return Math.abs(this.x - other.x) <= EPSILON
                && Math.abs(this.y - other.y) <= EPSILON
                && Math.abs(this.z - other.z) <= EPSILON;
    }
}
