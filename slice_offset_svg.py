#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import math
from pathlib import Path

from meshlib import mrmeshpy as mm


# ----------------------------
# SVG
# ----------------------------

def write_svg(polylines_2d, out_path: Path, stroke_width: float = 1.0, margin: float = 5.0):
    """
    polylines_2d: list[list[(x, y)]]
    """
    if not polylines_2d:
        raise ValueError("Нет 2D-контуров для записи в SVG")

    xs = [p[0] for poly in polylines_2d for p in poly]
    ys = [p[1] for poly in polylines_2d for p in poly]

    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)

    width = max_x - min_x
    height = max_y - min_y

    vb_x = min_x - margin
    vb_y = min_y - margin
    vb_w = width + 2 * margin
    vb_h = height + 2 * margin

    def path_d(poly):
        if not poly:
            return ""
        parts = [f"M {poly[0][0]:.6f} {poly[0][1]:.6f}"]
        for x, y in poly[1:]:
            parts.append(f"L {x:.6f} {y:.6f}")
        # замыкаем, если не замкнут
        if poly[0] != poly[-1]:
            parts.append("Z")
        return " ".join(parts)

    svg_parts = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        (
            f'<svg xmlns="http://www.w3.org/2000/svg" '
            f'viewBox="{vb_x:.6f} {vb_y:.6f} {vb_w:.6f} {vb_h:.6f}" '
            f'width="{vb_w:.6f}" height="{vb_h:.6f}">'
        ),
        '<g fill="none" stroke="black">'
    ]

    for poly in polylines_2d:
        d = path_d(poly)
        svg_parts.append(f'<path d="{d}" stroke-width="{stroke_width:.6f}" />')

    svg_parts.append("</g>")
    svg_parts.append("</svg>")

    out_path.write_text("\n".join(svg_parts), encoding="utf-8")


# ----------------------------
# Геометрия
# ----------------------------

def polygon_area_2d(poly):
    s = 0.0
    n = len(poly)
    for i in range(n):
        x1, y1 = poly[i]
        x2, y2 = poly[(i + 1) % n]
        s += x1 * y2 - x2 * y1
    return 0.5 * s


def ensure_closed(poly):
    if not poly:
        return poly
    if poly[0] != poly[-1]:
        return poly + [poly[0]]
    return poly


def choose_largest_closed_contour(contours_2d):
    closed = []
    for poly in contours_2d:
        poly = ensure_closed(poly)
        if len(poly) >= 4:
            closed.append(poly)

    if not closed:
        raise ValueError("Не удалось получить замкнутый контур сечения")

    return max(closed, key=lambda p: abs(polygon_area_2d(p)))


def contour3d_to_xy_list(mesh, contour3d):
    """
    contour3d: последовательность точек MeshLib Vector3f
    """
    out = []
    for ep in contour3d:
        p = mesh.edgePoint(ep)   # EdgePoint -> Vector3f
        out.append((float(p.x), float(p.y)))
    return out


# ----------------------------
# MeshLib helpers
# ----------------------------

def extract_slice_contours(mesh, z_height):
    """
    Возвращает список 3D-контуров сечения плоскостью Z = z_height.

    ВАЖНО:
    В MeshLib имя конструктора plane может отличаться по версии.
    Тут используется типовая идея: plane с normal=(0,0,1) и смещением z_height.
    """
    # В зависимости от версии может быть:
    # plane = mm.Plane3f(mm.Vector3f(0, 0, 1), z_height)
    # или
    # plane = mm.Plane3f(mm.Vector3f(0, 0, 1), -z_height)
    # или конструктор через point+normal.
    #
    # Проверь dir(mm) / help(mm.Plane3f), если упрется.
    plane = mm.Plane3f(mm.Vector3f(0.0, 0.0, 1.0), z_height)

    # По GitHub discussion у них используется именно extractPlaneSections(inputMesh, plane)
    sections = mm.extractPlaneSections(mesh, plane)
    return sections


def convert_3d_sections_to_2d_contours(mesh, sections_3d):
    """
    Преобразует 3D sections в список 2D полилиний в XY.

    В новых биндингах у MeshLib фигурирует convertContour / copyContour для контуров.
    Но для сечения плоскостью Z=const проще проецировать вручную в XY.
    """
    contours_2d = []
    for section in sections_3d:
        poly = contour3d_to_xy_list(mesh, section)
        if len(poly) >= 3:
            contours_2d.append(poly)
    return contours_2d


from shapely.geometry import Polygon, MultiPolygon

def build_inner_offset_with_shapely(contour_2d, offset_abs):
    contour_2d = ensure_closed(contour_2d)

    # shapely не любит самопересечения и мусор
    poly = Polygon(contour_2d)
    if not poly.is_valid:
        poly = poly.buffer(0)

    if poly.is_empty:
        raise ValueError("Исходный контур некорректный после исправления")

    # inward offset
    inner = poly.buffer(-float(offset_abs))

    if inner.is_empty:
        raise ValueError("Не удалось построить внутренний offset. Возможно, offset слишком большой.")

    polys = []
    if isinstance(inner, Polygon):
        polys = [inner]
    elif isinstance(inner, MultiPolygon):
        polys = list(inner.geoms)
    else:
        raise ValueError(f"Неожиданный тип результата offset: {type(inner)}")

    out = []
    for p in polys:
        coords = list(p.exterior.coords)
        polyline = [(float(x), float(y)) for x, y in coords]
        if len(polyline) >= 4:
            out.append(polyline)

    if not out:
        raise ValueError("После offset не осталось валидных контуров")

    return out

# ----------------------------
# Main
# ----------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Slice STL at given Z, take contour, offset inward, save SVG"
    )
    parser.add_argument("input_stl", help="Путь к STL")
    parser.add_argument("z", type=float, help="Высота сечения по Z")
    parser.add_argument("--offset", type=float, required=True, help="Внутренний offset в единицах модели")
    parser.add_argument("--output", help="Путь к SVG. По умолчанию <input>_z_<z>_off_<offset>.svg")
    args = parser.parse_args()

    input_path = Path(args.input_stl)
    if not input_path.exists():
        raise FileNotFoundError(f"Файл не найден: {input_path}")

    out_path = Path(args.output) if args.output else input_path.with_name(
        f"{input_path.stem}_z_{args.z:g}_off_{args.offset:g}.svg"
    )

    mesh = mm.loadMesh(str(input_path))

    # 1) режем плоскостью
    sections_3d = extract_slice_contours(mesh, args.z)
    if not sections_3d:
        raise ValueError(f"На высоте z={args.z} сечение пустое")

    # 2) переводим в 2D XY
    contours_2d = convert_3d_sections_to_2d_contours(mesh, sections_3d)

    # 3) если контуров несколько, берем самый большой замкнутый
    base_contour = choose_largest_closed_contour(contours_2d)

    # 4) inward offset
    offset_contours = build_inner_offset_with_shapely(base_contour, abs(args.offset))

    # 5) пишем svg
    write_svg(offset_contours + contours_2d, out_path)

    print(f"OK: {out_path}")


if __name__ == "__main__":
    main()
    