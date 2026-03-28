package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Классифицирует контуры как внешние или внутренние
 * по четности уровня вложенности.
 *
 * Правило такое:
 * - если контур не лежит ни в одном другом контуре, он внешний
 * - если лежит внутри одного контура, он внутренний
 * - если внутри двух, снова внешний
 * - и так далее
 *
 * Предполагается, что (проверяем отдельно перед классификацией):
 * - контуры не имеют самопересечений
 * - разные контуры не пересекаются между собой
 */
public final class ContourClassifier {
    private ContourClassifier() {
    }

    /**
     * Возвращает новый список контуров с проставленным типом:
     * OUTER или INNER.
     *
     * Исходные контуры не изменяются.
     */
    public static List<Contour> classify(List<Contour> contours) {
        /*
            Особые случаи, когда и проверять нечего
         */
        if (contours.isEmpty()) {
            return List.of();
        }
        if (contours.size() == 1) {
            return List.of(contours.get(0).withType(Contour.Type.OUTER));
        }

        List<Contour> result = new ArrayList<>(contours.size());

        /*
            Для каждого контура заранее подбираем тестовую точку,
            которая должна лежать строго внутри него.
         */
        List<Point2> innerPoints = new ArrayList<>(contours.size());
        for (Contour contour : contours) {
            innerPoints.add(findInnerPoint(contour));
        }

        for (int i = 0; i < contours.size(); i++) {
            Contour contour = contours.get(i);
            Point2 innerPoint = innerPoints.get(i);

            int containingCount = 0;

            for (int j = 0; j < contours.size(); j++) {
                if (i == j) {
                    continue;
                }

                if (contours.get(j).containsPoint(innerPoint)) {
                    containingCount++;
                }
            }

            Contour.Type type = (containingCount % 2 == 0)
                ? Contour.Type.OUTER
                : Contour.Type.INNER;

            result.add(contour.withType(type));
        }

        return result;
    }

    /**
     * Подбирает точку, лежащую строго внутри контура.
     *
     * Для этого:
     * - нормализуем контур к обходу против часовой стрелки
     * - берем середину одного из его ребер
     * - смещаемся чуть-чуть влево от ребра
     *
     * Для CCW-контура левая сторона ребра соответствует внутренней области.
     */
    private static Point2 findInnerPoint(Contour contour) {
        Contour normalized = contour.normalized();
        List<Segment2> segments = normalized.toSegments();

        for (Segment2 segment : segments) {
            Vector2 edge = segment.toVector();
            double length = edge.length();

            /*
                Пропускаем вырожденные ребра.
             */
            if (length <= Point2.EPSILON) {
                continue;
            }

            Point2 middle = segment.pointAt(0.5);

            /*
                Единичная нормаль влево.
                Для CCW-контура это направление внутрь.
             */
            Vector2 inwardNormal = new Vector2(-edge.y / length, edge.x / length);

            /*
                Пробуем несколько маленьких смещений,
                чтобы не попасть точно на границу.
             */
            double[] factors = {1e-3, 1e-4, 1e-5, 1e-6, 1e-7};

            for (double factor : factors) {
                Point2 candidate = middle.add(inwardNormal.multiply(length * factor));

                if (isStrictlyInside(normalized, candidate)) {
                    return candidate;
                }
            }
        }

        throw new IllegalStateException("Failed to find inner point for contour");
    }

    /**
     * Возвращает true, если точка лежит внутри контура
     * и не лежит на его границе.
     */
    private static boolean isStrictlyInside(Contour contour, Point2 point) {
        if (!contour.containsPoint(point)) {
            return false;
        }

        for (Segment2 segment : contour.toSegments()) {
            if (segment.containsPoint(point)) {
                return false;
            }
        }

        return true;
    }
}
