package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Узел двусвязного списка, содержащий точку контура и предвычисленную геометрическую информацию о ней.
 *
 * Класс мутабельный, потому что в дальнейшем узлы могут добавляться, удаляться и переподключаться.
 */
public class Node2 {
    /**
     * Если угол отличается от pi меньше чем на это значение,
     * он считается развернутым.
     */
    public static final double STRAIGHT_ANGLE_EPSILON = 1e-3;

    public final Point2 point;

    public Node2 prev;
    public Node2 next;

    public double distanceToPrev;
    public double distanceToNext;

    /**
     * Угол в диапазоне [0, pi].
     */
    public double angle;

    /**
     * true, если угол внешний.
     */
    public boolean outer;

    /**
     * true, если угол считается развернутым.
     */
    public boolean straight;


    public Node2(Point2 point) {
        this.point = point;
    }

    /**
     * Пересчитывает всю геометрическую информацию узла
     * по его текущим соседям.
     */
    public void update() {
        distanceToPrev = point.distanceTo(prev.point);
        distanceToNext = point.distanceTo(next.point);

        Vector2 toPrev = prev.point.subtract(point);
        Vector2 toNext = next.point.subtract(point);

        double prevLength = toPrev.length();
        double nextLength = toNext.length();

        double cos = toPrev.dot(toNext) / (prevLength * nextLength);
        cos = Math.max(-1.0, Math.min(1.0, cos));
        angle = Math.acos(cos);

        double cross = toPrev.cross(toNext);

        /*
            Для контура, ориентированного против часовой стрелки: внешний угол находится слева, то есть соответствует
            отрицательному повороту от вектора на prev к вектору на next.
         */
        outer = cross < 0.0;

        straight = Math.PI - angle <= STRAIGHT_ANGLE_EPSILON;
    }

    /**
     * Удаляет текущий узел из двусвязного циклического списка.
     *
     * После удаления соседние узлы соединяются друг с другом и пересчитывают свою геометрическую информацию.
     */
    public void remove() {
        prev.next = next;
        next.prev = prev;

        prev.update();
        next.update();

        prev = this;
        next = this;
        distanceToPrev = 0;
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

        return list;
    }

    @FunctionalInterface
    public interface RemoveCriteria {
        boolean shouldRemove(Node2 node);
    }

    /**
     * Удаляет из двусвязного циклического списка все узлы,
     * удовлетворяющие заданному критерию.
     *
     * Возвращает ссылку на любой оставшийся узел списка.
     */
    public static Node2 removeNodesByCriteria(Node2 start, RemoveCriteria criteria) {
        while (criteria.shouldRemove(start)) {
            start = start.next;
        }

        Node2 node = start;
        do {
            Node2 next = node.next;
            if (criteria.shouldRemove(node)) {
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
}
