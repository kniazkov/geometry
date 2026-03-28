package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Collections;
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
    private Double signedArea;


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

    /**
     * Превращает контур в двусвязный циклический список узлов.
     */
    public Node2 toLinkedList() {
        return Node2.fromPoints(normalized().points);
    }

    /**
     * Возвращает ориентированную площадь контура.
     *
     * Если обход идет против часовой стрелки, площадь положительная.
     * Если по часовой стрелке, площадь отрицательная.
     *
     * Так как контур немутабельный, значение вычисляется один раз
     * и затем кешируется.
     */
    public double getSignedArea() {
        if (signedArea == null) {
            double sum = 0.0;

            for (int i = 0; i < points.size(); i++) {
                Point2 a = points.get(i);
                Point2 b = points.get((i + 1) % points.size());
                sum += a.x * b.y - b.x * a.y;
            }

            signedArea = sum * 0.5;
        }

        return signedArea;
    }

    /**
     * Возвращает true, если контур ориентирован против часовой стрелки.
     */
    public boolean isCounterClockwise() {
        return getSignedArea() > 0.0;
    }

    /**
     * Возвращает новый контур с обратным направлением обхода.
     */
    public Contour reversed() {
        List<Point2> reversed = new ArrayList<>(points);
        Collections.reverse(reversed);
        return new Contour(reversed);
    }

    /**
     * Возвращает контур, ориентированный против часовой стрелки.
     *
     * Если текущий контур уже ориентирован правильно,
     * возвращается он сам.
     */
    public Contour normalized() {
        return isCounterClockwise() ? this : reversed();
    }

    /**
     * Нормализует список контуров, приводя каждый из них
     * к ориентации против часовой стрелки.
     */
    public static List<Contour> normalizeAll(List<Contour> contours) {
        List<Contour> result = new ArrayList<>(contours.size());

        for (Contour contour : contours) {
            result.add(contour.normalized());
        }

        return result;
    }
}
