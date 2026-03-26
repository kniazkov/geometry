package com.kniazkov.geometry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Накопитель SVG-сегментов с поддержкой групп.
 *
 * Каждая группа сегментов имеет свои параметры отображения:
 * - толщину
 * - цвет
 * - стиль линии
 */
public class SvgBuilder {

    private final List<Group> groups = new ArrayList<>();

    /**
     * Добавляет группу сегментов с заданными параметрами отображения.
     */
    public void addSegments(List<Segment2> segments, double thickness, String color, SvgStrokeStyle style) {
        if (segments == null) {
            throw new IllegalArgumentException("Segments must not be null");
        }
        if (thickness <= 0.0) {
            throw new IllegalArgumentException("Thickness must be positive");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Color must not be null or blank");
        }
        if (style == null) {
            throw new IllegalArgumentException("Style must not be null");
        }

        groups.add(new Group(new ArrayList<>(segments), thickness, color, style));
    }

    /**
     * Сохраняет накопленные сегменты в SVG-файл.
     *
     * Правило масштаба:
     * - scale = 10   -> координаты уменьшаются в 10 раз
     * - scale = 0.1  -> координаты увеличиваются в 10 раз
     *
     * То есть экранная координата = геометрическая координата / scale
     */
    public void save(Path path, double scale) throws IOException {
        if (scale <= 0.0) {
            throw new IllegalArgumentException("Scale must be positive");
        }

        List<Segment2> allSegments = new ArrayList<>();
        for (Group group : groups) {
            allSegments.addAll(group.segments);
        }

        if (allSegments.isEmpty()) {
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100">
                        </svg>
                        """);
            }
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Segment2 segment : allSegments) {
            minX = Math.min(minX, Math.min(segment.a.x, segment.b.x));
            minY = Math.min(minY, Math.min(segment.a.y, segment.b.y));
            maxX = Math.max(maxX, Math.max(segment.a.x, segment.b.x));
            maxY = Math.max(maxY, Math.max(segment.a.y, segment.b.y));
        }

        double svgMinX = minX / scale;
        double svgMinY = minY / scale;
        double svgWidth = (maxX - minX) / scale;
        double svgHeight = (maxY - minY) / scale;

        // Чтобы SVG не схлопнулся в линию или точку.
        if (svgWidth <= 0.0) {
            svgWidth = 1.0;
        }
        if (svgHeight <= 0.0) {
            svgHeight = 1.0;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write(String.format(
                Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"%.6f %.6f %.6f %.6f\">\n",
                svgMinX,
                svgMinY,
                svgWidth,
                svgHeight
            ));

            writer.write("  <g fill=\"none\">\n");

            for (Group group : groups) {
                writeGroup(writer, group, scale);
            }

            writer.write("  </g>\n");
            writer.write("</svg>\n");
        }
    }

    private void writeGroup(BufferedWriter writer, Group group, double scale) throws IOException {
        writer.write(String.format(
            Locale.US,
            "    <g stroke=\"%s\" stroke-width=\"%.6f\"",
            escapeXml(group.color),
            group.thickness / scale
        ));

        if (!"none".equals(group.style.dashArray)) {
            writer.write(String.format(
                Locale.US,
                " stroke-dasharray=\"%s\"",
                group.style.dashArray
            ));
        }

        writer.write(">\n");

        for (Segment2 segment : group.segments) {
            writer.write(String.format(
                Locale.US,
                "      <line x1=\"%.6f\" y1=\"%.6f\" x2=\"%.6f\" y2=\"%.6f\" />\n",
                segment.a.x / scale,
                segment.a.y / scale,
                segment.b.x / scale,
                segment.b.y / scale
            ));
        }

        writer.write("    </g>\n");
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static class Group {
        public final List<Segment2> segments;
        public final double thickness;
        public final String color;
        public final SvgStrokeStyle style;

        public Group(List<Segment2> segments, double thickness, String color, SvgStrokeStyle style) {
            this.segments = segments;
            this.thickness = thickness;
            this.color = color;
            this.style = style;
        }
    }
}
