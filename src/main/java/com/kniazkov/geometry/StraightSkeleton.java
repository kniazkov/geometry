package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Прямолинейный скелет многоугольного контура.
 *
 * Скелет состоит из:
 * - вершин событий и листьев
 * - ребер, соединяющих вершины
 * - граней, каждая из которых обычно соответствует одному исходному ребру контура
 *
 * Класс предназначен для хранения уже построенного результата.
 */
public class StraightSkeleton {
    public final List<SkeletonVertex> vertices;
    public final List<SkeletonEdge> edges;
    public final List<SkeletonFace> faces;

    private List<Segment2> segments;


    public StraightSkeleton(
        List<SkeletonVertex> vertices,
        List<SkeletonEdge> edges,
        List<SkeletonFace> faces
    ) {
        this.vertices = List.copyOf(vertices);
        this.edges = List.copyOf(edges);
        this.faces = List.copyOf(faces);
    }

    /**
     * Возвращает геометрию скелета как набор отрезков.
     *
     * Это удобно для отрисовки, если внешний код умеет рисовать список Segment2.
     */
    public List<Segment2> toSegments() {
        if (segments == null) {
            List<Segment2> result = new ArrayList<>(edges.size());

            for (SkeletonEdge edge : edges) {
                result.add(new Segment2(edge.start.point, edge.end.point));
            }

            segments = Collections.unmodifiableList(result);
        }

        return segments;
    }
}
