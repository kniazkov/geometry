package com.kniazkov.geometry;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class Test {

    public static void main(String[] args) throws IOException {
        ModelIO loader = ModelIOFactory.forFormat("stl");

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
            () -> model.sliceAt(1000)
        );

        SvgBuilder svg = new SvgBuilder();
        svg.addSegments(segments, 1,  "blue", SvgStrokeStyle.SOLID);
        svg.save(Paths.get("result.svg"), 1.0);
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
