package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Построитель прямолинейного скелета для одного простого контура.
 *
 * На вход подается текущее кольцо вершин wavefront'а.
 * Предполагается, что:
 * - контур простой
 * - вершины идут в порядке обхода
 * - ориентация уже приведена к нужной
 *
 * Текущая версия содержит только каркас алгоритма.
 * Реальная обработка событий будет добавляться постепенно.
 */
public class StraightSkeletonBuilder {
    /**
     * Любой узел текущего фронта.
     * Так как список циклический, этого достаточно,
     * чтобы обойти весь активный контур.
     */
    private Node2 front;

    /**
     * Накопленные вершины скелета.
     */
    private final List<SkeletonVertex> vertices = new ArrayList<>();

    /**
     * Накопленные ребра скелета.
     */
    private final List<SkeletonEdge> edges = new ArrayList<>();

    /**
     * Накопленные грани скелета.
     *
     * Пока это просто контейнер под будущую логику.
     */
    private final List<SkeletonFace> faces = new ArrayList<>();

    /**
     * Текущее "время" процесса схлопывания.
     */
    private double currentTime;


    public StraightSkeletonBuilder(Node2 front) {
        if (front == null) {
            throw new IllegalArgumentException("Front must not be null");
        }

        this.front = front;
    }

    /**
     * Строит прямолинейный скелет.
     *
     * Пока что метод возвращает пустой результат
     * и служит входной точкой для пошаговой реализации алгоритма.
     */
    public StraightSkeleton build() {
        initialize();

        /*
            Здесь постепенно появятся:
            - поиск ближайшего события
            - продвижение фронта к моменту события
            - добавление вершин и ребер скелета
            - перестройка фронта
         */

        return new StraightSkeleton(vertices, edges, faces);
    }

    /**
     * Выполняет начальную подготовку перед построением.
     */
    private void initialize() {
        currentTime = 0.0;
    }

    /**
     * Возвращает текущий момент времени процесса.
     */
    public double getCurrentTime() {
        return currentTime;
    }

    /**
     * Возвращает любой узел текущего фронта.
     */
    public Node2 getFront() {
        return front;
    }
}
