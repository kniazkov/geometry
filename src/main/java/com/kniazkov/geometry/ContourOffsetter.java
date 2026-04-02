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
    private final Contour originalContour;

    /**
     * Максимальная длина одного добавляемого сегмента при аппроксимации дуги
     * вокруг внешнего угла.
     *
     * Чем меньше значение, тем более гладкой получается дуга,
     * но тем больше в ней будет сегментов.
     */
    private double maxArcSegmentLength = 1.0;

    /**
     * Минимальная площадь результирующего контура. Все контуры меньшей площади будут
     * отбрасываться.
     */
    private double minContourArea = 100;

    public ContourOffsetter(Contour contour) {
        this.originalContour = contour;
    }

    /**
     * Возвращает исходный контур, с которым работает смещатель.
     */
    public Contour getOriginalContour() {
        return originalContour;
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
     * Задает минимальную площадь результирующих контуров.
     */
    public void setMinContourArea(double area) {
        if (area <= 0.0) {
            throw new IllegalArgumentException("Contour area must be positive");
        }

        this.minContourArea = area;
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
            Map<Point2, Set<Point2>> mapping = new HashMap<>();
            for (Point2 point : originalContour.points) {
                mapping.put(point, Set.of(point));
            }
            return List.of(new OffsetResult(originalContour, originalContour, mapping));
        }

        /*
            Действительное смещение зависит от типа контура: для внутренних контуров положительное
            смещение означает смещение внутрь.
         */
        if (originalContour.type == Contour.Type.INNER) {
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
        Node2 list = originalContour.toLinkedList();
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
        Contour simplified = Contour.fromLinkedList(originalContour.type, reduced.get().node);
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

        List<Point2> offsetPoints = new ArrayList<>(offsetSegments.size());
        PointsMapping pointsMapping = new PointsMapping(reduced.get().pointMapping);

        int size = offsetSegments.size();
        for (int i = 0; i < size; i++) {
            int previousIndex = (i - 1 + size) % size;

            Segment2 previousSimplifiedSegment = simplifiedSegments.get(previousIndex);
            Segment2 previousOffsetSegment = offsetSegments.get(previousIndex);
            Segment2 currentOffsetSegment = offsetSegments.get(i);

            var intersection = previousOffsetSegment.intersect(currentOffsetSegment);

            if (intersection.isPresent()) {
                SegmentIntersection value = intersection.get();

                if (value instanceof Point2 offsetPoint) {
                    offsetPoints.add(offsetPoint);
                    pointsMapping.addPairSimplifiedToOffset(
                        previousSimplifiedSegment.b,
                        offsetPoint
                    );
                } else if (value instanceof Segment2) {
                    throw new IllegalStateException("Unexpected overlapping offset segments");
                } else {
                    throw new IllegalStateException("Unsupported segment intersection type");
                }
            } else {
                Point2 arcStart = previousOffsetSegment.b;
                Point2 arcEnd = currentOffsetSegment.a;
                Point2 center = previousSimplifiedSegment.b;

                addArcPoints(
                    arcStart,
                    arcEnd,
                    center,
                    absDistance,
                    offsetPoints,
                    pointsMapping
                );
            }
        }

        /*
            Строим контур.
         */
        Contour offsetContour = new Contour(offsetPoints).withType(originalContour.type);

        /*
            Полученный контур должен иметь направление против часовой стрелки.
            Если иначе - значит смещенный контур при смещении внутрь получился больше
            оригинального, то есть, некуда было смещать. Возвращаем пустой результат.
         */
        if (!offsetContour.isCounterClockwise()) {
            return List.of();
        }

        /*
            Проверяем на самопересечение. Если их нет, возвращаем результат.
         */
        ContourIntersectionFinder finder = new ContourIntersectionFinder(offsetContour);
        List<ContourIntersection> intersections = finder.findSelfIntersections();
        if (intersections.isEmpty()) {
            return List.of(new OffsetResult(originalContour, offsetContour, pointsMapping.offsetToOriginal));
        }
        return removeLoops(offsetContour, pointsMapping, intersections);
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
        PointsMapping pointsMapping
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
            pointsMapping.addPairSimplifiedToOffset(c, point);
        }
    }

    /*
        Удаляет петли из смещенного контура, разделяя контур на несколько контуров.
     */
    private List<OffsetResult> removeLoops(
        Contour offsetContour,
        PointsMapping pointsMapping,
        List<ContourIntersection> intersections)
    {
        /*
            Сортируем петли. В начале списка должны оказаться петли, которые не имеют
            внутри других петель.
         */
        ContourIntersectionSorter intersectionSorter = new ContourIntersectionSorter(
            offsetContour,
            intersections
        );

        /*
            Строим кольцевой граф, начиная с точки, которая не входит ни в одну петлю.
         */
        List<Segment2> segments = offsetContour.toSegments();
        List<Node> nodes = new ArrayList<>(segments.size());
        Graph mainGraph = new Graph();
        Node first = new Node(segments.get(0));
        first.graph = mainGraph;
        nodes.add(first);
        Node last = first;
        for (int i = 1; i < segments.size(); i++) {
            Segment2 segment = segments.get(i);
            Node node = new Node(segment);
            node.graph = mainGraph;
            node.previous = last;
            last.next = node;
            last = node;
            nodes.add(node);
        }
        first.previous = last;
        last.next = first;
        mainGraph.begin = nodes.get(intersectionSorter.freeSegmentIndex);

        /*
            Разделяем граф по точкам пересечений.
         */
        List<Graph> graphs = new ArrayList<>(intersections.size() + 1);
        graphs.add(mainGraph);
        for (ContourIntersectionSorter.LoopInfo loop : intersectionSorter.loops) {
            if (!(loop.intersection instanceof Point2 intersection)) {
                throw new IllegalStateException(
                    "After the offset, only points are expected as intersections"
                );
            }

            /*
                Пара узлов, сегменты которых пересекаются. Траектория проходит через
                входящий узел, образует петлю и выходит через исходящий узел.
             */
            Node incoming = nodes.get(loop.firstSegmentIndex);
            Node outgoing = nodes.get(loop.secondSegmentIndex);

            if (incoming.graph != outgoing.graph) {
                throw new IllegalStateException(
                    "Both intersection nodes must be in the same graph"
                );
            }

            Segment2 firstSegment = incoming.segment;
            Segment2 secondSegment = outgoing.segment;

            /*
                Первый сегмент (входящий в петлю) обрезается по точку пересечения,
                образуя два сегмента:
             */
            Segment2 firstHead = new Segment2(firstSegment.a, intersection);
            Segment2 firstTail = new Segment2(intersection, firstSegment.b);

            /*
                Второй сегмент (исходящий из петли) также обрезается по точку пересечения,
                образуя два сегмента:
             */
            Segment2 secondHead = new Segment2(secondSegment.a, intersection);
            Segment2 secondTail = new Segment2(intersection, secondSegment.b);

            /*
                Делаем новый кольцевой граф и два дополнительных узла: первый образуется из
                хвоста первого сегмента, а второй - из головы второго сегмента.
             */
            Graph graph = new Graph();
            Node begin = new Node(firstTail);
            Node end = new Node(secondHead);
            graph.begin = begin;
            graphs.add(graph);

            graph.parent = incoming.graph;
            begin.childGraph = incoming.childGraph;
            incoming.childGraph = graph;

            /*
                Меняем указатели, что и разделяет исходный граф на два.
             */
            outgoing.previous.next = end;
            incoming.next.previous = begin;
            begin.previous = end;
            begin.next = incoming.next;
            end.previous = outgoing.previous;
            end.next = begin;
            incoming.next = outgoing;
            outgoing.previous = incoming;

            /*
                Выставляем новые сегменты во входящий и исходящий узлы.
             */
            incoming.segment = firstHead;
            outgoing.segment = secondTail;

            /*
                Выставляем правильный референс на граф
             */
            Node node = begin;
            do {
                node.graph = graph;
                if (node.childGraph != null) {
                    node.childGraph.parent = graph;
                }
                node = node.next;
            } while (node != end);
        }

        /*
            Формируем результат из полученных графов.
         */
        List<Contour> offsetContours = new ArrayList<>();
        for (Graph graph : graphs) {
            /*
                Петли, образованные нечетным количеством пересечений отбрасываются.
             */
            if (graph.getCountOfIntersections() % 2 != 0) {
                mapAllPointsToSinglePoint(graph, pointsMapping);
                continue;
            }

            /*
                Петли с количеством точек меньше 3 отбрасываются.
             */
            List<Point2> points = graph.toPoints();
            if (points.size() < 3) {
                mapAllPointsToSinglePoint(graph, pointsMapping);
                continue;
            }

            /*
                Петли с ничтожной площадью отбрасываются.
             */
            Contour contour = new Contour(points);
            double area = Math.abs(contour.getSignedArea());
            if (area < minContourArea) {
                mapAllPointsToSinglePoint(graph, pointsMapping);
                continue;
            }

            offsetContours.add(contour);
        }

        List<OffsetResult> resultingList = new ArrayList<>();
        for(Contour contour : offsetContours) {
            OffsetResult result = new OffsetResult(
                originalContour,
                contour,
                pointsMapping.offsetToOriginal
            );
            resultingList.add(result);
        }

        return resultingList;
    }

    /**
     * Сопоставляет все точки исходного графа с одной точкой удаляемого графа -
     * с его начальной точкой.
     */
    private static void mapAllPointsToSinglePoint(Graph graph, PointsMapping mapping) {
        Node node = graph.begin;
        Set<Point2> simplified = new HashSet<>();
        do {
            simplified.addAll(mapping.offsetToOriginal.getOrDefault(node.segment.a, Set.of()));
            node = node.next;
        } while(node != graph.begin);
        for (Point2 point : simplified) {
            mapping.removeOriginal(point);
            mapping.addPairOriginalToOffset(point, graph.begin.segment.a);
        }
    }

    /**
     * Вспомогательный класс для сопоставления точек.
     */
    private static class PointsMapping {
        final Map<Point2, Set<Point2>> simplifiedToOriginal;
        final Map<Point2, Set<Point2>> originalToOffset;
        final Map<Point2, Set<Point2>> offsetToOriginal;

        PointsMapping(Map<Point2, Set<Point2>> simplifiedToOriginal) {
            this.simplifiedToOriginal = simplifiedToOriginal;
            this.originalToOffset = new HashMap<>();
            this.offsetToOriginal = new HashMap<>();
        }

        public void addPairSimplifiedToOffset(Point2 simplified, Point2 offset) {
            Set<Point2> original = simplifiedToOriginal.get(simplified);
            if (original == null) {
                throw new IllegalStateException("Can't find the original points");
            }
            for (Point2 point : original) {
                originalToOffset.computeIfAbsent(point, x -> new HashSet<>()).add(offset);
            }
            offsetToOriginal.computeIfAbsent(offset, x -> new HashSet<>()).addAll(original);
        }

        public void removeOriginal(Point2 original) {
            Set<Point2> offset = originalToOffset.remove(original);
            if (offset != null) {
                for (Point2 point : offset) {
                    offsetToOriginal.remove(point);
                }
            }
        }

        public void addPairOriginalToOffset(Point2 original, Point2 offset) {
            originalToOffset.computeIfAbsent(original, x -> new HashSet<>()).add(offset);
            offsetToOriginal.computeIfAbsent(offset, x -> new HashSet<>()).add(original);
        }
    }

    /**
     * Кольцевой граф, содержащий сегменты контура.
     */
    private static class Graph {
        Graph parent;
        Node begin;

        List<Point2> toPoints() {
            final List<Point2> list = new ArrayList<>();
            Node node = begin;
            do {
                list.add(node.segment.a);
                node = node.next;
            } while (node != begin);
            return list;
        }

        /**
         * Возвращает число пересечений исходного контура, которые образуют эту петлю.
         * Считается по количеству родительских графов.
         */
        int getCountOfIntersections() {
            int count = 0;
            Graph graph = parent;
            while (graph != null) {
                count++;
                graph = graph.parent;
            }
            return count;
        }
    }

    /**
     * Узел графа, содержащего сегменты контура.
     */
    private static class Node {
        Graph graph;
        Graph childGraph; // граф, полученный в этой точке в результате разделения
        Segment2 segment;
        Node previous;
        Node next;

        Node(Segment2 segment) {
            this.segment = segment;
        }
    }
}
