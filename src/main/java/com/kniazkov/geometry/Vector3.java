package com.kniazkov.geometry;

/**
 * Вектор в трехмерном пространстве.
 */
public class Vector3 {
    public final double x;
    public final double y;
    public final double z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Длина вектора.
     */
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * Квадрат длины вектора.
     */
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    /**
     * Возвращает единичный вектор того же направления.
     */
    public Vector3 normalized() {
        double len = length();
        if (len == 0.0) {
            throw new IllegalStateException("Cannot normalize zero-length vector");
        }
        return new Vector3(x / len, y / len, z / len);
    }

    /**
     * Сложение векторов.
     */
    public Vector3 add(Vector3 other) {
        return new Vector3(
                this.x + other.x,
                this.y + other.y,
                this.z + other.z
        );
    }

    /**
     * Вычитание векторов.
     */
    public Vector3 subtract(Vector3 other) {
        return new Vector3(
                this.x - other.x,
                this.y - other.y,
                this.z - other.z
        );
    }

    /**
     * Умножение вектора на число.
     */
    public Vector3 multiply(double scalar) {
        return new Vector3(
                this.x * scalar,
                this.y * scalar,
                this.z * scalar
        );
    }

    /**
     * Скалярное произведение.
     */
    public double dot(Vector3 other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    /**
     * Векторное произведение.
     */
    public Vector3 cross(Vector3 other) {
        return new Vector3(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
        );
    }
}
