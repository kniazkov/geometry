package com.kniazkov.geometry;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class Test {

    public static void main(String[] args) throws IOException {
        ModelIO loader = ModelIOFactory.forFormat("stl");
        SvgBuilder svg = new SvgBuilder();

        Model model = measure(
            "Load STL",
            () -> loader.load(Paths.get("D:\\Models\\ss.stl"))
        );

        System.out.println("Loaded " + model.triangles.size() + " triangles");

        /*
        measure("Save STL", () -> {
            io.save(Paths.get("D:\\ss2.stl"), model);
            return null;
        });
         */

        List<Segment2> segments = measure(
            "Slicing",
            () -> model.sliceAt(1500)
        );

        System.out.println("Obtain " + segments.size() + " segments after slicing");
        //svg.addSegments(segments, 1, "red", SvgStrokeStyle.SOLID);

        List<Contour> contours = measure(
                "Assembling",
                () -> {
                    ContourAssembler assembler = new ContourAssembler(segments, 0.001);
                    return assembler.assemble();
                }
        );

        System.out.println("Obtain " + contours.size() + " contour" + (contours.size() > 1 ? "s" : "")  + " after assembling");

        List<Contour> normalized = measure(
                "Normalizing",
                () -> Contour.normalizeAll(contours)
        );

        for (Contour contour : normalized) {
            //svg.addSegments(contour.toSegments(), 1, "blue", SvgStrokeStyle.SOLID);
            System.out.println("Contour has " + contour.points.size() + " points before simplifying");
            Node2 begin = contour.toLinkedList();
            begin = Node2.removeStraight(begin);
            begin = Node2.removeNodesByCriteria(
                    begin,
                    node -> node.distanceToPrev + node.distanceToNext < 50 && node.angle > Math.PI / 2 + Math.PI / 4 && !node.outer
            );
            begin = Node2.removeStraight(begin);
            System.out.println("Contour has " + Node2.toPoints(begin).size() + " points after simplifying");
            Node2 node = begin;
            do {
                svg.addSegments(
                        Collections.singletonList(new Segment2(node.point, node.prev.point)),
                        1,
                        (node.straight ? "green" : (node.outer ? "blue" : "red")),
                        SvgStrokeStyle.SOLID
                );
                node = node.next;
            } while (node != begin);
        }

        svg.save(Paths.get("result.svg"), 0.2);
    }

    /**
     * Выполняет код, печатает затраченное время в миллисекундах
     * и возвращает результат.
     */
    private static <T> T measure(String label, ThrowingSupplier<T> action) throws IOException {
        long start = System.nanoTime();
        try {
            return action.get();
        } finally {
            long elapsedNanos = System.nanoTime() - start;
            double elapsedMs = elapsedNanos / 1_000_000.0;
            System.out.printf("%s took %.3f ms%n", label, elapsedMs);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws IOException;
    }
}
