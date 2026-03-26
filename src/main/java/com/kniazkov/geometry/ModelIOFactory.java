package com.kniazkov.geometry;

import java.util.Locale;

/**
 * Фабрика реализаций ModelIO по имени формата.
 */
public final class ModelIOFactory {
    private ModelIOFactory() {
    }

    /**
     * Возвращает реализацию ModelIO для указанного формата.
     *
     * Поддерживаемые значения:
     * - "stl"
     *
     * Регистр не учитывается.
     */
    public static ModelIO forFormat(String format) {
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Format must not be null or blank");
        }

        String normalized = format.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "stl" -> StlModelIO.INSTANCE;
            default -> throw new IllegalArgumentException("Unsupported 3D model format: " + format);
        };
    }
}
