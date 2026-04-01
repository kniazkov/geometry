package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

/**
 * Одно пересечение двух контуров.
 *
 * Хранит:
 * - геометрический результат пересечения двух сегментов
 * - индекс сегмента первого контура
 * - индекс сегмента второго контура
 *
 * Геометрический результат может быть:
 * - Point2, если сегменты пересекаются в одной точке
 * - Segment2, если сегменты накладываются общим участком
 */
public class ContourIntersection {
    public final SegmentIntersection intersection;
    public final int firstSegmentIndex;
    public final int secondSegmentIndex;

    public ContourIntersection(
        SegmentIntersection intersection,
        int firstSegmentIndex,
        int secondSegmentIndex
    ) {
        this.intersection = intersection;
        this.firstSegmentIndex = firstSegmentIndex;
        this.secondSegmentIndex = secondSegmentIndex;
    }

    /**
     * Сортирует пересечения по уровню вложенности петель:
     * сначала самые внутренние, затем содержащие их, и так далее.
     */
    public static List<ContourIntersection> sortByNesting(
            List<ContourIntersection> intersections,
            int segmentCount
    ) {
        if (segmentCount <= 0) {
            throw new IllegalArgumentException("segmentCount must be > 0");
        }

        List<LoopInfo> loops = new ArrayList<>(intersections.size());
        for (ContourIntersection ci : intersections) {
            loops.add(new LoopInfo(ci, segmentCount));
        }

        int n = loops.size();

        /*
            children[i] = индексы петель, лежащих внутри петли i
         */
        List<List<Integer>> children = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            children.add(new ArrayList<>());
        }

        /*
            Строим отношение вложенности между всеми парами петель
         */
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                if (contains(loops.get(i), loops.get(j), segmentCount)) {
                    children.get(i).add(j);
                }
            }
        }

        /*
            level = 0 у листьев, level = 1 + max(level(child)) у остальных
         */
        Integer[] memoLevel = new Integer[n];
        for (int i = 0; i < n; i++) {
            loops.get(i).level = calcLevel(i, children, memoLevel);
        }

        /*
            Сначала внутренние петли, потом более внешние
         */
        loops.sort(Comparator
                .comparingInt((LoopInfo x) -> x.level)
                .thenComparingInt(x -> x.length)
                .thenComparingInt(x -> x.intersection.firstSegmentIndex)
                .thenComparingInt(x -> x.intersection.secondSegmentIndex)
        );

        List<ContourIntersection> result = new ArrayList<>(n);
        for (LoopInfo loop : loops) {
            result.add(loop.intersection);
        }
        return result;
    }

    /**
     * Проверяет, лежит ли inner целиком внутри outer.
     *
     * Обе петли рассматриваются как дуги на циклическом контуре:
     * от firstSegmentIndex до secondSegmentIndex вперед по обходу.
     */
    private static boolean contains(LoopInfo outer, LoopInfo inner, int segmentCount) {
        /*
            Смещение начала inner относительно начала outer вдоль цикла
         */
        int innerStartRel = cyclicDistance(outer.start, inner.start, segmentCount);

        /*
            Конец inner в той же "развернутой" системе координат
         */
        int innerEndRel = innerStartRel + inner.length;

        /*
            Требуем, чтобы inner полностью помещалась в outer, но при этом не совпадала с ней полностью
         */
        return innerStartRel >= 0
                && innerEndRel <= outer.length
                && !(innerStartRel == 0 && inner.length == outer.length);
    }

    /**
     * Вычисляет уровень вложенности петли.
     *
     * 0 - если внутри нет других петель
     * 1 + максимум по дочерним - иначе
     */
    private static int calcLevel(int v, List<List<Integer>> children, Integer[] memo) {
        if (memo[v] != null) {
            return memo[v];
        }

        int maxChild = -1;
        for (int child : children.get(v)) {
            maxChild = Math.max(maxChild, calcLevel(child, children, memo));
        }

        memo[v] = maxChild + 1;
        return memo[v];
    }

    /**
     * Расстояние по циклу вперед от from до to.
     * Результат всегда в диапазоне [0, segmentCount).
     */
    private static int cyclicDistance(int from, int to, int segmentCount) {
        int d = to - from;
        if (d < 0) {
            d += segmentCount;
        }
        return d;
    }

    /**
     * Вспомогательное описание петли, полученной из одного самопересечения.
     */
    private static final class LoopInfo {
        final ContourIntersection intersection;
        final int start;
        final int end;
        final int length;

        /**
         * Уровень вложенности: 0 у самых внутренних петель
         */
        int level;

        LoopInfo(ContourIntersection intersection, int segmentCount) {
            this.intersection = intersection;
            this.start = intersection.firstSegmentIndex;
            this.end = intersection.secondSegmentIndex;
            this.length = cyclicDistance(start, end, segmentCount);
        }
    }
}
