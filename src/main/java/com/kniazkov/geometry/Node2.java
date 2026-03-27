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
}