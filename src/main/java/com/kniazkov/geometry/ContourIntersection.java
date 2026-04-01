package com.kniazkov.geometry;

/**
 * Одно пересечение двух контуров.
 *
 * Хранит:
 * - геометрический результат пересечения двух сегментов
 * - индекс сегмента первого контура
 * - индекс сегмента второго контура
 *
 * Геометрический результат может быть:
 * - Point2, если сегменты пересекаются в одной точке
 * - Segment2, если сегменты накладываются общим участком
 */
public class ContourIntersection {
    public final SegmentIntersection intersection;
    public final int firstSegmentIndex;
    public final int secondSegmentIndex;

    public ContourIntersection(
        SegmentIntersection intersection,
        int firstSegmentIndex,
        int secondSegmentIndex
    ) {
        this.intersection = intersection;
        this.firstSegmentIndex = firstSegmentIndex;
        this.secondSegmentIndex = secondSegmentIndex;
    }
}
