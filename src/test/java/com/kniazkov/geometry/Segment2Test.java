package com.kniazkov.geometry;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для пересечения двумерных отрезков.
 */
public class Segment2Test {

    @Test
    void intersect_returnsEmpty_whenSegmentsDoNotIntersect() {
        Segment2 s1 = new Segment2(
                new Point2(0, 0),
                new Point2(1, 0)
        );
        Segment2 s2 = new Segment2(
                new Point2(2, 0),
                new Point2(3, 0)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isEmpty());
    }

    @Test
    void intersect_returnsPoint_whenSegmentsCross() {
        Segment2 s1 = new Segment2(
                new Point2(0, 0),
                new Point2(2, 2)
        );
        Segment2 s2 = new Segment2(
                new Point2(0, 2),
                new Point2(2, 0)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isPresent());
        assertInstanceOf(Point2.class, result.get());

        Point2 p = (Point2) result.get();
        assertTrue(p.approximatelyEquals(new Point2(1, 1)));
    }

    @Test
    void intersect_returnsPoint_whenSegmentsTouchAtEndpoint() {
        Segment2 s1 = new Segment2(
                new Point2(0, 0),
                new Point2(1, 0)
        );
        Segment2 s2 = new Segment2(
                new Point2(1, 0),
                new Point2(2, 0)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isPresent());
        assertInstanceOf(Point2.class, result.get());

        Point2 p = (Point2) result.get();
        assertTrue(p.approximatelyEquals(new Point2(1, 0)));
    }

    @Test
    void intersect_returnsOverlapSegment_whenCollinearSegmentsPartiallyOverlap() {
        Segment2 s1 = new Segment2(
                new Point2(0, 0),
                new Point2(10, 0)
        );
        Segment2 s2 = new Segment2(
                new Point2(3, 0),
                new Point2(7, 0)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isPresent());
        assertInstanceOf(Segment2.class, result.get());

        Segment2 overlap = (Segment2) result.get();
        assertTrue(overlap.a.approximatelyEquals(new Point2(3, 0)));
        assertTrue(overlap.b.approximatelyEquals(new Point2(7, 0)));
    }

    @Test
    void intersect_returnsOverlapSegment_withOrientationOfCurrentSegment() {
        Segment2 s1 = new Segment2(
                new Point2(10, 0),
                new Point2(0, 0)
        );
        Segment2 s2 = new Segment2(
                new Point2(3, 0),
                new Point2(7, 0)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isPresent());
        assertInstanceOf(Segment2.class, result.get());

        Segment2 overlap = (Segment2) result.get();

        // Результирующий сегмент должен быть ориентирован как текущий s1:
        // начало ближе к s1.a = (10, 0), конец ближе к s1.b = (0, 0)
        assertTrue(overlap.a.approximatelyEquals(new Point2(7, 0)));
        assertTrue(overlap.b.approximatelyEquals(new Point2(3, 0)));
    }

    @Test
    void intersect_returnsPoint_whenCollinearSegmentsTouchAtOnePoint() {
        Segment2 s1 = new Segment2(
                new Point2(0, 0),
                new Point2(5, 0)
        );
        Segment2 s2 = new Segment2(
                new Point2(5, 0),
                new Point2(8, 0)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isPresent());
        assertInstanceOf(Point2.class, result.get());

        Point2 p = (Point2) result.get();
        assertTrue(p.approximatelyEquals(new Point2(5, 0)));
    }

    @Test
    void intersect_returnsEmpty_whenParallelButNotCollinear() {
        Segment2 s1 = new Segment2(
                new Point2(0, 0),
                new Point2(5, 0)
        );
        Segment2 s2 = new Segment2(
                new Point2(0, 1),
                new Point2(5, 1)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isEmpty());
    }

    @Test
    void intersect_returnsPoint_whenFirstSegmentIsDegenerateAndLiesOnSecond() {
        Segment2 s1 = new Segment2(
                new Point2(2, 0),
                new Point2(2, 0)
        );
        Segment2 s2 = new Segment2(
                new Point2(0, 0),
                new Point2(5, 0)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isPresent());
        assertInstanceOf(Point2.class, result.get());

        Point2 p = (Point2) result.get();
        assertTrue(p.approximatelyEquals(new Point2(2, 0)));
    }

    @Test
    void intersect_returnsEmpty_whenDegenerateSegmentsAreDifferentPoints() {
        Segment2 s1 = new Segment2(
                new Point2(1, 1),
                new Point2(1, 1)
        );
        Segment2 s2 = new Segment2(
                new Point2(2, 2),
                new Point2(2, 2)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isEmpty());
    }

    @Test
    void intersect_returnsPoint_whenBothDegenerateSegmentsAreSamePoint() {
        Segment2 s1 = new Segment2(
                new Point2(1, 1),
                new Point2(1, 1)
        );
        Segment2 s2 = new Segment2(
                new Point2(1, 1),
                new Point2(1, 1)
        );

        Optional<SegmentIntersection> result = s1.intersect(s2);

        assertTrue(result.isPresent());
        assertInstanceOf(Point2.class, result.get());

        Point2 p = (Point2) result.get();
        assertTrue(p.approximatelyEquals(new Point2(1, 1)));
    }
}
