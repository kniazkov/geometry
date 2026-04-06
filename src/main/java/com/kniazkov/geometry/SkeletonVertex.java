package com.kniazkov.geometry;

/**
 * Вершина прямолинейного скелета.
 *
 * Вершина может быть:
 * - листом, соответствующим исходной вершине контура
 * - внутренней вершиной события
 * - корневой вершиной, если схлопывание заканчивается в одной точке
 *
 * Поле time задает "время" offset-процесса,
 * в которое эта вершина появилась.
 */
public class SkeletonVertex {
    public enum Type {
        LEAF,
        EDGE_EVENT,
        SPLIT_EVENT,
        MULTI_EVENT,
        ROOT
    }

    public final Point2 point;
    public final double time;
    public final Type type;

    /**
     * Индекс исходной вершины контура, если он известен.
     *
     * Для листьев это обычно индекс вершины исходного контура.
     * Для внутренних событий может быть равен -1.
     */
    public final int sourceVertexIndex;


    public SkeletonVertex(Point2 point, double time, Type type) {
        this(point, time, type, -1);
    }

    public SkeletonVertex(Point2 point, double time, Type type, int sourceVertexIndex) {
        this.point = point;
        this.time = time;
        this.type = type;
        this.sourceVertexIndex = sourceVertexIndex;
    }

    /**
     * Возвращает true, если вершина является листом скелета.
     */
    public boolean isLeaf() {
        return type == Type.LEAF;
    }

    @Override
    public String toString() {
        return point.toString();
    }
}
