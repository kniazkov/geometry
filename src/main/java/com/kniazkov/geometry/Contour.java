package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Замкнутый двумерный контур.
 *
 * Контур задается списком вершин в порядке обхода.
 * При этом первую точку НЕ нужно дублировать в конце: замыкание контура выполняется автоматически.
 *
 * Класс немутабельный:
 * - входной список копируется
 * - наружу отдается только неизменяемое представление
 */
public class Contour {
    public final List<Point2> points;

    public Contour(List<Point2> points) {
        if (points.size() < 3) {
            throw new IllegalArgumentException("Contour must contain at least 3 points");
        }

        this.points = List.copyOf(points);
    }

    /**
     * Превращает контур в набор его ребер.
     *
     * Так как контур замкнутый, последний отрезок соединяет
     * последнюю вершину с первой.
     */
    public List<Segment2> toSegments() {
        List<Segment2> result = new ArrayList<>(points.size());

        for (int i = 0; i < points.size(); i++) {
            Point2 a = points.get(i);
            Point2 b = points.get((i + 1) % points.size());
            result.add(new Segment2(a, b));
        }

        return result;
    }
}
