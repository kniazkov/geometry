package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Выполняет сложное упрощение кольца узлов за несколько проходов.
 *
 * Алгоритм работает с цепочками узлов, выбранными по обязательному
 * критерию расстояния и дополнительному пользовательскому критерию.
 *
 * Пока это только заготовка класса: публичный API уже есть,
 * а сама реализация будет добавляться по частям.
 */
public class Node2ChainReducer {
    private final double distance;
    private final Node2SelectionCriteria criteria;


    public Node2ChainReducer(double distance, Node2SelectionCriteria criteria) {
        this.distance = distance;
        this.criteria = criteria;
    }

    /**
     * Выполняет упрощение кольца узлов.
     *
     * Возвращает пустой результат, если после обработки нельзя получить
     * корректный контур как минимум из трех точек.
     */
    public Optional<Node2ProcessingResult> reduce(Node2 start) {
        List<Chain> chains = findChains(start);

        /*
            Нет цепочек — возвращаем исходный результат как есть.
         */
        if (chains.isEmpty()) {
            Map<Point2, Set<Point2>> identityMapping = new HashMap<>();

            Node2 node = start;
            do {
                identityMapping.put(node.point, Set.of(node.point));
                node = node.getNext();
            } while (node != start);

            return Optional.of(new Node2ProcessingResult(start, identityMapping));
        }

        /*
            Строим замены.
         */
        Map<Node2, ChainReplacement> replacements = buildReplacements(chains);

        /*
            Строим новые точки.
         */
        List<Point2> newPoints = buildResultPoints(start, replacements);

        /*
            Проверка минимального количества точек.
         */
        if (newPoints.size() < 3) {
            return Optional.empty();
        }

        /*
            Создаем новое кольцо.
         */
        Node2 newStart = Node2.fromPoints(newPoints);

        /*
            Строим сопоставление старых точек и новых.
         */
        Map<Point2, Set<Point2>> mapping = buildResultMapping(start, replacements);

        return Optional.of(new Node2ProcessingResult(newStart, mapping));
    }

    public double getDistance() {
        return distance;
    }

    public Node2SelectionCriteria getCriteria() {
        return criteria;
    }

    /**
     * Находит в кольце максимальные цепочки подряд идущих узлов,
     * которые подходят под критерий специальной обработки.
     *
     * Узел включается в цепочку, если:
     * - выполняется дополнительный пользовательский критерий;
     * - хотя бы один из двух соседних сегментов короче заданного порога.
     *
     * Так как кольцо циклическое, первая и последняя цепочки при обходе
     * не считаются разными: если подходящие узлы идут через границу обхода,
     * возвращается одна общая цепочка.
     */
    List<Chain> findChains(Node2 start) {
        /*
            Вырожденный случай: кольцо из одного узла
         */
        if (start.getNext() == start) {
            return List.of();
        }

        /*
            Ищем узел-разделитель, после которого безопасно начинать обход.
            Разделитель - это узел, который сам не подходит под дополнительный критерий
            или не соединен со следующим узлом коротким сегментом.

            Если такого узла нет, значит:
            - все узлы проходят дополнительный критерий;
            - каждый переход к следующему узлу короче distance.
            То есть, все кольцо является одной плотной цепочкой.
         */
        Node2 node = start;
        Node2 anchor = null;
        do {
            if (!criteria.test(node) || node.getDistanceToNext() >= distance) {
                anchor = node;
                break;
            }

            node = node.getNext();
        } while (node != start);

        if (anchor == null) {
            return List.of(new Chain(Node2.toNodes(start)));
        }


        /*
            Ищем стартовый узел, такой, с которого можно начать цепочку.
            Стартовый узел:
            - проходит дополнительный критерий;
            - переход до следующего узла < distance;
            - переход до предыдущего узла >= distance или предыдущий узел не проходит дополнительный критерий.
            Если такого узла нет, то значит, ни один узел не подходит и нельзя собрать
            ни одной цепочки.
         */
        Node2 first = anchor.getNext();
        node = first;
        while(true) {
            if (criteria.test(node) &&
                (node.getDistanceToPrevious() >= distance || !criteria.test(node.getPrevious())) &&
                node.getDistanceToNext() < distance)
            {
                first = node;
                break;
            }
            node = node.getNext();
            if (node == first) {
                return List.of();
            }
        }

        List<Chain> chains = new ArrayList<>();

        while(true) {
            List<Node2> nodes = new ArrayList<>();

            /*
                Здесь node указывает на начало цепочки. Кладем этот узел в цепочку.
             */
            nodes.add(node);
            node = node.getNext();

            /*
                Пока у текущего узла расстояние до предыдущего узла < distance и узел
                проходит дополнительный критерий, этот узел добавляется в цепочку.
             */
            while(criteria.test(node) && node.getDistanceToPrevious() < distance) {
                nodes.add(node);
                node = node.getNext();
            }

            /*
                Цепочка собрана. Если в ней больше 1 узла, добавляем ее в список.
             */
            if (nodes.size() > 1) {
                chains.add(new Chain(nodes));
            }

            /*
                Сейчас node указывает на неподходящий узел.
                Находим следующий стартовый узел. Если такой не нашли (вернулись в first),
                конец алгоритма.
             */
            while(true) {
                if (node == first) {
                    return Collections.unmodifiableList(chains);
                }
                if (criteria.test(node) &&
                    (node.getDistanceToPrevious() >= distance || !criteria.test(node.getPrevious())) &&
                    node.getDistanceToNext() < distance)
                {
                    break;
                }
                node = node.getNext();
            }
        }
   }

    /**
     * Строит новый список точек для одной плотной цепочки.
     *
     * Правила:
     * - если длина всей цепочки меньше distance, цепочка схлопывается в одну точку,
     *   лежащую посередине полилинии по накопленной длине;
     * - иначе первая и последняя точки сохраняются,
     *   а внутренние точки расставляются по полилинии максимально плотно,
     *   но так, чтобы евклидово расстояние между соседними новыми точками
     *   было не меньше distance.
     */
    List<Point2> buildReplacementPoints(Chain chain) {
        /*
            Если вся цепочка меньше distance, оставляем одну точку.
         */
        if (chain.totalLength < distance) {
            return List.of(
                chain.pointAtLength(chain.totalLength / 2.0)
            );
        }

        /*
            Считаем приблизительное количество отрезков, размер которых не меньше distance.
         */
        int segmentCount = (int) Math.floor(chain.totalLength / distance);
        segmentCount = Math.max(1, segmentCount);

        while (true) {
            /*
                Строим новые точки по количеству отрезков, эти точки будут равномерно
                распределены по старой траектории.
             */
            List<Point2> points = buildPointsForSegmentCount(chain, segmentCount);

            /*
                Проверяем, действительно ли полученное расстояние между новыми точками не
                превышает distance.
             */
            if (hasMinEuclideanSpacing(points)) {
                return points;
            }

            /*
                Если превышает - уменьшаем количество точек и пересчитываем.
             */
            segmentCount--;
            if (segmentCount <= 1) {
                /*
                    Если уменьшать некуда - оставляем две точки в начале и в конце.
                 */
                return List.of(
                    chain.points.get(0),
                    chain.points.get(chain.points.size() - 1)
                );
            }
        }
    }

    /**
     * Строит соответствия между новыми и старыми точками одной цепочки.
     * Простейшее сопоставление:
     * - если одна новая точка — к ней идут все старые
     * - иначе:
     *   - первая новая -> первая старая
     *   - последняя новая -> последняя старая
     *   - остальные старые -> ближайшая новая (по квадрату расстояния)
     */
    Map<Point2,Set<Point2>> buildPointMapping(Chain chain, List<Point2> newPoints) {
        Map<Point2, Set<Point2>> mapping = new HashMap<>();

        /*
             Одна точка — все сопоставляется с ней.
         */
        if (newPoints.size() == 1) {
            mapping.put(newPoints.get(0), new HashSet<>(chain.points));
            return mapping;
        }

        /*
            Иначе, первая и последняя точки сопоставляется однозначно.
         */
        for (Point2 p : newPoints) {
            mapping.put(p, new HashSet<>());
        }

        Point2 firstNew = newPoints.get(0);
        Point2 lastNew = newPoints.get(newPoints.size() - 1);

        mapping.get(firstNew).add(chain.points.get(0));
        mapping.get(lastNew).add(chain.points.get(chain.points.size() - 1));

        /*
            Все остальные — к ближайшей новой.
         */
        for (int i = 1; i < chain.points.size() - 1; i++) {
            Point2 oldPoint = chain.points.get(i);

            int bestIndex = 0;
            double bestDistance = oldPoint.distanceSquaredTo(newPoints.get(0));

            for (int j = 1; j < newPoints.size(); j++) {
                double distance = oldPoint.distanceSquaredTo(newPoints.get(j));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = j;
                }
            }

            mapping.get(newPoints.get(bestIndex)).add(oldPoint);
        }

        return mapping;
    }
    
    /**
     * Строит точки цепочки для заданного числа интервалов.
     *
     * Если intervalCount == 1, остаются только первая и последняя точки.
     * Иначе внутренние точки ставятся равномерно по длине полилинии.
     */
    private static List<Point2> buildPointsForSegmentCount(Chain chain, int intervalCount) {
        if (intervalCount <= 1) {
            return List.of(
                chain.points.get(0),
                chain.points.get(chain.points.size() - 1)
            );
        }

        List<Point2> result = new ArrayList<>(intervalCount + 1);
        double step = chain.totalLength / intervalCount;

        result.add(chain.points.get(0));
        for (int i = 1; i < intervalCount; i++) {
            result.add(chain.pointAtLength(step * i));
        }
        result.add(chain.points.get(chain.points.size() - 1));

        return result;
    }

    /**
     * Проверяет, что евклидово расстояние между соседними точками
     * не меньше distance.
     */
    private boolean hasMinEuclideanSpacing(List<Point2> points) {
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i - 1).distanceTo(points.get(i)) < distance) {
                return false;
            }
        }

        return true;
    }

    /**
     * Строит замены для всех найденных цепочек.
     *
     * Для каждой цепочки:
     * - вычисляются новые точки;
     * - строится сопоставление новых точек со старыми;
     * - результат индексируется по первому узлу цепочки.
     *
     * Используется далее для сборки итогового кольца и общей карты соответствий.
     */
    private Map<Node2, ChainReplacement> buildReplacements(List<Chain> chains) {
        Map<Node2, ChainReplacement> result = new HashMap<>();

        for (Chain chain : chains) {
            List<Point2> newPoints = buildReplacementPoints(chain);
            Map<Point2, Set<Point2>> mapping = buildPointMapping(chain, newPoints);

            result.put(chain.getFirst(), new ChainReplacement(chain, newPoints, mapping));
        }

        return result;
    }

    /**
     * Формирует новый список точек всего кольца с учетом замен цепочек.
     *
     * Логика:
     * - обход выполняется по исходному кольцу;
     * - если текущий узел является началом цепочки, добавляются все новые точки цепочки,
     *   а исходные узлы цепочки пропускаются;
     * - если узел не входит в цепочку, его точка переносится без изменений.
     *
     * Порядок обхода сохраняется.
     */
    private List<Point2> buildResultPoints(Node2 start, Map<Node2, ChainReplacement> replacements) {
        List<Point2> result = new ArrayList<>();

        Node2 node = start;
        do {
            ChainReplacement replacement = replacements.get(node);

            if (replacement != null) {
                result.addAll(replacement.newPoints);

                // перепрыгиваем всю цепочку
                node = replacement.chain.getLast().getNext();
            } else {
                result.add(node.point);
                node = node.getNext();
            }

        } while (node != start);

        return result;
    }

    /**
     * Строит итоговое соответствие между новыми и старыми точками всего кольца.
     *
     * Логика:
     * - для цепочек используется заранее построенное сопоставление;
     * - для узлов вне цепочек каждая точка сопоставляется сама с собой.
     *
     * Гарантируется, что каждая исходная точка входит ровно в одно множество результата.
     */
    private Map<Point2, Set<Point2>> buildResultMapping(
        Node2 start,
        Map<Node2, ChainReplacement> replacements
    ) {
        Map<Point2, Set<Point2>> result = new HashMap<>();

        Node2 node = start;
        do {
            ChainReplacement replacement = replacements.get(node);

            if (replacement != null) {
                result.putAll(replacement.mapping);
                node = replacement.chain.getLast().getNext();
            } else {
                result.put(node.point, Set.of(node.point));
                node = node.getNext();
            }
        } while (node != start);

        return result;
    }

    /**
     * Непрерывная цепочка подряд идущих узлов кольца,
     * отобранных для дальнейшей обработки.
     *
     * Цепочка всегда содержит как минимум два узла,
     * потому что одиночная точка не образует участок ломаной.
     */
    static class Chain {
        final List<Node2> nodes;
        final List<Point2> points;
        final List<Double> cumulativeLengths;
        final double totalLength;

        Chain(List<Node2> nodes) {
            if (nodes.size() < 2) {
                throw new IllegalArgumentException("Chain must contain at least 2 nodes");
            }

            this.nodes = List.copyOf(nodes);

            List<Point2> points = new ArrayList<>(nodes.size());
            List<Double> cumulativeLengths = new ArrayList<>(nodes.size());

            double length = 0.0;
            cumulativeLengths.add(length);

            for (int i = 0; i < nodes.size(); i++) {
                Node2 node = nodes.get(i);
                points.add(node.point);

                if (i > 0) {
                    /*
                        Узлы в цепочке идут подряд по кольцу,
                        поэтому расстояние от текущего узла до предыдущего
                        уже заранее посчитано в самом Node2.
                     */
                    length += node.getDistanceToPrevious();
                    cumulativeLengths.add(length);
                }
            }

            this.points = Collections.unmodifiableList(points);
            this.cumulativeLengths = Collections.unmodifiableList(cumulativeLengths);
            this.totalLength = length;
        }

        Node2 getFirst() {
            return nodes.get(0);
        }

        Node2 getLast() {
            return nodes.get(nodes.size() - 1);
        }

        int size() {
            return nodes.size();
        }

        /**
         * Возвращает точку на полилинии цепочки на заданном расстоянии от ее начала.
         */
        Point2 pointAtLength(double distanceFromStart) {
            if (distanceFromStart <= 0.0) {
                return points.get(0);
            }

            if (distanceFromStart >= totalLength) {
                return points.get(points.size() - 1);
            }

            for (int i = 1; i < points.size(); i++) {
                double segmentStart = cumulativeLengths.get(i - 1);
                double segmentEnd = cumulativeLengths.get(i);

                if (distanceFromStart <= segmentEnd) {
                    double segmentLength = segmentEnd - segmentStart;

                    if (segmentLength <= Point2.EPSILON) {
                        return points.get(i);
                    }

                    double t = (distanceFromStart - segmentStart) / segmentLength;
                    Point2 a = points.get(i - 1);
                    Point2 b = points.get(i);

                    return new Point2(
                        a.x + (b.x - a.x) * t,
                        a.y + (b.y - a.y) * t
                    );
                }
            }

            return points.get(points.size() - 1);
        }
    }

    /**
     * Результат обработки одной цепочки:
     * - исходная цепочка узлов;
     * - новые точки, которыми она заменяется;
     * - соответствие новых точек старым.
     */
    private static class ChainReplacement {
        final Chain chain;
        final List<Point2> newPoints;
        final Map<Point2, Set<Point2>> mapping;

        ChainReplacement(Chain chain, List<Point2> newPoints, Map<Point2, Set<Point2>> mapping) {
            this.chain = chain;
            this.newPoints = newPoints;
            this.mapping = mapping;
        }
    }
}
