package com.kniazkov.geometry;

import java.util.*;

/**
 * Пространственный индекс сегментов по грубым ячейкам.
 *
 * Каждый сегмент добавляется в карту дважды:
 * по ячейке первой точки и по ячейке второй точки.
 *
 * Поиск работает так:
 * - сначала ищем сегменты в ячейке самой точки
 * - если там ничего нет, ищем в 8 соседних ячейках
 */
public class SegmentMap {
    private final Map<Cell, Set<Segment2>> map = new HashMap<>();


    public SegmentMap(List<Segment2> segments) {
        for (Segment2 segment : segments) {
            add(segment.a.toCell(), segment);
            add(segment.b.toCell(), segment);
        }
    }

    /**
     * Ищет сегменты-кандидаты для точки.
     *
     * Сначала возвращаются сегменты из той же ячейки.
     * Если в этой ячейке ничего нет, тогда ищутся сегмент в соседних ячейках.
     */
    public Set<Segment2> find(Point2 point) {
        Cell cell = point.toCell();

        Set<Segment2> own = map.get(cell);
        if (own != null && !own.isEmpty()) {
            return Collections.unmodifiableSet(own);
        }

        Set<Segment2> result = new HashSet<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                Set<Segment2> neighbor = map.get(cell.offset(dx, dy));
                if (neighbor != null) {
                    result.addAll(neighbor);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Добавляет сегмент в индекс по указанной ячейке.
     */
    private void add(Cell cell, Segment2 segment) {
        map.computeIfAbsent(cell, key -> new HashSet<>()).add(segment);
    }
}
