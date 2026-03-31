package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private final NodeSelectionCriteria criteria;


    public Node2ChainReducer(double distance, NodeSelectionCriteria criteria) {
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
        return Optional.empty();
    }

    public double getDistance() {
        return distance;
    }

    public NodeSelectionCriteria getCriteria() {
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
    public List<Chain> findChains(Node2 start) {
        List<Node2> nodes = Node2.toNodes(start);
        int size = nodes.size();

        if (size == 0) {
            return Collections.emptyList();
        }

        List<Boolean> selected = new ArrayList<>(size);
        boolean hasSelected = false;
        boolean hasUnselected = false;

        for (Node2 node : nodes) {
            boolean isSelected = isChainNode(node);
            selected.add(isSelected);
            hasSelected |= isSelected;
            hasUnselected |= !isSelected;
        }

        if (!hasSelected) {
            return Collections.emptyList();
        }

        /*
            Если выбран весь контур, это одна большая циклическая цепочка.
         */
        if (!hasUnselected) {
            return Collections.singletonList(new Chain(nodes));
        }

        /*
            Чтобы не заниматься последующим склеиванием первой и последней цепочки,
            начинаем обход сразу после любого узла, который не входит в цепочку.
            Тогда каждая найденная группа выбранных узлов автоматически будет максимальной.
         */
        int anchorIndex = -1;
        for (int i = 0; i < size; i++) {
            if (!selected.get(i)) {
                anchorIndex = i;
                break;
            }
        }

        List<Chain> chains = new ArrayList<>();
        List<Node2> currentChain = null;

        for (int step = 1; step <= size; step++) {
            int index = (anchorIndex + step) % size;
            Node2 node = nodes.get(index);

            if (selected.get(index)) {
                if (currentChain == null) {
                    currentChain = new ArrayList<>();
                }
                currentChain.add(node);
            } else if (currentChain != null) {
                chains.add(new Chain(currentChain));
                currentChain = null;
            }
        }

        return Collections.unmodifiableList(chains);
    }

    /**
     * Возвращает true, если узел должен войти в плотную цепочку.
     */
    private boolean isChainNode(Node2 node) {
        boolean shortNeighborSegment = node.getDistanceToPrevious() < distance || node.getDistanceToNext() < distance;
        return shortNeighborSegment && criteria.test(node);
    }

    /**
     * Непрерывная цепочка подряд идущих узлов кольца,
     * отобранных для дальнейшей обработки.
     */
    public static class Chain {
        public final List<Node2> nodes;


        public Chain(List<Node2> nodes) {
            if (nodes.isEmpty()) {
                throw new IllegalArgumentException("Chain must contain at least one node");
            }

            this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        }

        public Node2 getFirst() {
            return nodes.get(0);
        }

        public Node2 getLast() {
            return nodes.get(nodes.size() - 1);
        }

        public int size() {
            return nodes.size();
        }
    }
}
