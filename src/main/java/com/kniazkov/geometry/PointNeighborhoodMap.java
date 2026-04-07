package com.kniazkov.geometry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Пространственный индекс точек по грубым ячейкам.
 *
 * Используется для быстрого поиска точек, находящихся рядом
 * с заданной точкой.
 *
 * Каждая точка:
 * - сохраняется в своей ячейке в карте pointCells
 * - добавляется в карту ближайших точек не только в свою ячейку,
 *   но и в 8 соседних ячеек
 *
 * Это позволяет затем по индексу точки быстро получить множество
 * ближайших кандидатов без отдельного обхода соседних ячеек.
 */
public class PointNeighborhoodMap {
    private final Map<Integer, Cell> pointCells = new HashMap<>();
    private final Map<Cell, Set<Integer>> nearbyPoints = new HashMap<>();


    public PointNeighborhoodMap(List<Point2> points, double distance) {
        if (distance <= 0.0) {
            throw new IllegalArgumentException("distance must be > 0");
        }

        double accuracy = 1.0 / distance;

        for (int i = 0; i < points.size(); i++) {
            Cell cell = points.get(i).toCell(accuracy);
            pointCells.put(i, cell);

            /*
                Точка добавляется не только в свою ячейку,
                но и в 8 соседних.
             */
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    add(cell.offset(dx, dy), i);
                }
            }
        }
    }

    /**
     * Возвращает ячейку, в которой лежит точка с указанным индексом.
     */
    public Cell getCell(int pointIndex) {
        Cell cell = pointCells.get(pointIndex);
        if (cell == null) {
            throw new IllegalArgumentException("Unknown point index: " + pointIndex);
        }
        return cell;
    }

    /**
     * Возвращает индексы ближайших точек для точки с указанным индексом.
     *
     * Результат включает все точки, которые были добавлены
     * в ту же ячейку поиска.
     *
     * Возвращается неизменяемое представление множества.
     */
    public Set<Integer> findNearbyPoints(int pointIndex) {
        Cell cell = getCell(pointIndex);
        Set<Integer> result = nearbyPoints.get(cell);
        if (result == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Добавляет индекс точки в индекс по указанной ячейке.
     */
    private void add(Cell cell, int pointIndex) {
        nearbyPoints.computeIfAbsent(cell, key -> new HashSet<>()).add(pointIndex);
    }
}
