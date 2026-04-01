package com.kniazkov.geometry;

import java.util.*;

/**
 * Заготовка для смещения контура внутрь или наружу.
 *
 * Предполагается, что:
 * - положительное смещение задает движение наружу
 * - отрицательное смещение задает движение внутрь
 *
 * Пока класс содержит только API, без реализации алгоритма.
 */
public class ContourOffsetter {
    /**
     * Исходный контур.
     */
    private final Contour contour;

    /**
     * Максимальная длина одного добавляемого сегмента при аппроксимации дуги
     * вокруг внешнего угла.
     *
     * Чем меньше значение, тем более гладкой получается дуга,
     * но тем больше в ней будет сегментов.
     */
    private double maxArcSegmentLength = 1.0;


    public ContourOffsetter(Contour contour) {
        this.contour = contour;
    }

    /**
     * Возвращает исходный контур, с которым работает смещатель.
     */
    public Contour getContour() {
        return contour;
    }

    /**
     * Задает максимальную длину одного сегмента,
     * используемого для аппроксимации дуги вокруг внешнего угла.
     */
    public void setMaxArcSegmentLength(double length) {
        if (length <= 0.0) {
            throw new IllegalArgumentException("Max arc segment length must be positive");
        }

        this.maxArcSegmentLength = length;
    }

    /**
     * Смещает контур на указанное расстояние.
     *
     * Положительное значение означает смещение наружу,
     * отрицательное - внутрь.
     *
     * Пока метод не реализован.
     */
    public List<OffsetResult> offset(double distance) {
        /*
            Вырожденный случай.
         */
        if (distance == 0.0) {
            OffsetResult.Builder builder = new OffsetResult.Builder(contour, Map.of());
            builder.setOffsetContour(contour);
            for (Point2 point : contour.points) {
                builder.addCorrespondingPoints(point, point);
            }
            return List.of(builder.build());
        }

        /*
            Действительное смещение зависит от типа контура: для внутренних контуров положительное
            смещение означает смещение внутрь.
         */
        if (contour.type == Contour.Type.INNER) {
            distance = -distance;
        }

        /*
            Сначала упрощаем контур перед смещением.

            Идея такая:
            - строим двусвязный циклический список узлов
            - удаляем слишком короткие сегменты, которые при смещении гарантированно
              схлопнутся и породят маленькие петли
            - какие именно узлы удалять, зависит от направления смещения:
              - при смещении наружу удаляем внутренние углы
              - при смещении внутрь удаляем внешние углы

            Порог берем равным абсолютной величине смещения.
         */
        Node2 list = contour.toLinkedList();
        double absDistance = Math.abs(distance);

        final Optional<Node2ProcessingResult> reduced;
        if (distance > 0.0) {
            Node2ChainReducer reducer = new Node2ChainReducer(
                absDistance * 0.5,
                node -> !node.isOuter()
            );
            reduced = reducer.reduce(list);
        } else {
            Node2ChainReducer reducer = new Node2ChainReducer(
                absDistance * 0.5,
                node -> node.isOuter()
            );
            reduced = reducer.reduce(list);
        }

        /*
         * Если в упрощенном контуре не осталось точек, значит нам нечего сдвигать.
         */
        if (reduced.isEmpty()) {
            return List.of();
        }

        /*
            Строим упрощенный контур и массив смещенных сегментов упрощенного контура.
         */
        Contour simplified = Contour.fromLinkedList(contour.type, reduced.get().node);
        List<Segment2> simplifiedSegments = simplified.toSegments();
        List<Segment2> offsetSegments = new ArrayList<>(simplifiedSegments.size());
        for (Segment2 segment : simplified.toSegments()) {
            offsetSegments.add(segment.offset(distance));
        }

        /*
            Собираем вершины итогового смещенного контура.

            Для каждой пары соседних смещенных сегментов:
            - если они пересекаются в точке, берем эту точку
            - если пересекаются отрезком, это недопустимо
            - если не пересекаются, достраиваем между ними дугу радиуса absDistance

            В качестве центра дуги берем исходную вершину упрощенного контура,
            то есть общую точку соответствующих оригинальных сегментов.

            Сначала обрабатывается пара:
            - последний смещенный сегмент
            - первый смещенный сегмент

            Это дает вершину, соответствующую первой точке контура.
         */
        OffsetResult.Builder builder = new OffsetResult.Builder(
            contour,
            reduced.get().pointMapping
        );
        List<Point2> offsetPoints = new ArrayList<>(offsetSegments.size());

        int size = offsetSegments.size();
        for (int i = 0; i < size; i++) {
            int previousIndex = (i - 1 + size) % size;

            Segment2 previousOriginalSegment = simplifiedSegments.get(previousIndex);
            Segment2 previousOffsetSegment = offsetSegments.get(previousIndex);
            Segment2 currentOffsetSegment = offsetSegments.get(i);

            var intersection = previousOffsetSegment.intersect(currentOffsetSegment);

            if (intersection.isPresent()) {
                SegmentIntersection value = intersection.get();

                if (value instanceof Point2 point) {
                    offsetPoints.add(point);
                    builder.addCorrespondingPoints(previousOriginalSegment.b, point);
                } else if (value instanceof Segment2) {
                    throw new IllegalStateException("Unexpected overlapping offset segments");
                } else {
                    throw new IllegalStateException("Unsupported segment intersection type");
                }
            } else {
                Point2 arcStart = previousOffsetSegment.b;
                Point2 arcEnd = currentOffsetSegment.a;
                Point2 center = previousOriginalSegment.b;

                addArcPoints(
                    arcStart,
                    arcEnd,
                    center,
                    absDistance,
                    offsetPoints,
                    builder
                );
            }
        }

        /*
            Строим контур и связи между оригинальным контуром.
         */
        builder.setOffsetContour(new Contour(offsetPoints).withType(contour.type));
        OffsetResult offsetResult = builder.build();

        /*
            Проверяем на самопересечение.
         */
        ContourIntersectionFinder finder = new ContourIntersectionFinder(offsetResult.contour);
        List<ContourIntersection> intersections = finder.findSelfIntersections();
        if (intersections.isEmpty()) {
            return List.of(builder.build());
        }
        return removeLoops(offsetResult, intersections);
    }

    /**
     * Добавляет в список точки дуги от a до b с центром в c.
     *
     * Предполагается, что точки a и b лежат на окружности радиуса radius
     * с центром в c.
     *
     * Всегда выбирается более короткая из двух возможных дуг.
     *
     * Начальная точка a в список не добавляется, потому что обычно она уже
     * была добавлена ранее. Конечная точка b добавляется всегда.
     *
     * Шаг разбиения подбирается так, чтобы длина каждого добавленного
     * сегмента не превышала maxArcSegmentLength.
     */
    private void addArcPoints(
        Point2 a,
        Point2 b,
        Point2 c,
        double radius,
        List<Point2> points,
        OffsetResult.Builder builder
    ) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Radius must be positive");
        }

        Vector2 va = a.subtract(c);
        Vector2 vb = b.subtract(c);

        double startAngle = Math.atan2(va.y, va.x);
        double endAngle = Math.atan2(vb.y, vb.x);

        /*
            Считаем обе возможные дуги:
            - против часовой стрелки
            - по часовой стрелке

            Затем выбираем более короткую.
         */
        double ccwSweep = endAngle - startAngle;
        while (ccwSweep <= 0.0) {
            ccwSweep += 2.0 * Math.PI;
        }

        double cwSweep = startAngle - endAngle;
        while (cwSweep <= 0.0) {
            cwSweep += 2.0 * Math.PI;
        }

        boolean clockwise = cwSweep < ccwSweep;
        double sweepAngle = clockwise ? cwSweep : ccwSweep;

        /*
            Максимальный угловой шаг выбираем из ограничения на длину хорды:
                chord = 2 * radius * sin(angle / 2)
            Требуем:
                chord <= maxArcSegmentLength
         */
        double maxStepAngle;
        if (maxArcSegmentLength >= 2.0 * radius) {
            maxStepAngle = Math.PI;
        } else {
            maxStepAngle = 2.0 * Math.asin(maxArcSegmentLength / (2.0 * radius));
        }

        /*
            На всякий случай страхуемся от численных странностей.
         */
        if (maxStepAngle <= 0.0) {
            maxStepAngle = sweepAngle;
        }

        int segmentCount = Math.max(1, (int) Math.ceil(sweepAngle / maxStepAngle));
        double stepAngle = sweepAngle / segmentCount;

        for (int i = 1; i <= segmentCount; i++) {
            double angle = clockwise
                ? startAngle - stepAngle * i
                : startAngle + stepAngle * i;

            Point2 point = new Point2(
                    c.x + radius * Math.cos(angle),
                    c.y + radius * Math.sin(angle)
            );
            points.add(point);
            builder.addCorrespondingPoints(c, point);
        }
    }

    /**
     * Удаляет петли из смещенного контура, разделяя контур на несколько контуров.
     */
    private static List<OffsetResult> removeLoops(
        OffsetResult offsetResult,
        List<ContourIntersection> intersections)
    {
        return List.of(offsetResult);
    }
}
