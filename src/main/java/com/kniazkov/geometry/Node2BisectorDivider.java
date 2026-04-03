package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

        return List.of();
    }
}
