package com.kniazkov.geometry;

import java.util.List;
import java.util.Map;

/**
 * Находит точки, которые при сдвиге контура внутрь или наружу образуют петли.
 */
public class LoopFinder {
    private final Contour contour;
    private final ContourIntersectionFinder intersectionFinder;

    LoopFinder(Contour contour) {
        this.contour = contour;
        this.intersectionFinder = new ContourIntersectionFinder(contour);
    }

    /**
     * Находит пары точек, которые при смещении друг к другу на расстояние distance
     * образуют траекторию с петлей.
     */
    Map<Integer, Integer> findCorrespondingPairs(double distance) {
        double doubleAbsDistance = Math.abs(distance * 2);
        PointNeighborhoodMap neighbors = new PointNeighborhoodMap(contour.points, doubleAbsDistance);
        Map<Integer, Integer> result = new java.util.HashMap<>();
        int size = contour.points.size();

        for (int i = 0; i < size; i++) {
            int previous = (i - 1 + size) % size;
            int next = (i + 1) % size;
            Point2 point = contour.points.get(i);

            for (int j : neighbors.findNearbyPoints(i)) {
                /*
                    Не проверяем точку саму с собой и двух соседей по кольцу.
                 */
                if (j == i || j == previous || j == next) {
                    continue;
                }

                /*
                    Если пара уже была обработана раньше,
                    повторно ее не проверяем.
                 */
                if (result.containsKey(i) && result.get(i) == j) {
                    continue;
                }
                if (result.containsKey(j) && result.get(j) == i) {
                    continue;
                }

                Point2 candidate = contour.points.get(j);
                if (point.distanceTo(candidate) - doubleAbsDistance > Point2.EPSILON) {
                    continue;
                }

                Segment2 segment = new Segment2(point, candidate);

                /*
                    Для смещения внутрь проверяем, что хорда идет внутри контура.
                    Для смещения наружу проверяем, что хорда идет снаружи.
                 */
                boolean matches;
                if (distance < 0.0) {
                    matches = isFullyInsideContour(segment);
                } else if (distance > 0.0) {
                    matches = isFullyOutsideContour(segment);
                } else {
                    matches = false;
                }

                if (matches) {
                    result.put(i, j);
                    result.put(j, i);
                }
            }
        }

        return java.util.Collections.unmodifiableMap(result);
    }

    /**
     * Возвращает true, если хотя бы часть указанного сегмента лежит внутри контура.
     *
     * Метод работает так:
     * - находит все пересечения сегмента с границей контура
     * - раскладывает сегмент на участки между соседними точками пересечения
     * - для каждого такого участка проверяет середину
     *
     * Если середина какого-либо участка лежит внутри контура,
     * значит и соответствующая часть сегмента проходит внутри.
     *
     * Совпадение с границей само по себе внутренним участком не считается.
     */
    private boolean hasPartInsideContour(Segment2 segment) {
        List<Double> parameters = new java.util.ArrayList<>();
        parameters.add(0.0);
        parameters.add(1.0);

        /*
            Собираем все параметры t точек пересечения сегмента с границей контура.
            Параметризация такая:
            t = 0 соответствует началу сегмента,
            t = 1 соответствует концу сегмента.
         */
        for (ContourIntersection intersection : intersectionFinder.findIntersections(segment)) {
            if (intersection.intersection instanceof Point2 point) {
                double t;

                /*
                    Вычисляем параметр по той координате, где у сегмента
                    изменение больше. Это чуть устойчивее к почти вертикальным
                    и почти горизонтальным случаям.
                 */
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                if (Math.abs(dx) >= Math.abs(dy)) {
                    if (Math.abs(dx) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.x - segment.a.x) / dx;
                } else {
                    if (Math.abs(dy) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.y - segment.a.y) / dy;
                }

                if (t >= 0.0 - Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                    parameters.add(Math.max(0.0, Math.min(1.0, t)));
                }
            } else if (intersection.intersection instanceof Segment2 overlap) {
                /*
                    Если сегмент частично совпадает с границей,
                    добавляем оба конца общего участка.
                 */
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                Point2[] points = {overlap.a, overlap.b};
                for (Point2 point : points) {
                    double t;

                    if (Math.abs(dx) >= Math.abs(dy)) {
                        if (Math.abs(dx) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.x - segment.a.x) / dx;
                    } else {
                        if (Math.abs(dy) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.y - segment.a.y) / dy;
                    }

                    if (t >= 0.0 - Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                        parameters.add(Math.max(0.0, Math.min(1.0, t)));
                    }
                }
            }
        }

        parameters.sort(Double::compare);

        /*
            Удаляем почти совпадающие параметры, чтобы не проверять
            вырожденные нулевые интервалы.
         */
        List<Double> unique = new java.util.ArrayList<>();
        for (double t : parameters) {
            if (unique.isEmpty() || Math.abs(t - unique.get(unique.size() - 1)) > Point2.EPSILON) {
                unique.add(t);
            }
        }

        /*
            Проверяем середины всех непустых участков между соседними
            точками пересечения. Если середина внутри контура,
            значит этот участок проходит внутри.
         */
        for (int i = 0; i < unique.size() - 1; i++) {
            double t1 = unique.get(i);
            double t2 = unique.get(i + 1);

            if (t2 - t1 <= Point2.EPSILON) {
                continue;
            }

            Point2 midpoint = segment.pointAt((t1 + t2) * 0.5);
            if (contour.containsPoint(midpoint)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Возвращает true, если хотя бы часть указанного сегмента лежит снаружи контура.
     *
     * Метод работает так:
     * - находит все пересечения сегмента с границей контура
     * - раскладывает сегмент на участки между соседними точками пересечения
     * - для каждого такого участка проверяет середину
     *
     * Если середина какого-либо участка лежит снаружи контура,
     * значит и соответствующая часть сегмента проходит снаружи.
     *
     * Совпадение с границей само по себе внешним участком не считается.
     */
    private boolean hasPartOutsideContour(Segment2 segment) {
        List<Double> parameters = new java.util.ArrayList<>();
        parameters.add(0.0);
        parameters.add(1.0);

        /*
            Собираем все параметры t точек пересечения сегмента с границей контура.
            Параметризация такая:
            t = 0 соответствует началу сегмента,
            t = 1 соответствует концу сегмента.
         */
        for (ContourIntersection intersection : intersectionFinder.findIntersections(segment)) {
            if (intersection.intersection instanceof Point2 point) {
                double t;

                /*
                    Вычисляем параметр по той координате, где у сегмента
                    изменение больше. Это чуть устойчивее к почти вертикальным
                    и почти горизонтальным случаям.
                 */
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                if (Math.abs(dx) >= Math.abs(dy)) {
                    if (Math.abs(dx) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.x - segment.a.x) / dx;
                } else {
                    if (Math.abs(dy) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.y - segment.a.y) / dy;
                }

                if (t >= 0.0 - Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                    parameters.add(Math.max(0.0, Math.min(1.0, t)));
                }
            } else if (intersection.intersection instanceof Segment2 overlap) {
                /*
                    Если сегмент частично совпадает с границей,
                    добавляем оба конца общего участка.
                 */
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                Point2[] points = {overlap.a, overlap.b};
                for (Point2 point : points) {
                    double t;

                    if (Math.abs(dx) >= Math.abs(dy)) {
                        if (Math.abs(dx) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.x - segment.a.x) / dx;
                    } else {
                        if (Math.abs(dy) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.y - segment.a.y) / dy;
                    }

                    if (t >= 0.0 - Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                        parameters.add(Math.max(0.0, Math.min(1.0, t)));
                    }
                }
            }
        }

        parameters.sort(Double::compare);

        /*
            Удаляем почти совпадающие параметры, чтобы не проверять
            вырожденные нулевые интервалы.
         */
        List<Double> unique = new java.util.ArrayList<>();
        for (double t : parameters) {
            if (unique.isEmpty() || Math.abs(t - unique.get(unique.size() - 1)) > Point2.EPSILON) {
                unique.add(t);
            }
        }

        /*
            Проверяем середины всех непустых участков между соседними
            точками пересечения. Если середина снаружи контура,
            значит этот участок проходит снаружи.
         */
        for (int i = 0; i < unique.size() - 1; i++) {
            double t1 = unique.get(i);
            double t2 = unique.get(i + 1);

            if (t2 - t1 <= Point2.EPSILON) {
                continue;
            }

            Point2 midpoint = segment.pointAt((t1 + t2) * 0.5);
            if (!contour.containsPoint(midpoint)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Возвращает true, если весь указанный сегмент лежит внутри контура.
     *
     * Метод работает так:
     * - находит все пересечения сегмента с границей контура
     * - раскладывает сегмент на участки между соседними точками пересечения
     * - для каждого непустого участка проверяет середину
     *
     * Если хотя бы один такой участок имеет середину снаружи контура,
     * значит весь сегмент внутри не лежит.
     *
     * Совпадение с границей само по себе внутренним участком не считается.
     */
    private boolean isFullyInsideContour(Segment2 segment) {
        List<Double> parameters = new java.util.ArrayList<>();
        parameters.add(0.0);
        parameters.add(1.0);

        /*
            Собираем все параметры t точек пересечения сегмента с границей контура.
         */
        for (ContourIntersection intersection : intersectionFinder.findIntersections(segment)) {
            if (intersection.intersection instanceof Point2 point) {
                double t;
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                if (Math.abs(dx) >= Math.abs(dy)) {
                    if (Math.abs(dx) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.x - segment.a.x) / dx;
                } else {
                    if (Math.abs(dy) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.y - segment.a.y) / dy;
                }

                if (t >= -Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                    parameters.add(Math.max(0.0, Math.min(1.0, t)));
                }
            } else if (intersection.intersection instanceof Segment2 overlap) {
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                Point2[] points = {overlap.a, overlap.b};
                for (Point2 point : points) {
                    double t;

                    if (Math.abs(dx) >= Math.abs(dy)) {
                        if (Math.abs(dx) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.x - segment.a.x) / dx;
                    } else {
                        if (Math.abs(dy) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.y - segment.a.y) / dy;
                    }

                    if (t >= -Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                        parameters.add(Math.max(0.0, Math.min(1.0, t)));
                    }
                }
            }
        }

        parameters.sort(Double::compare);

        /*
            Удаляем почти совпадающие параметры.
         */
        List<Double> unique = new java.util.ArrayList<>();
        for (double t : parameters) {
            if (unique.isEmpty() || Math.abs(t - unique.get(unique.size() - 1)) > Point2.EPSILON) {
                unique.add(t);
            }
        }

        boolean hasInteriorPart = false;

        /*
            Все непустые участки между соседними точками пересечения
            должны иметь середину внутри контура.
         */
        for (int i = 0; i < unique.size() - 1; i++) {
            double t1 = unique.get(i);
            double t2 = unique.get(i + 1);

            if (t2 - t1 <= Point2.EPSILON) {
                continue;
            }

            Point2 midpoint = segment.pointAt((t1 + t2) * 0.5);
            if (!contour.containsPoint(midpoint)) {
                return false;
            }

            hasInteriorPart = true;
        }

        return hasInteriorPart;
    }

    /**
     * Возвращает true, если весь указанный сегмент лежит снаружи контура.
     *
     * Метод работает так:
     * - находит все пересечения сегмента с границей контура
     * - раскладывает сегмент на участки между соседними точками пересечения
     * - для каждого непустого участка проверяет середину
     *
     * Если хотя бы один такой участок имеет середину внутри контура,
     * значит весь сегмент снаружи не лежит.
     *
     * Совпадение с границей само по себе внешним участком не считается.
     */
    private boolean isFullyOutsideContour(Segment2 segment) {
        List<Double> parameters = new java.util.ArrayList<>();
        parameters.add(0.0);
        parameters.add(1.0);

        /*
            Собираем все параметры t точек пересечения сегмента с границей контура.
         */
        for (ContourIntersection intersection : intersectionFinder.findIntersections(segment)) {
            if (intersection.intersection instanceof Point2 point) {
                double t;
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                if (Math.abs(dx) >= Math.abs(dy)) {
                    if (Math.abs(dx) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.x - segment.a.x) / dx;
                } else {
                    if (Math.abs(dy) <= Point2.EPSILON) {
                        continue;
                    }
                    t = (point.y - segment.a.y) / dy;
                }

                if (t >= -Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                    parameters.add(Math.max(0.0, Math.min(1.0, t)));
                }
            } else if (intersection.intersection instanceof Segment2 overlap) {
                double dx = segment.b.x - segment.a.x;
                double dy = segment.b.y - segment.a.y;

                Point2[] points = {overlap.a, overlap.b};
                for (Point2 point : points) {
                    double t;

                    if (Math.abs(dx) >= Math.abs(dy)) {
                        if (Math.abs(dx) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.x - segment.a.x) / dx;
                    } else {
                        if (Math.abs(dy) <= Point2.EPSILON) {
                            continue;
                        }
                        t = (point.y - segment.a.y) / dy;
                    }

                    if (t >= -Point2.EPSILON && t <= 1.0 + Point2.EPSILON) {
                        parameters.add(Math.max(0.0, Math.min(1.0, t)));
                    }
                }
            }
        }

        parameters.sort(Double::compare);

        /*
            Удаляем почти совпадающие параметры.
         */
        List<Double> unique = new java.util.ArrayList<>();
        for (double t : parameters) {
            if (unique.isEmpty() || Math.abs(t - unique.get(unique.size() - 1)) > Point2.EPSILON) {
                unique.add(t);
            }
        }

        boolean hasExteriorPart = false;

        /*
            Все непустые участки между соседними точками пересечения
            должны иметь середину снаружи контура.
         */
        for (int i = 0; i < unique.size() - 1; i++) {
            double t1 = unique.get(i);
            double t2 = unique.get(i + 1);

            if (t2 - t1 <= Point2.EPSILON) {
                continue;
            }

            Point2 midpoint = segment.pointAt((t1 + t2) * 0.5);
            if (contour.containsPoint(midpoint)) {
                return false;
            }

            hasExteriorPart = true;
        }

        return hasExteriorPart;
    }
}
