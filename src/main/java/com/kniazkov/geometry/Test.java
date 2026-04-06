package com.kniazkov.geometry;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] args) throws IOException {
        ModelIO loader = ModelIOFactory.forFormat("stl");
        SvgBuilder svg = new SvgBuilder();

        Model model = measure(
            "Load STL",
            () -> loader.load(Paths.get("D:\\ss.stl"))
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

        List<Contour> simplified = measure(
                "Simplifying",
                () -> {
                    List<Contour> result = new ArrayList<>(normalized.size());
                    for(Contour contour : normalized) {
                        Node2 node = contour.toLinkedList();
                        node = Node2.removeStraight(node);
                        node = Node2.removeShortSegments(node, 1.0);
                        node = Node2.removeOddAngleType(node);
                        node = Node2.removeStraight(node);
                        result.add(Contour.fromLinkedList(node));
                    }
                    return result;
                }
        );

        Boolean hasIntersections = measure(
            "Checking for self-intersections",
            () -> ContourIntersectionFinder.hasAnyIntersections(simplified)
        );
        if (hasIntersections) {
            System.err.println("Has intersections!");
            return;
        }

        List<Contour> classified = measure(
            "Classifying",
            () -> ContourClassifier.classify(simplified)
        );

        for (Contour contour : classified) {
            svg.addSegments(contour.toSegments(), 1, contour.type == Contour.Type.INNER ? "red" : "blue", SvgStrokeStyle.SOLID);
            /*
            ContourOffsetter offsetter = new ContourOffsetter(contour);
            List<OffsetResult> offsetList = measure(
                    "Offset",
                    () -> offsetter.offset(-15.0)
            );
            System.out.println("Obtain " + offsetList.size() + " contour" + (offsetList.size() > 1 ? "s" : "")  + " after offset");
            for (OffsetResult offset : offsetList) {
                svg.addSegments(offset.contour.toSegments(), 1, offset.contour.type == Contour.Type.INNER ? "orange" : "cyan", SvgStrokeStyle.SOLID);
                for (Integer originalIndex : offset.originalToOffset.keySet()) {
                    for (Integer offsetIndex : offset.originalToOffset.get(originalIndex)) {
                        svg.addSegments(
                                List.of(new Segment2(offset.originalContour.points.get(originalIndex), offset.contour.points.get(offsetIndex))),
                                1,
                                "gray",
                                SvgStrokeStyle.SOLID
                        );
                    }
                }
            }
            Node2BisectorDivider divider = new Node2BisectorDivider(-15);
            List<Node2ProcessingResult> dividedList = divider.divide(contour.toLinkedList());
            for (Node2ProcessingResult divided : dividedList) {
                svg.addSegments(Node2.toSegments(divided.node), 1, "orange", SvgStrokeStyle.SOLID);
            }
             */
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
