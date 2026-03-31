package com.kniazkov.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class Node2ChainReducerReduceTest {

    @Test
    void reduce_returnsOriginalRingAndIdentityMapping_whenNoChainsFound() {
        Node2 ring = ring(
            0, 0,
            5, 0,
            5, 5,
            0, 5
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, n -> true);

        Optional<Node2ProcessingResult> result = reducer.reduce(ring);

        assertTrue(result.isPresent());

        Node2ProcessingResult processingResult = result.get();
        assertSame(ring, processingResult.node);

        List<Point2> original = toPoints(ring);
        List<Point2> reduced = toPoints(processingResult.node);

        assertEquals(original.size(), reduced.size());
        for (int i = 0; i < original.size(); i++) {
            assertSame(original.get(i), reduced.get(i));
        }

        assertEquals(original.size(), processingResult.pointMapping.size());
        for (Point2 point : original) {
            assertEquals(Set.of(point), processingResult.pointMapping.get(point));
        }
    }

    @Test
    void reduce_simplifiesDenseChain_andKeepsContourValid() {
        Node2 ring = ring(
            0, 0,
            0.3, 0,
            0.6, 0,
            5, 0,
            5, 5,
            0, 5
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, n -> true);

        Optional<Node2ProcessingResult> result = reducer.reduce(ring);

        assertTrue(result.isPresent());

        Node2ProcessingResult processingResult = result.get();
        List<Point2> reduced = toPoints(processingResult.node);

        assertTrue(reduced.size() >= 3);
        assertTrue(reduced.size() < 6);

        assertContainsApproxPoint(reduced, 0.3, 0.0);
        assertContainsApproxPoint(reduced, 5.0, 0.0);
        assertContainsApproxPoint(reduced, 5.0, 5.0);
        assertContainsApproxPoint(reduced, 0.0, 5.0);
    }

    @Test
    void reduce_returnsEmpty_whenResultWouldContainLessThanThreePoints() {
        Node2 ring = ring(
            0.0, 0.0,
            0.3, 0.0,
            0.6, 0.0,
            0.9, 0.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, n -> true);

        Optional<Node2ProcessingResult> result = reducer.reduce(ring);

        assertTrue(result.isEmpty());
    }

    @Test
    void reduce_handlesChainAcrossRingBoundary() {
        Node2 ring = ring(
            0.0, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.4, 5.0,
            0.2, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, n -> true);

        Optional<Node2ProcessingResult> result = reducer.reduce(ring);

        assertTrue(result.isPresent());

        List<Point2> reduced = toPoints(result.get().node);
        assertTrue(reduced.size() >= 3);

        assertContainsApproxPoint(reduced, 5.0, 0.0);
        assertContainsApproxPoint(reduced, 5.0, 5.0);
    }

    @Test
    void reduce_pointMapping_coversAllOriginalPointsExactlyOnce() {
        Node2 ring = ring(
            0, 0,
            0.3, 0,
            0.6, 0,
            1.0, 0,
            5, 0,
            5, 5,
            0, 5
        );

        Node2ChainReducer reducer = new Node2ChainReducer(1.0, n -> true);

        Optional<Node2ProcessingResult> result = reducer.reduce(ring);

        assertTrue(result.isPresent());

        Map<Point2, Set<Point2>> pointMapping = result.get().pointMapping;

        Set<Point2> collected = new HashSet<>();
        for (Set<Point2> mappedPoints : pointMapping.values()) {
            for (Point2 point : mappedPoints) {
                assertTrue(collected.add(point));
            }
        }

        assertEquals(new HashSet<>(toPoints(ring)), collected);
    }

    @Test
    void reduce_appliesAdditionalSelectionCriteria() {
        Node2 ring = ring(
            0.0, 0.0,
            0.2, 0.0,
            0.6, 0.0,
            5.0, 0.0,
            5.0, 5.0,
            0.0, 5.0
        );

        Node2ChainReducer reducer = new Node2ChainReducer(
            1.0,
            node -> node.point.x >= 0.2
        );

        Optional<Node2ProcessingResult> result = reducer.reduce(ring);

        assertTrue(result.isPresent());

        List<Point2> reduced = toPoints(result.get().node);

        assertEquals(5, reduced.size());
        assertContainsApproxPoint(reduced, 0.0, 0.0);
        assertContainsApproxPoint(reduced, 0.4, 0.0);
        assertContainsApproxPoint(reduced, 5.0, 0.0);
        assertContainsApproxPoint(reduced, 5.0, 5.0);
        assertContainsApproxPoint(reduced, 0.0, 5.0);
    }


    private static Node2 ring(double... coords) {
        List<Point2> points = new ArrayList<>();
        for (int i = 0; i < coords.length; i += 2) {
            points.add(new Point2(coords[i], coords[i + 1]));
        }
        return Node2.fromPoints(points);
    }

    private static List<Point2> toPoints(Node2 start) {
        return Node2.toPoints(start);
    }

    private static void assertContainsApproxPoint(List<Point2> points, double x, double y) {
        Point2 expected = new Point2(x, y);

        for (Point2 point : points) {
            if (point.approximatelyEquals(expected)) {
                return;
            }
        }

        fail("Point not found: (" + x + ", " + y + ")");
    }
}