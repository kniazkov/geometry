package com.kniazkov.geometry;

/**
 * Критерий выбора узла для различных алгоритмов
 */
@FunctionalInterface
public interface Node2SelectionCriteria {
    boolean test(Node2 node);
}
