package com.kniazkov.geometry;

/**
 * Грубая дискретизация точки на плоскости.
 *
 * Ячейка определяется целочисленными координатами: в нее попадают все точки, у которых
 * floor(x) и floor(y) совпадают с координатами этой ячейки.
 *
 * Класс немутабельный и подходит для использования в Map.
 */
public class Cell {
    public final int x;
    public final int y;


    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Возвращает соседнюю ячейку со смещением.
     */
    public Cell offset(int dx, int dy) {
        return new Cell(x + dx, y + dy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Cell other)) {
            return false;
        }
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }
}
