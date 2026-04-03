package com.kniazkov.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Поисковик пересечений для одного базового контура.
 *
 * При создании:
 * - сохраняет сам контур
 * - разворачивает его в список сегментов
 * - строит карту сегментов для ускорения поиска кандидатов на пересечение
 *
 * После этого объект можно использовать:
 * - для поиска самопересечений контура
 * - для поиска пересечений с другим контуром
 */
public class ContourIntersectionFinder {
    private final List<Segment2> segments;
    private final SegmentIntersectionMap intersectionMap;


    public ContourIntersectionFinder(Contour contour) {
        this.segments = contour.toSegments();
        this.intersectionMap = new SegmentIntersectionMap(segments);
    }

    /**
     * Ищет все самопересечения контура.
     *
     * Соседние сегменты не проверяются, потому что у замкнутого контура
     * они пересекаются по определению в общей вершине.
     *
     * Также каждая пара сегментов проверяется только один раз.
     */
    public List<ContourIntersection> findSelfIntersections() {
        List<ContourIntersection> result = new ArrayList<>();
        int size = segments.size();

        for (int i = 0; i < size; i++) {
            Segment2 first = segments.get(i);
            Set<Integer> candidates = intersectionMap.find(first);

            for (int j : candidates) {
                /*
                    Не проверяем сегмент с самим собой.
                 */
                if (j == i) {
                    continue;
                }

                /*
                    Чтобы не проверять одну и ту же пару дважды,
                    оставляем только пары i < j.
                 */
                if (j < i) {
                    continue;
                }

                /*
                    Соседние сегменты замкнутого контура пересекаются в общей вершине,
                    и это не считается самопересечением.
                 */
                if (areNeighbors(i, j, size)) {
                    continue;
                }

                Segment2 second = segments.get(j);
                Optional<SegmentIntersection> intersection = first.intersect(second);
                if (intersection.isPresent()) {
                    result.add(new ContourIntersection(intersection.get(), i, j));
                }
            }
        }

        return result;
    }

    /**
     * Ищет все пересечения базового контура с одним сегментом.
     */
    public List<ContourIntersection> findIntersections(Segment2 segment) {
        List<ContourIntersection> result = new ArrayList<>();
        Set<Integer> candidates = intersectionMap.find(segment);

        for (int index : candidates) {
            Segment2 candidate = segments.get(index);
            Optional<SegmentIntersection> intersection = candidate.intersect(segment);
            if (intersection.isPresent()) {
                result.add(new ContourIntersection(intersection.get(), index, -1));
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Ищет все пересечения базового контура с другим контуром.
     */
    public List<ContourIntersection> findIntersections(Contour other) {
        List<ContourIntersection> result = new ArrayList<>();
        List<Segment2> otherSegments = other.toSegments();

        for (int j = 0; j < otherSegments.size(); j++) {
            Segment2 otherSegment = otherSegments.get(j);
            Set<Integer> candidates = intersectionMap.find(otherSegment);

            for (int i : candidates) {
                Segment2 ownSegment = segments.get(i);
                Optional<SegmentIntersection> intersection = ownSegment.intersect(otherSegment);
                if (intersection.isPresent()) {
                    result.add(new ContourIntersection(intersection.get(), i, j));
                }
            }
        }

        return result;
    }

    /**
     * Проверяет набор контуров на самопересечения и попарные пересечения.
     *
     * Возвращает true, если найдено хотя бы одно пересечение:
     * - либо самопересечение хотя бы одного контура
     * - либо пересечение между двумя разными контурами
     */
    public static boolean hasAnyIntersections(List<Contour> contours) {
        List<ContourIntersectionFinder> finders = new ArrayList<>(contours.size());

        /*
            Сначала проверяем самопересечения каждого контура.
         */
        for (Contour contour : contours) {
            ContourIntersectionFinder finder = new ContourIntersectionFinder(contour);
            if (!finder.findSelfIntersections().isEmpty()) {
                return true;
            }
            finders.add(finder);
        }

        /*
            Затем проверяем попарные пересечения разных контуров.
            Достаточно рассматривать только пары i < j.
         */
        for (int i = 0; i < finders.size(); i++) {
            ContourIntersectionFinder finder = finders.get(i);

            for (int j = i + 1; j < contours.size(); j++) {
                if (!finder.findIntersections(contours.get(j)).isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Возвращает true, если два сегмента являются соседними
     * в замкнутом контуре.
     */
    private static boolean areNeighbors(int firstIndex, int secondIndex, int size) {
        int diff = Math.abs(firstIndex - secondIndex);
        return diff == 1 || diff == size - 1;
    }
}
