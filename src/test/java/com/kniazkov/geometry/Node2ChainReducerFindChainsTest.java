package com.kniazkov.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для поиска цепочек узлов в {@link Node2ChainReducer}.
 */
public class Node2ChainReducerFindChainsTest {

    @Test
    void findChains_returnsEmpty_whenNoNodesMatchDistanceCriterion() {
        Node2 ring = ring(
            0, 0,
            5, 0,
            5, 5,
            0, 5
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);

        List<Node2ChainReducer.Chain> chains = reducer.findChains(ring);

        assertTrue(chains.isEmpty());
    }

    @Test
    void findChains_returnsSingleChain_whenOneDenseGroupIsInsideContour() {
        Node2 ring = ring(
            0.0, 0.0,
            0.3, 0.0,
            0.6, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);

        List<Node2ChainReducer.Chain> chains = reducer.findChains(ring);

        assertEquals(1, chains.size());
        assertChain(
            chains.get(0),
            0.0, 0.0,
            0.3, 0.0,
            0.6, 0.0
        );
    }

    @Test
    void findChains_returnsSeveralChains_whenDenseGroupsAreSeparated() {
        Node2 ring = ring(
            0.0, 0.0,
            0.3, 0.0,
            0.6, 0.0,
            5.0, 0.0,
            5.3, 0.0,
            10.0, 0.0,
            10.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);

        List<Node2ChainReducer.Chain> chains = reducer.findChains(ring);

        assertEquals(2, chains.size());
        assertChain(
            chains.get(0),
            5.0, 0.0,
            5.3, 0.0
        );
        assertChain(
            chains.get(1),
            0.0, 0.0,
            0.3, 0.0,
            0.6, 0.0
        );
    }

    @Test
    void findChains_mergesFirstAndLastGroups_whenChainCrossesRingBoundary() {
        Node2 ring = ring(
            0.0, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.0, 5.0,
            0.0, 0.6
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);

        List<Node2ChainReducer.Chain> chains = reducer.findChains(ring);

        assertEquals(1, chains.size());
        assertChain(
            chains.get(0),
            0.0, 0.6,
            0.0, 0.0
        );
    }

    @Test
    void findChains_returnsWholeRingAsSingleChain_whenAllNodesAreSelected() {
        Node2 ring = ring(
            0.0, 0.0,
            0.5, 0.0,
            0.25, 0.4
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> true);

        List<Node2ChainReducer.Chain> chains = reducer.findChains(ring);

        assertEquals(1, chains.size());
        assertChain(
            chains.get(0),
            0.0, 0.0,
            0.5, 0.0,
            0.25, 0.4
        );
    }

    @Test
    void findChains_appliesAdditionalNodeCriterion() {
        Node2 ring = ring(
            0.0, 0.0,
            0.3, 0.0,
            0.6, 0.0,
            0.9, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, node -> node.point.x >= 0.5);

        List<Node2ChainReducer.Chain> chains = reducer.findChains(ring);

        assertEquals(1, chains.size());
        assertChain(
            chains.get(0),
            0.6, 0.0,
            0.9, 0.0
        );
    }

    private static Node2 ring(double... coords) {
        if (coords.length < 6 || coords.length % 2 != 0) {
            throw new IllegalArgumentException("Expected even number of coordinates for at least 3 points");
        }

        List<Point2> points = new ArrayList<>(coords.length / 2);
        for (int i = 0; i < coords.length; i += 2) {
            points.add(new Point2(coords[i], coords[i + 1]));
        }

        return Node2.fromPoints(points);
    }

    private static void assertChain(Node2ChainReducer.Chain chain, double... coords) {
        assertEquals(coords.length / 2, chain.size(), "Unexpected chain size");

        List<Node2> nodes = chain.nodes;
        for (int i = 0; i < nodes.size(); i++) {
            assertPoint(nodes.get(i).point, coords[i * 2], coords[i * 2 + 1]);
        }
    }

    private static void assertPoint(Point2 point, double x, double y) {
        assertTrue(
            point.approximatelyEquals(new Point2(x, y)),
            () -> "Expected point (" + x + ", " + y + "), but was (" + point.x + ", " + point.y + ")"
        );
    }
}
