package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Делит кольцо узлов по "плохим" точкам, найденным с помощью биссектрис.
 *
 * Класс используется как предварительный этап перед смещением трассы внутрь.
 * Для каждого узла кольца рассматривается отрезок, построенный по биссектрисе
 * угла в этом узле и направленный внутрь трассы.
 *
 * Если такой отрезок пересекает саму трассу, это означает, что при дальнейшем
 * смещении внутрь соответствующий участок почти наверняка схлопнется
 * или породит лишние самопересечения.
 *
 * В этом случае трасса мысленно разрезается в точке пересечения,
 * а сама точка считается "плохой".
 *
 * После обработки всех узлов исходное кольцо разбивается по найденным
 * плохим точкам на несколько частей.
 * Из слишком коротких частей результат не собирается.
 *
 * На выходе возвращается список новых колец узлов и отображение
 * между точками результата и исходными точками, из которых они получены.
 */
public class Node2BisectorDivider {
    private final double bisectLength;


    public Node2BisectorDivider(double bisectLength) {
        this.bisectLength = bisectLength;
    }

    /**
     * Делит кольцо узлов на несколько частей по точкам,
     * найденным пересечением внутренних биссектрис с трассой.
     */
    public List<Node2ProcessingResult> divide(Node2 begin) {
        List<Node2> nodes = Node2.toNodes(begin);
        ContourIntersectionFinder finder = new ContourIntersectionFinder(
            Node2.toSegments(begin)
        );
        Set<Node2> bad = new HashSet<>();

        for (int i = 0; i < nodes.size(); i++) {
            int j = (i - 1 + nodes.size()) % nodes.size();

            /*
                Строим биссектрису и находим ее пересечение с контуром.
             */
            Node2 node = nodes.get(i);
            Segment2 bisect = node.buildBisectorSegment(bisectLength * 2);
            List<ContourIntersection> intersections = finder.findIntersections(bisect);

            for (ContourIntersection intersection : intersections) {
                /*
                    Два индекса надо удалить - текущий сегмент (узел) и предыдущий, так как
                    они сами по себе входят в пересечение.
                 */
                if (intersection.firstSegmentIndex == i || intersection.firstSegmentIndex == j) {
                    continue;
                }

                if (!(intersection.intersection instanceof Point2 point)) {
                    continue;
                }

                /*
                    Если мы здесь, пересечение с контуром найдено, и оно дает два "плохих" узла:
                    текущий узел и узел, который образуется в результате пересечения.
                 */
                bad.add(node);
                Node2 inserted = new Node2(point);
                nodes.get(intersection.firstSegmentIndex).insertAfter(inserted);
                bad.add(inserted);
            }
        }

        /*
            Если нет "плохих" точек, возвращается исходное кольцо.
         */
        if (bad.isEmpty()) {
            return List.of(new Node2ProcessingResult(begin, Map.of()));
        }

        /*
            Если есть "плохие" точки, кольцо разделяется на секции,
            которые не содержат таких точек.
         */
        List<Section> sections = splitToSections(begin, bad);

        /*
            Вырезаем кольца по сегментам.
         */
        List<Node2> cuttings = new ArrayList<>();
        for (Section section : sections) {
            section.first.cutTo(section.last);
            cuttings.add(section.first);
        }

        /*
            Формируем результат.
         */
        List<Node2ProcessingResult> result = new ArrayList<>();
        for (Node2 node : cuttings) {
            result.add(new Node2ProcessingResult(node, Map.of()));
        }

        return result;
    }

    /**
     * Разделяет кольцо на сегменты, которые не содержат "плохих" точек.
     */
    private static List<Section> splitToSections(Node2 begin, Set<Node2> bad) {
        List<Section> sections = new ArrayList<>();

        /*
            Ищем любую "плохую" точку, от которой удобно начать обход.
         */
        Node2 startBad = begin;
        while (!bad.contains(startBad)) {
            startBad = startBad.getNext();
        }

        Node2 cursor = startBad;

        do {
            /*
                Пропускаем подряд идущие "плохие" точки.
             */
            while (bad.contains(cursor.getNext())) {
                cursor = cursor.getNext();
                if (cursor == startBad) {
                    break;
                }
            }

            /*
                Если после пропуска снова пришли в стартовую точку,
                значит хороших секций больше нет.
             */
            if (cursor.getNext() == startBad) {
                break;
            }

            /*
                Начинаем секцию сразу после последней "плохой" точки в серии.
             */
            Node2 first = cursor.getNext();
            Node2 last = first;

            /*
                Идем, пока не встретим следующую "плохую" точку.
             */
            while (!bad.contains(last.getNext())) {
                last = last.getNext();
            }

            /*
                Если от начального до конечного узла больше 2 точек, то они могут образовать
                кольцо, и добавляются в список сегментов.
             */
            if (first.numberOfNodesTo(last) >= 2) {
                Section section = new Section(first, last);
                sections.add(section);
            }

            /*
                Переходим к следующей серии "плохих" точек.
             */
            cursor = last.getNext();
        } while (cursor != startBad);
        return sections;
    }

    private static class Section {
        public final Node2 first;
        public final Node2 last;

        private Section(Node2 first, Node2 last) {
            this.first = first;
            this.last = last;
        }
    }
}
