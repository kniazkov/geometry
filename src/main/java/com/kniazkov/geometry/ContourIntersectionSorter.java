package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Анализирует самопересечения замкнутого контура.
 *
 * По переданному контуру и списку его самопересечений класс:
 * - строит список петель
 * - выделяет "плохие" случаи с переплетением интервалов
 * - сортирует нормальные пересечения по уровню вложенности петель
 * - находит индекс сегмента, не входящего ни в одну нормальную петлю
 *
 * Петля задается дугой замкнутого контура:
 * от firstSegmentIndex до secondSegmentIndex вперед по обходу.
 * Если secondSegmentIndex меньше firstSegmentIndex,
 * дуга проходит через конец списка сегментов.
 */
public class ContourIntersectionSorter {
    /**
     * Общее количество сегментов.
     */
    private final int segmentCount;

    /**
     * Список петель, соответствующих самопересечениям.
     *
     * Список уже отсортирован:
     * сначала самые внутренние петли,
     * затем содержащие их, и так далее.
     *
     * Сюда попадают только "нормальные" петли,
     * не участвующие в badCases.
     */
    public final List<LoopInfo> loops;

    /**
     * Группы "плохих" случаев.
     *
     * Каждый элемент списка - связная группа самопересечений,
     * в которой интервалы петель переплетаются
     * (например, A < C < B < D).
     *
     * Такие случаи исключаются из обычной сортировки loops
     * и должны обрабатываться отдельно.
     */
    public final List<List<ContourIntersection>> badCases;

    /**
     * Индекс сегмента, не входящего ни в одну нормальную петлю.
     *
     * Если все сегменты входят хотя бы в одну нормальную петлю,
     * значение равно -1.
     */
    public final int freeSegmentIndex;


    public ContourIntersectionSorter(
        Contour contour,
        List<ContourIntersection> intersections
    ) {
        segmentCount = contour.points.size();

        List<LoopInfo> loops = new ArrayList<>(intersections.size());
        for (ContourIntersection intersection : intersections) {
            loops.add(new LoopInfo(intersection, segmentCount));
        }

        BadCaseBuildResult badCaseBuildResult = buildBadCases(loops);
        this.badCases = List.copyOf(badCaseBuildResult.badCases);

        List<LoopInfo> goodLoops = new ArrayList<>();
        for (int i = 0; i < loops.size(); i++) {
            if (!badCaseBuildResult.badLoopIndices.contains(i)) {
                goodLoops.add(loops.get(i));
            }
        }

        loops = goodLoops;

        int n = loops.size();

        /*
            children[i] = индексы петель, лежащих внутри петли i.
         */
        List<List<Integer>> children = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            children.add(new ArrayList<>());
        }

        /*
            Строим отношение вложенности между всеми парами нормальных петель.
         */
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                if (contains(loops.get(i), loops.get(j))) {
                    children.get(i).add(j);
                }
            }
        }

        /*
            level = 0 у листьев,
            level = 1 + max(level(child)) у остальных.
         */
        Integer[] memoLevel = new Integer[n];
        for (int i = 0; i < n; i++) {
            loops.set(i, loops.get(i).withLevel(calcLevel(i, children, memoLevel)));
        }

        /*
            Сначала внутренние петли, потом более внешние.
         */
        loops.sort(Comparator
            .comparingInt((LoopInfo x) -> x.level)
            .thenComparingInt(x -> x.length)
            .thenComparingInt(x -> x.firstSegmentIndex)
            .thenComparingInt(x -> x.secondSegmentIndex)
        );

        this.loops = List.copyOf(loops);
        freeSegmentIndex = this.loops.isEmpty() ? -1 : findFreeSegmentIndex(this.loops);
    }

    /**
     * Проверяет, лежит ли inner целиком внутри outer.
     *
     * Обе петли рассматриваются как дуги на циклическом контуре:
     * от firstSegmentIndex до secondSegmentIndex вперед по обходу.
     */
    private boolean contains(LoopInfo outer, LoopInfo inner) {
        /*
            Смещение начала inner относительно начала outer вдоль цикла.
         */
        int innerStartRel = cyclicDistance(outer.firstSegmentIndex, inner.firstSegmentIndex, segmentCount);

        /*
            Конец inner в той же развернутой системе координат.
         */
        int innerEndRel = innerStartRel + inner.length;

        /*
            Требуем, чтобы inner целиком помещалась в outer,
            но при этом не совпадала с ней полностью.
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
    private static int calcLevel(int index, List<List<Integer>> children, Integer[] memo) {
        if (memo[index] != null) {
            return memo[index];
        }

        int maxChildLevel = -1;
        for (int child : children.get(index)) {
            maxChildLevel = Math.max(maxChildLevel, calcLevel(child, children, memo));
        }

        memo[index] = maxChildLevel + 1;
        return memo[index];
    }

    /**
     * Проверяет, что две петли образуют переплетение интервалов:
     *
     * A < C < B < D
     * или
     * C < A < D < B
     *
     * Такие петли нельзя упорядочить как дерево вложенности.
     */
    private static boolean isBadPair(LoopInfo first, LoopInfo second) {
        int a = first.firstSegmentIndex;
        int b = first.secondSegmentIndex;
        int c = second.firstSegmentIndex;
        int d = second.secondSegmentIndex;

        return (a < c && c < b && b < d)
            || (c < a && a < d && d < b);
    }

    /**
     * Находит все "плохие" случаи и собирает их в связные группы.
     *
     * Вершины графа - петли.
     * Ребро есть между двумя петлями, если их интервалы переплетаются.
     * Каждая компонента связности выносится в отдельный bad case.
     */
    private static BadCaseBuildResult buildBadCases(List<LoopInfo> loops) {
        int n = loops.size();
        List<List<Integer>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isBadPair(loops.get(i), loops.get(j))) {
                    graph.get(i).add(j);
                    graph.get(j).add(i);
                }
            }
        }

        Set<Integer> badLoopIndices = new HashSet<>();
        List<List<ContourIntersection>> badCases = new ArrayList<>();
        boolean[] visited = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (visited[i] || graph.get(i).isEmpty()) {
                continue;
            }

            List<ContourIntersection> group = new ArrayList<>();
            List<Integer> stack = new ArrayList<>();
            stack.add(i);
            visited[i] = true;

            while (!stack.isEmpty()) {
                int current = stack.remove(stack.size() - 1);
                badLoopIndices.add(current);
                group.add(loops.get(current));

                for (int next : graph.get(current)) {
                    if (!visited[next]) {
                        visited[next] = true;
                        stack.add(next);
                    }
                }
            }

            badCases.add(List.copyOf(group));
        }

        return new BadCaseBuildResult(
            List.copyOf(badCases),
            Set.copyOf(badLoopIndices)
        );
    }

    /**
     * Ищет любой индекс сегмента, не входящий ни в одну петлю.
     */
    private int findFreeSegmentIndex(List<LoopInfo> sortedLoops) {
        for (int index = 0; index < segmentCount; index++) {
            boolean good = true;
            for (LoopInfo info : sortedLoops) {
                if (isSegmentInsideLoop(index, info)) {
                    good = false;
                    break;
                }
            }
            if (good) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Проверяет, входит ли сегмент с данным индексом в дугу петли.
     */
    private boolean isSegmentInsideLoop(int segmentIndex, LoopInfo loop) {
        int rel = cyclicDistance(loop.firstSegmentIndex, segmentIndex, segmentCount);
        return rel >= 0 && rel < loop.length;
    }

    /**
     * Возвращает расстояние по циклу вперед от from до to.
     *
     * Результат всегда лежит в диапазоне [0, segmentCount).
     */
    private static int cyclicDistance(int from, int to, int segmentCount) {
        int distance = to - from;
        if (distance < 0) {
            distance += segmentCount;
        }
        return distance;
    }

    private record BadCaseBuildResult(
        List<List<ContourIntersection>> badCases,
        Set<Integer> badLoopIndices
    ) {
    }

    /**
     * Описание петли, полученной из одного самопересечения.
     *
     * Петля трактуется как проход по контуру
     * от firstSegmentIndex до secondSegmentIndex вперед по обходу.
     */
    public static final class LoopInfo extends ContourIntersection {
        /**
         * Длина дуги в сегментах без учета включенности правой границы.
         *
         * Например:
         * - для 2 -> 5 длина равна 3
         * - для 7 -> 1 на контуре из 10 сегментов длина равна 4
         */
        public final int length;

        /**
         * Уровень вложенности.
         *
         * 0 у самых внутренних петель.
         */
        public final int level;

        private LoopInfo(ContourIntersection base, int segmentCount) {
            super(base.intersection, base.firstSegmentIndex, base.secondSegmentIndex);
            this.length = cyclicDistance(firstSegmentIndex, secondSegmentIndex, segmentCount);
            this.level = -1;
        }

        private LoopInfo(
            SegmentIntersection intersection,
            int firstSegmentIndex,
            int secondSegmentIndex,
            int length,
            int level
        ) {
            super(intersection, firstSegmentIndex, secondSegmentIndex);
            this.length = length;
            this.level = level;
        }

        private LoopInfo withLevel(int newLevel) {
            return new LoopInfo(
                intersection,
                firstSegmentIndex,
                secondSegmentIndex,
                length,
                newLevel
            );
        }
    }
}
