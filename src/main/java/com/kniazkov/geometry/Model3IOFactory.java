package com.kniazkov.geometry;

import java.util.Locale;

/**
 * Фабрика реализаций Model3IO по имени формата.
 */
public final class Model3IOFactory {
    private Model3IOFactory() {
    }

    /**
     * Возвращает реализацию Model3IO для указанного формата.
     *
     * Поддерживаемые значения:
     * - "stl"
     *
     * Регистр не учитывается.
     */
    public static Model3IO forFormat(String format) {
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Format must not be null or blank");
        }

        String normalized = format.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "stl" -> StlModel3IO.INSTANCE;
            default -> throw new IllegalArgumentException("Unsupported 3D model format: " + format);
        };
    }
}
