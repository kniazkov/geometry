package com.kniazkov.geometry;

import java.util.Optional;

/**
 * Отрезок в двумерном пространстве.
 */
public class Segment2 implements SegmentIntersection {
    public final Point2 a;
    public final Point2 b;


    public Segment2(Point2 a, Point2 b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Длина отрезка.
     */
    public double length() {
        return a.distanceTo(b);
    }

    /**
     * Квадрат длины отрезка.
     * Полезно, когда не хочется лишний раз вызывать sqrt.
     */
    public double lengthSquared() {
        return a.distanceSquaredTo(b);
    }

    /**
     * Вектор от начала отрезка к его концу.
     */
    public Vector2 toVector() {
        return b.subtract(a);
    }

    /**
     * Возвращает точку на отрезке по параметру t:
     * t = 0 -> a
     * t = 1 -> b
     * значения между 0 и 1 дают точки внутри отрезка
     */
    public Point2 pointAt(double t) {
        return new Point2(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t
        );
    }

    /**
     * Возвращает true, если точка лежит на этом отрезке с учетом допуска.
     */
    public boolean containsPoint(Point2 p) {
        // Вектор вдоль отрезка: от a к b
        Vector2 ab = b.subtract(a);

        /* 0.
           Вырожденный случай: отрезок на самом деле является одной точкой.
           Тогда проверяем просто совпадение точки p с a. */
        if (ab.lengthSquared() <= Point2.EPSILON * Point2.EPSILON) {
            return a.approximatelyEquals(p);
        }

        // Вектор от начала отрезка к проверяемой точке
        Vector2 ap = p.subtract(a);

        /* 1.
           Проверяем, что точка лежит на той же прямой.
           Если векторы ab и ap не коллинеарны, точка точно не на отрезке.
           Для этого смотрим на cross product: он должен быть близок к нулю. */
        if (Math.abs(ab.cross(ap)) > Point2.EPSILON) {
            return false;
        }

        /* 2.
           Проверяем, что точка не "до" начала отрезка.
           Если dot(ap, ab) < 0, значит угол тупой, и точка лежит с противоположной стороны от точки a. */
        double dot = ap.dot(ab);
        if (dot < -Point2.EPSILON) {
            return false;
        }

        /* 3.
           Проверяем, что точка не "после" конца отрезка.
           Если dot(ap, ab) больше длины отрезка в квадрате, значит точка ушла дальше точки b. */
        double abLengthSquared = ab.lengthSquared();
        if (dot - abLengthSquared > Point2.EPSILON) {
            return false;
        }

        return true;
    }

    /**
     * Возвращает пересечение этого отрезка с другим:
     * - Optional.empty(), если пересечения нет
     * - Point2, если пересечение состоит из одной точки
     * - Segment2, если отрезки частично совпадают
     *
     * Для совпадающего участка ориентация результирующего сегмента подбирается так, чтобы его начало было ближе к
     * началу текущего отрезка, а конец - ближе к концу текущего отрезка.
     */
    public Optional<SegmentIntersection> intersect(Segment2 other) {
        Vector2 r = this.toVector();
        Vector2 s = other.toVector();

        double rLenSq = r.lengthSquared();
        double sLenSq = s.lengthSquared();

        /* 1.
           Обрабатываем вырожденные случаи:
           один или оба отрезка на самом деле являются точками. */
        if (rLenSq <= Point2.EPSILON * Point2.EPSILON && sLenSq <= Point2.EPSILON * Point2.EPSILON) {
            if (this.a.approximatelyEquals(other.a)) {
                return Optional.of(this.a);
            }
            return Optional.empty();
        }

        if (rLenSq <= Point2.EPSILON * Point2.EPSILON) {
            if (other.containsPoint(this.a)) {
                return Optional.of(this.a);
            }
            return Optional.empty();
        }

        if (sLenSq <= Point2.EPSILON * Point2.EPSILON) {
            if (this.containsPoint(other.a)) {
                return Optional.of(other.a);
            }
            return Optional.empty();
        }

        double rxs = r.cross(s);
        Vector2 qp = other.a.subtract(this.a);
        double qpxr = qp.cross(r);

        /* 2.
           Если векторное произведение почти ноль, отрезки параллельны. */
        if (Math.abs(rxs) <= Point2.EPSILON) {
            // Если при этом точки не лежат на одной прямой, пересечения нет.
            if (Math.abs(qpxr) > Point2.EPSILON) {
                return Optional.empty();
            }

            /* 3.
               Отрезки коллинеарны.
               Проецируем второй отрезок на параметризацию первого:
               this.pointAt(t), где t в [0, 1] соответствует текущему отрезку. */
            double t0 = qp.dot(r) / rLenSq;
            double t1 = t0 + s.dot(r) / rLenSq;

            // Упорядочиваем параметры второго отрезка на оси первого.
            double minT = Math.min(t0, t1);
            double maxT = Math.max(t0, t1);

            // Находим пересечение интервалов [0, 1] и [minT, maxT].
            double overlapStart = Math.max(0.0, minT);
            double overlapEnd = Math.min(1.0, maxT);

            // Интервалы не пересекаются.
            if (overlapStart - overlapEnd > Point2.EPSILON) {
                return Optional.empty();
            }

            // Интервалы касаются в одной точке.
            if (Math.abs(overlapStart - overlapEnd) <= Point2.EPSILON) {
                return Optional.of(this.pointAt(overlapStart));
            }

            /* Есть общий участок.
               Берем его в ориентации текущего отрезка: начало ближе к this.a, конец ближе к this.b. */
            return Optional.of(new Segment2(
                    this.pointAt(overlapStart),
                    this.pointAt(overlapEnd)
            ));
        }

        /* 4.
           Непараллельные прямые.
           Находим параметры пересечения: */
        double t = qp.cross(s) / rxs;
        double u = qp.cross(r) / rxs;

        // Пересечение лежит внутри обоих отрезков, если параметры в [0, 1].
        if (t < -Point2.EPSILON || t > 1.0 + Point2.EPSILON
                || u < -Point2.EPSILON || u > 1.0 + Point2.EPSILON) {
            return Optional.empty();
        }

        return Optional.of(this.pointAt(t));
    }
}
