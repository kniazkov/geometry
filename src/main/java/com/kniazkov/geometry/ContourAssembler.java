package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Склеивает набор сегментов в замкнутые контуры.
 *
 * Алгоритм работает через:
 * - пространственный индекс SegmentMap
 * - множество еще не использованных сегментов
 *
 * Незамкнутые контуры и контуры менее чем из трех точек
 * отбрасываются.
 */
public class ContourAssembler {
    private final SegmentMap segmentMap;
    private final Set<Segment2> unusedSegments;
    private final double epsilon;


    public ContourAssembler(List<Segment2> segments, double epsilon) {
        this.segmentMap = new SegmentMap(segments);
        this.unusedSegments = new HashSet<>(segments);
        this.epsilon = epsilon;
    }

    /**
     * Собирает все возможные замкнутые контуры.
     */
    public List<Contour> assemble() {
        List<Contour> result = new ArrayList<>();

        while (!unusedSegments.isEmpty()) {
            Segment2 seed = unusedSegments.iterator().next();
            Contour contour = assembleContour(seed);

            if (contour != null) {
                result.add(contour);
            }
        }

        return result;
    }

    /**
     * Собирает один контур, начиная с указанного сегмента.
     *
     * Если удалось получить замкнутый контур хотя бы из трех точек,
     * он возвращается. Иначе возвращается null.
     */
    private Contour assembleContour(Segment2 seed) {
        List<Point2> points = new ArrayList<>();

        Point2 startPoint = seed.a;
        Point2 currentPoint = seed.b;

        points.add(startPoint);
        points.add(currentPoint);
        unusedSegments.remove(seed);

        while (true) {
            Point2 nextPoint = findNextPoint(currentPoint);

            if (nextPoint == null) {
                Point2 lastPoint = points.get(points.size() - 1);
                Point2 firstPoint = points.get(0);

                if (lastPoint.distanceTo(firstPoint) <= epsilon) {
                    points.remove(points.size() - 1);

                    if (points.size() >= 3) {
                        return new Contour(points);
                    }
                }

                return null;
            }

            points.add(nextPoint);
            currentPoint = nextPoint;
        }
    }

    /**
     * Ищет следующую точку контура по текущей точке.
     *
     * Из всех сегментов-кандидатов выбирается тот, у которого одна из конечных точек ближе всего к текущей точке.
     * Если даже минимальное расстояние больше epsilon, считается, что продолжения нет.
     *
     * Если подходящий сегмент найден, он удаляется из множества неиспользованных,
     * а метод возвращает его противоположную точку.
     */
    private Point2 findNextPoint(Point2 currentPoint) {
        Set<Segment2> candidates = segmentMap.find(currentPoint);
        if (candidates.isEmpty()) {
            return null;
        }

        double minDistance = Double.POSITIVE_INFINITY;
        Segment2 bestSegment = null;
        Point2 bestNextPoint = null;

        for (Segment2 segment : candidates) {
            if (!unusedSegments.contains(segment)) {
                continue;
            }

            double distanceToA = currentPoint.distanceTo(segment.a);
            if (distanceToA < minDistance) {
                minDistance = distanceToA;
                bestSegment = segment;
                bestNextPoint = segment.b;
            }

            double distanceToB = currentPoint.distanceTo(segment.b);
            if (distanceToB < minDistance) {
                minDistance = distanceToB;
                bestSegment = segment;
                bestNextPoint = segment.a;
            }
        }

        if (minDistance > epsilon) {
            return null;
        }

        unusedSegments.remove(bestSegment);
        return bestNextPoint;
    }
}
