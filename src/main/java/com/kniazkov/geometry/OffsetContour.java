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
public class OffsetContour {
    /**
     * Оригинальный контур.
     */
    public final Contour original;

    /**
     * Смещенный контур.
     */
    public final Contour offset;

    /**
     * original index -> ordered set of offset indices
     */
    public final Map<Integer, SortedSet<Integer>> originalToOffset;

    /**
     * offset index -> ordered set of original indices
     */
    public final Map<Integer, SortedSet<Integer>> offsetToOriginal;


    private OffsetContour(
            Contour original,
            Contour offset,
            Map<Integer, SortedSet<Integer>> originalToOffset,
            Map<Integer, SortedSet<Integer>> offsetToOriginal
    ) {
        this.original = original;
        this.offset = offset;
        this.originalToOffset = originalToOffset;
        this.offsetToOriginal = offsetToOriginal;
    }

    /**
     * Собирает объект класса OffsetContour.
     */
    static class Builder {
        /**
         * Оригинальный, не упрощенный контур.
         */
        private final Contour originalContour;

        /**
         * Сопоставление точек между оригинальным и упрощенным контуром.
         */
        private final Map<Point2, Set<Point2>> mappingOriginalToSimplified;

        /**
         * Смещенный контур.
         */
        private Contour offsetContour;

        /**
         * Накопленные соответствия между точками.
         *
         * Здесь пока хранятся сами точки, потому что смещенный контур
         * может быть задан позже.
         */
        private final List<PointPair> correspondingPoints = new ArrayList<>();


        Builder(
                Contour originalContour, Map<Point2, Set<Point2>> mappingOriginalToSimplified
        ) {
            this.originalContour = originalContour;
            this.mappingOriginalToSimplified = mappingOriginalToSimplified;
        }

        void setOffsetContour(Contour contour) {
            this.offsetContour = contour;
        }

        /**
         * Добавляет соответствие между точкой упрощенного контура
         * и точкой смещенного контура.
         *
         * Одни и те же точки могут участвовать в нескольких соответствиях.
         */
        void addCorrespondingPoints(Point2 simplifiedPoint, Point2 offsetPoint) {
            correspondingPoints.add(new PointPair(simplifiedPoint, offsetPoint));
        }

        /**
         * Собирает окончательный результат смещения.
         *
         * Во время сборки:
         * - точки превращаются в индексы через Contour.getPointIndex(...)
         * - строятся обе двусторонние карты соответствий
         * - отдельно контролируется, что ни одна точка не была пропущена
         */
        OffsetContour build() {
            if (offsetContour == null) {
                throw new IllegalStateException("Offset contour is not set");
            }

            Map<Integer, SortedSet<Integer>> originalToOffset = new HashMap<>();
            Map<Integer, SortedSet<Integer>> offsetToOriginal = new HashMap<>();

            Set<Point2> unusedOriginalPoints = new HashSet<>(originalContour.points);
            Set<Point2> unusedOffsetPoints = new HashSet<>(offsetContour.points);

            for (PointPair pair : correspondingPoints) {
                int offsetIndex = offsetContour.getPointIndex(pair.offsetPoint);
                if (offsetIndex < 0) {
                    throw new IllegalStateException("Offset point does not belong to offset contour");
                }

                Set<Point2> originalPoints = mappingOriginalToSimplified.get(pair.simplifiedPoint);
                if (originalPoints == null) {
                    originalPoints = Set.of(pair.simplifiedPoint);
                }

                for (Point2 originalPoint : originalPoints) {
                    int originalIndex = originalContour.getPointIndex(originalPoint);
                    if (originalIndex < 0) {
                        throw new IllegalStateException("Original point does not belong to original contour");
                    }

                    addIndexMapping(originalToOffset, originalIndex, offsetIndex);
                    addIndexMapping(offsetToOriginal, offsetIndex, originalIndex);

                    unusedOriginalPoints.remove(originalPoint);
                    unusedOffsetPoints.remove(pair.offsetPoint);
                }
            }

            if (!unusedOriginalPoints.isEmpty()) {
                throw new IllegalStateException("Some original contour points have no correspondence");
            }

            if (!unusedOffsetPoints.isEmpty()) {
                throw new IllegalStateException("Some offset contour points have no correspondence");
            }

            return new OffsetContour(
                    originalContour,
                    offsetContour,
                    freezeIndexMap(originalToOffset),
                    freezeIndexMap(offsetToOriginal)
            );
        }

        /**
         * Добавляет одно индексное соответствие в карту.
         */
        private static void addIndexMapping(
                Map<Integer, SortedSet<Integer>> map,
                int key,
                int value
        ) {
            map.computeIfAbsent(key, ignored -> new TreeSet<>()).add(value);
        }

        /**
         * Делает карту и ее множества неизменяемыми.
         */
        private static Map<Integer, SortedSet<Integer>> freezeIndexMap(
                Map<Integer, SortedSet<Integer>> source
        ) {
            Map<Integer, SortedSet<Integer>> result = new HashMap<>(source.size());

            for (Map.Entry<Integer, SortedSet<Integer>> entry : source.entrySet()) {
                result.put(
                        entry.getKey(),
                        Collections.unmodifiableSortedSet(entry.getValue())
                );
            }

            return Collections.unmodifiableMap(result);
        }

        /**
         * Одна накопленная пара соответствующих точек.
         */
        private record PointPair(Point2 simplifiedPoint, Point2 offsetPoint) {
        }
    }
}
