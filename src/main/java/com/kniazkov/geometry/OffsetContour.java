package com.kniazkov.geometry;

import java.util.Map;

/**
 * Результат смещения контура.
 *
 * Хранит:
 * - сам смещенный контур
 * - отображение индексов оригинального контура в индексы смещенного
 * - отображение индексов смещенного контура в индексы оригинального
 *
 * Не каждая вершина оригинального контура обязана иметь соответствие
 * в смещенном контуре, и наоборот.
 */
public class OffsetContour {
    public final Contour contour;

    /**
     * original index -> offset index
     */
    public final Map<Integer, Integer> originalToOffset;

    /**
     * offset index -> original index
     */
    public final Map<Integer, Integer> offsetToOriginal;


    public OffsetContour(
        Contour contour,
        Map<Integer, Integer> originalToOffset,
        Map<Integer, Integer> offsetToOriginal
    ) {
        this.contour = contour;
        this.originalToOffset = Map.copyOf(originalToOffset);
        this.offsetToOriginal = Map.copyOf(offsetToOriginal);
    }
}
