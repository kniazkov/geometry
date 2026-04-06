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
    private List<SkeletonVertex> leafVertices;


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
                result.add(edge.toSegment());
            }

            segments = Collections.unmodifiableList(result);
        }

        return segments;
    }

    /**
     * Возвращает список листовых вершин скелета.
     *
     * Обычно это вершины, соответствующие исходным вершинам контура.
     */
    public List<SkeletonVertex> getLeafVertices() {
        if (leafVertices == null) {
            List<SkeletonVertex> result = new ArrayList<>();

            for (SkeletonVertex vertex : vertices) {
                if (vertex.isLeaf()) {
                    result.add(vertex);
                }
            }

            leafVertices = Collections.unmodifiableList(result);
        }

        return leafVertices;
    }

    /**
     * Возвращает новый скелет без вырожденных ребер.
     *
     * Вырожденным считается ребро, у которого начало и конец совпадают
     * с учетом стандартного допуска.
     *
     * Список вершин и граней сохраняется как есть.
     * Если позже понадобится "сжать" и их тоже, это уже отдельная операция.
     */
    public StraightSkeleton removeDegenerateEdges() {
        List<SkeletonEdge> result = new ArrayList<>(edges.size());

        for (SkeletonEdge edge : edges) {
            if (!edge.isDegenerate()) {
                result.add(edge);
            }
        }

        if (result.size() == edges.size()) {
            return this;
        }

        return new StraightSkeleton(vertices, result, faces);
    }

    /**
     * Возвращает список корневых вершин скелета.
     *
     * Обычно это вершины, в которых завершилось схлопывание
     * одной или нескольких компонент фронта.
     */
    public List<SkeletonVertex> getRootVertices() {
        List<SkeletonVertex> result = new ArrayList<>();

        for (SkeletonVertex vertex : vertices) {
            if (vertex.type == SkeletonVertex.Type.ROOT) {
                result.add(vertex);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Возвращает список внутренних вершин-событий.
     *
     * Листья и корневые вершины в результат не включаются.
     */
    public List<SkeletonVertex> getEventVertices() {
        List<SkeletonVertex> result = new ArrayList<>();

        for (SkeletonVertex vertex : vertices) {
            if (vertex.type == SkeletonVertex.Type.EDGE_EVENT
                || vertex.type == SkeletonVertex.Type.SPLIT_EVENT
                || vertex.type == SkeletonVertex.Type.MULTI_EVENT) {
                result.add(vertex);
            }
        }

        return Collections.unmodifiableList(result);
    }
}
