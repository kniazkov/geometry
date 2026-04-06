package com.kniazkov.geometry;

import java.util.List;

/**
 * Грань прямолинейного скелета.
 *
 * Грань соответствует одному исходному ребру контура и содержит набор ребер скелета,
 * ограничивающих эту грань.
 *
 * Здесь хранится только состав грани, без сложной топологии.
 * Этого достаточно, чтобы знать происхождение частей offset-контура.
 */
public class SkeletonFace {
    /**
     * Индекс исходного ребра контура, которому соответствует грань.
     */
    public final int sourceEdgeIndex;

    /**
     * Индексы ребер скелета, входящих в границу грани.
     *
     * Предполагается, что порядок уже задан внешним кодом,
     * если он вообще нужен.
     */
    public final List<Integer> edgeIndices;


    public SkeletonFace(int sourceEdgeIndex, List<Integer> edgeIndices) {
        this.sourceEdgeIndex = sourceEdgeIndex;
        this.edgeIndices = List.copyOf(edgeIndices);
    }
}