package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Результат смещения контура.
 *
 * Хранит:
 * - оригинальный контур
 * - смещенный контур
 * - отображение индексов оригинального контура в индексы смещенного
 * - отображение индексов смещенного контура в индексы оригинального
 *
 * Одной точке исходного контура может соответствовать несколько точек смещенного контура.
 * Одной точке смещенного контура может соответствовать несколько точек исходного контура.
 */
public class OffsetResult {
    /**
     * Оригинальный контур.
     */
    public final Contour originalContour;

    /**
     * Смещенный контур.
     */
    public final Contour contour;

    /**
     * original index -> ordered set of offset indices
     */
    public final Map<Integer, SortedSet<Integer>> originalToOffset;

    /**
     * offset index -> ordered set of original indices
     */
    public final Map<Integer, SortedSet<Integer>> offsetToOriginal;


    public OffsetResult(
            Contour originalContour,
            Contour offsetContour,
            Map<Point2, Set<Point2>> offsetToOriginalPoints
    ) {
        this.originalContour = originalContour;
        this.contour = offsetContour;

        Map<Integer, SortedSet<Integer>> originalToOffsetIndices = new HashMap<>();
        Map<Integer, SortedSet<Integer>> offsetToOriginalIndices = new HashMap<>();

        for (int offsetIndex = 0; offsetIndex < offsetContour.points.size(); offsetIndex++) {
            Point2 offsetPoint = offsetContour.points.get(offsetIndex);
            Set<Point2> originalPoints = offsetToOriginalPoints.computeIfAbsent(offsetPoint, x -> Set.of());
            Set<Integer> originalIndices = offsetToOriginalIndices.computeIfAbsent(offsetIndex, x -> new TreeSet<>());
            for (Point2 originalPoint : originalPoints) {
                int originalIndex = originalContour.getPointIndex(originalPoint);
                if (originalIndex < 0) {
                    throw new IllegalStateException("Original point does not belong to original contour");
                }
                originalToOffsetIndices.computeIfAbsent(originalIndex, x -> new TreeSet<>()).add(offsetIndex);
                originalIndices.add(originalIndex);
            }
        }

        this.originalToOffset = unmodifiableIndexMap(originalToOffsetIndices);
        this.offsetToOriginal = unmodifiableIndexMap(offsetToOriginalIndices);
    }

    /**
     * Делает карту и ее множества неизменяемыми.
     */
    private static Map<Integer, SortedSet<Integer>> unmodifiableIndexMap(Map<Integer, SortedSet<Integer>> source) {
        Map<Integer, SortedSet<Integer>> result = new HashMap<>(source.size());
        for (Map.Entry<Integer, SortedSet<Integer>> entry : source.entrySet()) {
            result.put(
                entry.getKey(),
                Collections.unmodifiableSortedSet(entry.getValue())
            );
        }
        return Collections.unmodifiableMap(result);
    }
}
