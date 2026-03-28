package com.kniazkov.geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        /*
            Вырожденный случай.
         */
        if (distance == 0.0) {
            Map<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < contour.points.size(); i++) {
                map.put(i, i);
            }
            return List.of(new OffsetContour(contour, map, map));
        }

        /*
            Действительное смещение зависит от типа контура: для внутренних контуров положительное
            смещение означает смещение внутрь.
         */
        if (contour.type == Contour.Type.INNER) {
            distance = -distance;
        }

        /*
            Сначала упрощаем контур перед смещением.

            Идея такая:
            - строим двусвязный циклический список узлов
            - удаляем слишком короткие сегменты, которые при смещении гарантированно
              схлопнутся и породят маленькие петли
            - какие именно узлы удалять, зависит от направления смещения:
              - при смещении наружу удаляем внутренние углы
              - при смещении внутрь удаляем внешние углы

            Порог берем равным абсолютной величине смещения.
         */
        Node2 list = contour.toLinkedList();
        double absDistance = Math.abs(distance);

        if (distance > 0.0) {
            list = Node2.removeNodesByCriteria(
                list,
                node -> !node.isOuter() && node.getDistanceToPrevious() < absDistance
            );
        } else {
            list = Node2.removeNodesByCriteria(
                list,
                node -> node.isOuter() && node.getDistanceToPrevious() < absDistance
            );
        }

        Contour simplified = Contour.fromLinkedList(contour.type, list);

        return List.of();
    }
}
