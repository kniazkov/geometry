package com.kniazkov.geometry;

/**
 * Критерий выбора узла для различных алгоритмов
 */
@FunctionalInterface
public interface NodeSelectionCriteria {
    boolean test(Node2 node);
}
