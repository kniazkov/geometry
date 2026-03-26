package com.kniazkov.geometry;

/**
 * Вектор в двумерном пространстве.
 * Описывает смещение: направление и длину.
 */
public class Vector2 {
    public final double x;
    public final double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Длина вектора.
     */
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * Квадрат длины вектора.
     * Полезно, когда не хочется лишний раз вызывать sqrt.
     */
    public double lengthSquared() {
        return x * x + y * y;
    }

    /**
     * Возвращает единичный вектор в том же направлении.
     *
     * Если вектор нулевой, бросаем исключение, потому что
     * у нулевого вектора направление не определено.
     */
    public Vector2 normalized() {
        double len = length();
        if (len == 0.0) {
            throw new IllegalStateException("Cannot normalize zero-length vector");
        }
        return new Vector2(x / len, y / len);
    }

    /**
     * Умножение вектора на число.
     */
    public Vector2 multiply(double scalar) {
        return new Vector2(x * scalar, y * scalar);
    }

    /**
     * Сложение двух векторов.
     */
    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }

    /**
     * Вычитание двух векторов.
     */
    public Vector2 subtract(Vector2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }

    /**
     * Скалярное произведение.
     * Часто нужно для углов, проекций и проверок направления.
     */
    public double dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }
}
