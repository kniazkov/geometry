package com.kniazkov.geometry;

/**
 * Ребро прямолинейного скелета.
 *
 * Ребро соединяет две вершины скелета и соответствует траектории
 * движения одной вершины фронта между двумя событиями.
 *
 * Слева и справа от ребра могут находиться грани скелета.
 * Обычно каждая грань связана с одним исходным ребром контура.
 */
public class SkeletonEdge {
    public final SkeletonVertex start;
    public final SkeletonVertex end;

    /**
     * Индекс грани слева от ребра или -1, если он не задан.
     */
    public final int leftFaceIndex;

    /**
     * Индекс грани справа от ребра или -1, если он не задан.
     */
    public final int rightFaceIndex;


    public SkeletonEdge(SkeletonVertex start, SkeletonVertex end) {
        this(start, end, -1, -1);
    }

    public SkeletonEdge(
        SkeletonVertex start,
        SkeletonVertex end,
        int leftFaceIndex,
        int rightFaceIndex
    ) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Skeleton edge vertices must not be null");
        }

        this.start = start;
        this.end = end;
        this.leftFaceIndex = leftFaceIndex;
        this.rightFaceIndex = rightFaceIndex;
    }

    /**
     * Возвращает геометрию ребра как обычный отрезок.
     */
    public Segment2 toSegment() {
        return new Segment2(start.point, end.point);
    }

    /**
     * Возвращает true, если ребро вырождено в точку.
     */
    public boolean isDegenerate() {
        return start.point.approximatelyEquals(end.point);
    }
}
