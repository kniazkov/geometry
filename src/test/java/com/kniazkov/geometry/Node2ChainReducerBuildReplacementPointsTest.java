package com.kniazkov.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для построения новых точек одной плотной цепочки.
 */
public class Node2ChainReducerBuildReplacementPointsTest {

    @Test
    void buildReplacementPoints_returnsSingleMidpoint_whenChainLengthIsLessThanDistance() {
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

        List<Point2> result = reducer.buildReplacementPoints(chain);

        assertEquals(1, result.size());
        assertTrue(result.get(0).approximatelyEquals(new Point2(0.4, 0.0)));
    }

    @Test
    void buildReplacementPoints_keepsOnlyEndpoints_whenLengthEqualsDistance() {
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

        List<Point2> result = reducer.buildReplacementPoints(chain);

        assertEquals(2, result.size());
        assertSame(chain.points.get(0), result.get(0));
        assertSame(chain.points.get(2), result.get(1));
    }

    @Test
    void buildReplacementPoints_addsInnerPoints_whenChainIsLongEnough() {
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

        List<Point2> result = reducer.buildReplacementPoints(chain);

        assertEquals(3, result.size());
        assertSame(chain.points.get(0), result.get(0));
        assertTrue(result.get(1).approximatelyEquals(new Point2(1.0, 0.0)));
        assertSame(chain.points.get(4), result.get(2));
    }

    @Test
    void buildReplacementPoints_reducesIntervalCount_whenEuclideanDistanceIsTooSmall() {
        Node2 ring = ring(
            0.0, 0.0,
            0.0, 1.0,
            1.0, 1.0,
            1.0, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.1, node -> true);
        Node2ChainReducer.Chain chain = chain(ring, 4);

        List<Point2> result = reducer.buildReplacementPoints(chain);

        assertEquals(3, result.size());
        assertSame(chain.points.get(0), result.get(0));
        assertSame(chain.points.get(3), result.get(2));
        assertTrue(result.get(0).distanceTo(result.get(1)) >= 1.1);
    }

    @Test
    void buildReplacementPoints_preservesMinDistanceBetweenNeighboringNewPoints() {
        Node2 ring = ring(
            0.0, 0.0,
            0.7, 0.0,
            1.2, 0.0,
            2.1, 0.0,
            2.8, 0.0,
            6.0, 0.0,
            6.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);
        Node2ChainReducer.Chain chain = chain(ring, 5);

        List<Point2> result = reducer.buildReplacementPoints(chain);

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).distanceTo(result.get(i)) >= 1.0);
        }
    }

    private static Node2 ring(double... coords) {
        if (coords.length < 6 || coords.length % 2 != 0) {
            throw new IllegalArgumentException("Coordinates must contain at least 3 points");
        }

        List<Point2> points = new java.util.ArrayList<>(coords.length / 2);
        for (int i = 0; i < coords.length; i += 2) {
            points.add(new Point2(coords[i], coords[i + 1]));
        }

        return Node2.fromPoints(points);
    }

    private static Node2ChainReducer.Chain chain(Node2 start, int size) {
        List<Node2> nodes = new java.util.ArrayList<>(size);
        Node2 node = start;

        for (int i = 0; i < size; i++) {
            nodes.add(node);
            node = node.getNext();
        }

        return new Node2ChainReducer.Chain(nodes);
    }
}