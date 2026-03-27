package com.kniazkov.geometry;

/**
 * Точка в двумерном пространстве.
 */
public class Point2 implements SegmentIntersection {
    /**
     * Допуск для геометрических сравнений.
     * Две координаты считаются равными, если отличаются не больше, чем на эту величину.
     */
    public static final double EPSILON = 1e-9;

    public final double x;
    public final double y;


    public Point2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Смещает точку на заданный вектор.
     */
    public Point2 add(Vector2 vector) {
        return new Point2(this.x + vector.x, this.y + vector.y);
    }

    /**
     * Вычитание двух точек дает вектор:
     * направление и расстояние от другой точки до текущей.
     *
     * Иными словами:
     * this - other = вектор от other к this
     */
    public Vector2 subtract(Point2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }

    /**
     * Смещает точку на вектор в противоположную сторону.
     */
    public Point2 subtract(Vector2 vector) {
        return new Point2(this.x - vector.x, this.y - vector.y);
    }

    /**
     * Евклидово расстояние между двумя точками.
     */
    public double distanceTo(Point2 other) {
        return Math.sqrt(distanceSquaredTo(other));
    }

    /**
     * Квадрат расстояния между двумя точками.
     * Полезно, когда не нужен sqrt.
     */
    public double distanceSquaredTo(Point2 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    /**
     * Приближенное сравнение двух точек с использованием стандартного допуска.
     */
    public boolean approximatelyEquals(Point2 other) {
        return approximatelyEquals(other, EPSILON);
    }

    /**
     * Приближенное сравнение двух точек с указанным допуском.
     */
    public boolean approximatelyEquals(Point2 other, double epsilon) {
        return Math.abs(this.x - other.x) <= epsilon && Math.abs(this.y - other.y) <= epsilon;
    }

    /**
     * Преобразует точку в ячейку грубой пространственной сетки.
     * Обе координаты округляются вниз.
     */
    public Cell toCell() {
        return new Cell((int) Math.floor(x), (int) Math.floor(y));
    }
}
