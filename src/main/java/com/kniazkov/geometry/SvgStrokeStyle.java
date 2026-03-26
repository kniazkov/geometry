package com.kniazkov.geometry;

/**
 * Стиль линии в SVG.
 */
public enum SvgStrokeStyle {
    SOLID("none"),
    DASHED("8,4"),
    DOTTED("2,4");

    public final String dashArray;

    SvgStrokeStyle(String dashArray) {
        this.dashArray = dashArray;
    }
}
