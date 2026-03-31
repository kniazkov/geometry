package com.kniazkov.geometry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Результат алгоритма обработки кольца узлов.
 *
 * Содержит новый узел кольца и соответствие между новыми точками
 * результата и исходными точками, из которых они получились.
 */
public class Node2ProcessingResult {
    public final Node2 node;
    public final Map<Point2, Set<Point2>> pointMapping;


    public Node2ProcessingResult(Node2 node, Map<Point2, Set<Point2>> pointMapping) {
        this.node = node;
        this.pointMapping = copyMapping(pointMapping);
    }

    private static Map<Point2, Set<Point2>> copyMapping(Map<Point2, Set<Point2>> pointMapping) {
        Map<Point2, Set<Point2>> copy = new HashMap<>(pointMapping.size());

        for (Map.Entry<Point2, Set<Point2>> entry : pointMapping.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }

        return Collections.unmodifiableMap(copy);
    }
}
