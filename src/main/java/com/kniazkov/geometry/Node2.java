package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Узел двусвязного списка, содержащий точку контура
 * и предвычисленную геометрическую информацию о ней.
 *
 * Класс мутабельный, так что в дальнейшем узлы могут добавляться, удаляться и переподключаться.
 */
public class Node2 {
    /**
     * Если угол отличается от pi меньше чем на это значение,
     * он считается развернутым.
     */
    public static final double STRAIGHT_ANGLE_EPSILON = 1e-3;

    public final Point2 point;

    private Node2 previous;
    private Node2 next;

    private double distanceToPrevious;
    private double distanceToNext;

    /**
     * Угол в диапазоне [0, pi].
     */
    private double angle;

    /**
     * true, если угол внешний.
     */
    private boolean outer;

    /**
     * true, если угол считается развернутым.
     */
    private boolean straight;


    public Node2(Point2 point) {
        this.point = point;
    }

    public Node2 getPrevious() {
        return previous;
    }

    public Node2 getNext() {
        return next;
    }

    public double getDistanceToPrevious() {
        return distanceToPrevious;
    }

    public double getDistanceToNext() {
        return distanceToNext;
    }

    public double getAngle() {
        return angle;
    }

    public boolean isOuter() {
        return outer;
    }

    public boolean isStraight() {
        return straight;
    }

    /**
     * Создает двусвязный циклический список узлов по списку точек
     * и рассчитывает геометрические параметры каждого узла.
     */
    public static Node2 fromPoints(List<Point2> points) {
        if (points.size() < 3) {
            throw new IllegalArgumentException("Contour must contain at least 3 points");
        }

        List<Node2> nodes = new ArrayList<>(points.size());

        for (Point2 point : points) {
            nodes.add(new Node2(point));
        }

        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            Node2 current = nodes.get(i);
            current.previous = nodes.get((i - 1 + size) % size);
            current.next = nodes.get((i + 1) % size);
        }

        for (Node2 node : nodes) {
            node.update();
        }

        return nodes.get(0);
    }

    /**
     * Удаляет текущий узел из двусвязного циклического списка.
     *
     * После удаления соседние узлы соединяются друг с другом и пересчитывают
     * свою геометрическую информацию.
     */
    public void remove() {
        previous.next = next;
        next.previous = previous;

        previous.update();
        next.update();

        previous = this;
        next = this;
        distanceToPrevious = 0;
        distanceToNext = 0;
        angle = 0;
        outer = false;
        straight = false;
    }

    /**
     * Собирает точки кольца в список в порядке обхода.
     */
    public static List<Point2> toPoints(Node2 start) {
        List<Point2> list = new ArrayList<>();
        Node2 node = start;

        do {
            list.add(node.point);
            node = node.next;
        } while (node != start);

        return Collections.unmodifiableList(list);
    }

    /**
     * Удаляет из двусвязного циклического списка все узлы,
     * удовлетворяющие заданному критерию.
     *
     * Возвращает ссылку на любой оставшийся узел списка.
     */
    public static Node2 removeNodesByCriteria(Node2 start, NodeSelectionCriteria criteria) {
        while (criteria.test(start)) {
            start = start.next;
        }

        Node2 node = start;
        do {
            Node2 next = node.next;
            if (criteria.test(node)) {
                node.remove();
            }
            node = next;
        } while (node != start);

        return start;
    }

    /**
     * Удаляет из двусвязного циклического списка все узлы с развернутым углом.
     *
     * Возвращает ссылку на любой оставшийся узел списка.
     */
    public static Node2 removeStraight(Node2 start) {
        return removeNodesByCriteria(start, node -> node.straight);
    }

    /**
     * Удаляет узлы, у которых предыдущий сегмент короче заданного порога,
     * но только если угол в узле достаточно тупой.
     *
     * Острые и почти прямые углы не удаляются, чтобы не портить основную форму.
     */
    public static Node2 removeShortSegments(Node2 start, double minDistance) {
        final double minAngle = 2.0 * Math.PI / 3.0;

        return removeNodesByCriteria(
            start,
            node -> node.angle > minAngle && node.distanceToPrevious < minDistance
        );
    }

    /**
     * Удаляет узлы, у которых тип угла отличается от типов углов обоих соседей,
     * при этом оба соседа имеют одинаковый тип угла, а сам угол узла
     * близок к развернутому.
     *
     * Это помогает убирать одиночные "переключатели" кривизны,
     * которые обычно являются артефактами ломаной аппроксимации.
     */
    public static Node2 removeOddAngleType(Node2 start) {
        final double minAngle = 8.0 * Math.PI / 9.0; // 160 degrees

        return removeNodesByCriteria(start, node ->
            node.angle > minAngle &&
                node.previous.outer == node.next.outer &&
                node.outer != node.previous.outer
        );
    }

    /**
     * Пересчитывает всю геометрическую информацию узла по его текущим соседям.
     */
    private void update() {
        Vector2 toPrevious = previous.point.subtract(point);
        Vector2 toNext = next.point.subtract(point);

        distanceToPrevious = toPrevious.length();
        distanceToNext = toNext.length();

        if (distanceToPrevious <= Point2.EPSILON || distanceToNext <= Point2.EPSILON) {
            angle = Math.PI;
            outer = false;
            straight = true;
            return;
        }

        double cos = toPrevious.dot(toNext) / (distanceToPrevious * distanceToNext);
        cos = Math.max(-1.0, Math.min(1.0, cos));
        angle = Math.acos(cos);

        double cross = toPrevious.cross(toNext);

        /*
            Для контура, ориентированного против часовой стрелки: внешний угол находится слева,
            то есть соответствует отрицательному повороту от вектора на prev к вектору на next.
         */
        outer = cross < 0.0;
        straight = Math.PI - angle <= STRAIGHT_ANGLE_EPSILON;
    }
}
