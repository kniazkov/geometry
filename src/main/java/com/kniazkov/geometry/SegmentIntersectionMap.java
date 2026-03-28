package com.kniazkov.geometry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Пространственный индекс сегментов для ускорения поиска возможных пересечений.
 *
 * Для каждого сегмента в индекс сохраняется не сам сегмент, а его индекс
 * в исходном списке.
 *
 * Сегмент добавляется в две карты:
 * - по оси X
 * - по оси Y
 *
 * При поиске кандидатов для заданного сегмента:
 * - оценивается его протяженность в ячейках по X и по Y
 * - выбирается более узкая ось
 * - из соответствующей карты собираются индексы сегментов-кандидатов
 *
 * Это не гарантирует реальное пересечение, но заметно уменьшает
 * число точных геометрических проверок.
 */
public class SegmentIntersectionMap {
    private final Map<Integer, Set<Integer>> xMap = new HashMap<>();
    private final Map<Integer, Set<Integer>> yMap = new HashMap<>();


    public SegmentIntersectionMap(List<Segment2> segments) {
        for (int i = 0; i < segments.size(); i++) {
            add(i, segments.get(i));
        }
    }

    /**
     * Возвращает набор индексов сегментов, которые теоретически могут
     * пересекаться с указанным сегментом.
     */
    public Set<Integer> find(Segment2 segment) {
        Cell a = segment.a.toCell();
        Cell b = segment.b.toCell();

        int minX = Math.min(a.x, b.x);
        int maxX = Math.max(a.x, b.x);
        int minY = Math.min(a.y, b.y);
        int maxY = Math.max(a.y, b.y);

        int xSpan = maxX - minX;
        int ySpan = maxY - minY;

        Set<Integer> result = new TreeSet<>();  // даст нам упорядоченный набор сегментов

        /*
            Берем более узкую ось, чтобы получить меньше кандидатов.
         */
        if (xSpan <= ySpan) {
            for (int x = minX - 1; x <= maxX + 1; x++) {
                Set<Integer> bucket = xMap.get(x);
                if (bucket != null) {
                    result.addAll(bucket);
                }
            }
        } else {
            for (int y = minY - 1; y <= maxY + 1; y++) {
                Set<Integer> bucket = yMap.get(y);
                if (bucket != null) {
                    result.addAll(bucket);
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Добавляет индекс сегмента в обе карты:
     * - по диапазону ячеек X
     * - по диапазону ячеек Y
     *
     * Диапазон расширяется на одну ячейку в обе стороны,
     * чтобы захватывать близкие случаи около границ.
     */
    private void add(int index, Segment2 segment) {
        Cell a = segment.a.toCell();
        Cell b = segment.b.toCell();

        int minX = Math.min(a.x, b.x);
        int maxX = Math.max(a.x, b.x);
        int minY = Math.min(a.y, b.y);
        int maxY = Math.max(a.y, b.y);

        for (int x = minX - 1; x <= maxX + 1; x++) {
            addToMap(xMap, x, index);
        }

        for (int y = minY - 1; y <= maxY + 1; y++) {
            addToMap(yMap, y, index);
        }
    }

    /**
     * Добавляет индекс сегмента в одну корзину карты.
     */
    private static void addToMap(Map<Integer, Set<Integer>> map, int key, int index) {
        map.computeIfAbsent(key, ignored -> new HashSet<>()).add(index);
    }
}
