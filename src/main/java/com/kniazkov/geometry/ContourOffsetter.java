package com.kniazkov.geometry;

import java.util.List;

/**
 * Заготовка для смещения контура внутрь или наружу.
 *
 * Предполагается, что:
 * - положительное смещение задает движение наружу
 * - отрицательное смещение задает движение внутрь
 *
 * Пока класс содержит только API, без реализации алгоритма.
 */
public class ContourOffsetter {
    private final Contour contour;


    public ContourOffsetter(Contour contour) {
        this.contour = contour;
    }

    /**
     * Возвращает исходный контур, с которым работает смещатель.
     */
    public Contour getContour() {
        return contour;
    }

    /**
     * Смещает контур на указанное расстояние.
     *
     * Положительное значение означает смещение наружу,
     * отрицательное - внутрь.
     *
     * Пока метод не реализован.
     */
    public List<OffsetContour> offset(double distance) {

        return List.of();
    }
}
