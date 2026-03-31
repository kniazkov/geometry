package com.kniazkov.geometry;

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
}
