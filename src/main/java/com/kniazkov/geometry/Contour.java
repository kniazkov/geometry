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
    public enum Type {
        UNCLASSIFIED,
        OUTER,
        INNER
    }

    public final Type type;
    public final List<Point2> points;
    private java.util.Map<Point2, Integer> pointIndices;
    private List<Segment2> segments;
    private Double signedArea;


    public Contour(List<Point2> points) {
        this(Type.UNCLASSIFIED, List.copyOf(points));
    }

    private Contour(Type type, List<Point2> points) {
        if (points.size() < 3) {
            throw new IllegalArgumentException("Contour must contain at least 3 points");
        }

        this.type = type;
        this.points = points;
    }

    /**
     * Возвращает новый контур с теми же точками, но с указанным типом.
     */
    public Contour withType(Type type) {
        Contour contour = new Contour(type, points);
        contour.pointIndices = pointIndices;
        contour.signedArea = signedArea;
        contour.segments = segments;
        return contour;
    }

    /**
     * Возвращает индекс точки в контуре.
     *
     * Если точка в контуре не найдена, возвращается -1.
     */
    public int getPointIndex(Point2 point) {
        if (pointIndices == null) {
            pointIndices = new java.util.HashMap<>(points.size());

            for (int i = 0; i < points.size(); i++) {
                pointIndices.put(points.get(i), i);
            }
        }

        Integer index = pointIndices.get(point);
        return index != null ? index : -1;
    }

    /**
     * Превращает контур в набор его ребер.
     *
     * Так как контур замкнутый, последний отрезок соединяет
     * последнюю вершину с первой.
     */
    public List<Segment2> toSegments() {
        if (segments == null) {
            segments = new ArrayList<>(points.size());
            for (int i = 0; i < points.size(); i++) {
                Point2 a = points.get(i);
                Point2 b = points.get((i + 1) % points.size());
                segments.add(new Segment2(a, b));
            }
        }

        return segments;
    }

    /**
     * Превращает контур в двусвязный циклический список узлов.
     */
    public Node2 toLinkedList() {
        return Node2.fromPoints(normalized().points);
    }

    /**
     * Превращает двусвязный циклический список узлов в контур.
     */
    public static Contour fromLinkedList(Node2 node) {
        return new Contour(Node2.toPoints(node));
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
        return new Contour(type, Collections.unmodifiableList(reversed));
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

    /**
     * Возвращает true, если точка лежит внутри контура или на его границе.
     *
     * Метод работает и для невыпуклых контуров.
     * Используется алгоритм ray casting: из точки пускается горизонтальный луч вправо,
     * и считается число пересечений с ребрами контура.
     *
     * Если число пересечений нечетное, точка внутри.
     * Если четное, точка снаружи.
     *
     * Точка на границе считается принадлежащей контуру.
     */
    public boolean containsPoint(Point2 point) {
        List<Segment2> segments = toSegments();

    /*
        Сначала отдельно проверяем случай, когда точка лежит прямо на границе.
     */
        for (Segment2 segment : segments) {
            if (segment.containsPoint(point)) {
                return true;
            }
        }

        int intersections = 0;

        for (Segment2 segment : segments) {
            Point2 a = segment.a;
            Point2 b = segment.b;

        /*
            Проверяем, пересекает ли ребро горизонтальную прямую y = point.y.
            Используем полуинтервал, чтобы вершины не считались дважды.
         */
            boolean intersectsByY = (a.y > point.y) != (b.y > point.y);
            if (!intersectsByY) {
                continue;
            }

        /*
            Находим x-координату точки пересечения ребра с горизонтальной прямой y = point.y.
         */
            double xIntersection = a.x + (point.y - a.y) * (b.x - a.x) / (b.y - a.y);

        /*
            Считаем только пересечения строго справа от точки.
            Случай попадания ровно на границу уже обработан выше.
         */
            if (xIntersection > point.x) {
                intersections++;
            }
        }

        return (intersections % 2) == 1;
    }
}
