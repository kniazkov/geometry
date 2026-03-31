package com.kniazkov.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для построения соответствия между новыми и старыми точками одной цепочки.
 */
public class Node2ChainReducerBuildPointMappingTest {

    @Test
    void buildPointMapping_mapsAllOldPointsToSingleNewPoint_whenChainCollapsed() {
        Node2 ring = ring(
            0.0, 0.0,
            0.4, 0.0,
            0.8, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);
        Node2ChainReducer.Chain chain = chain(ring, 3);

        Point2 newPoint = chain.pointAtLength(chain.totalLength / 2.0);
        Map<Point2, Set<Point2>> mapping = reducer.buildPointMapping(chain, List.of(newPoint));

        assertEquals(1, mapping.size());
        assertTrue(mapping.containsKey(newPoint));
        assertEquals(Set.copyOf(chain.points), mapping.get(newPoint));
    }

    @Test
    void buildPointMapping_mapsEndpointsToThemselves_whenOnlyEndpointsRemain() {
        Node2 ring = ring(
            0.0, 0.0,
            0.5, 0.0,
            1.0, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);
        Node2ChainReducer.Chain chain = chain(ring, 3);

        List<Point2> newPoints = List.of(
            chain.points.get(0),
            chain.points.get(2)
        );

        Map<Point2, Set<Point2>> mapping = reducer.buildPointMapping(chain, newPoints);

        assertEquals(2, mapping.size());
        assertEquals(Set.of(chain.points.get(0), chain.points.get(1)), mapping.get(chain.points.get(0)));
        assertEquals(Set.of(chain.points.get(2)), mapping.get(chain.points.get(2)));
    }

    @Test
    void buildPointMapping_distributesOldPointsBetweenSeveralNewPoints_byNearestPosition() {
        Node2 ring = ring(
            0.0, 0.0,
            0.5, 0.0,
            1.0, 0.0,
            1.5, 0.0,
            2.0, 0.0,
            6.0, 0.0,
            6.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);
        Node2ChainReducer.Chain chain = chain(ring, 5);

        List<Point2> newPoints = reducer.buildReplacementPoints(chain);
        Map<Point2, Set<Point2>> mapping = reducer.buildPointMapping(chain, newPoints);

        assertEquals(3, mapping.size());

        Point2 first = newPoints.get(0);
        Point2 middle = newPoints.get(1);
        Point2 last = newPoints.get(2);

        assertEquals(Set.of(chain.points.get(0), chain.points.get(1)), mapping.get(first));
        assertEquals(Set.of(chain.points.get(2), chain.points.get(3)), mapping.get(middle));
        assertEquals(Set.of(chain.points.get(4)), mapping.get(last));
    }

    @Test
    void buildPointMapping_assignsEachOldPointExactlyOnce() {
        Node2 ring = ring(
            0.0, 0.0,
            0.7, 0.0,
            1.4, 0.0,
            2.1, 0.0,
            2.8, 0.0,
            6.0, 0.0,
            6.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);
        Node2ChainReducer.Chain chain = chain(ring, 5);

        List<Point2> newPoints = reducer.buildReplacementPoints(chain);
        Map<Point2, Set<Point2>> mapping = reducer.buildPointMapping(chain, newPoints);

        java.util.Set<Point2> assigned = new java.util.HashSet<>();
        for (Set<Point2> set : mapping.values()) {
            for (Point2 point : set) {
                assertTrue(assigned.add(point));
            }
        }

        assertEquals(Set.copyOf(chain.points), assigned);
    }


    private static Node2 ring(double... coords) {
        if (coords.length < 6 || coords.length % 2 != 0) {
            throw new IllegalArgumentException("Coordinates must contain at least 3 points");
        }

        List<Point2> points = new ArrayList<>(coords.length / 2);
        for (int i = 0; i < coords.length; i += 2) {
            points.add(new Point2(coords[i], coords[i + 1]));
        }

        return Node2.fromPoints(points);
    }

    private static Node2ChainReducer.Chain chain(Node2 start, int size) {
        List<Node2> nodes = new ArrayList<>(size);
        Node2 node = start;

        for (int i = 0; i < size; i++) {
            nodes.add(node);
            node = node.getNext();
        }

        return new Node2ChainReducer.Chain(nodes);
    }
}
