package com.kniazkov.geometry;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация Model3IO для STL-файлов.
 *
 * Чтение:
 * - binary STL
 * - ASCII STL
 *
 * Запись:
 * - только binary STL
 */
public class StlModel3IO implements Model3IO {

    /**
     * Готовый singleton-экземпляр.
     * Класс не содержит состояния, так что одного экземпляра достаточно.
     */
    public static final StlModel3IO INSTANCE = new StlModel3IO();

    private StlModel3IO() {
    }

    @Override
    public Model3 load(Path path) throws IOException {
        if (isBinaryStl(path)) {
            return loadBinary(path);
        }
        return loadAscii(path);
    }

    @Override
    public void save(Path path, Model3 model) throws IOException {
        try (var out = Files.newOutputStream(path)) {
            writeBinaryStl(out, model);
        }
    }

    /**
     * Пытается определить, является ли STL бинарным.
     *
     * Надежная эвристика такая:
     * - binary STL имеет:
     *   80 байт заголовка
     *   4 байта числа треугольников
     *   далее по 50 байт на каждый треугольник
     *
     * Поэтому если размер файла совпадает с формулой:
     * 84 + triangleCount * 50
     * то почти наверняка это binary STL.
     *
     * Это лучше, чем проверять только слово "solid" в начале,
     * потому что binary STL тоже иногда начинается с "solid".
     */
    private boolean isBinaryStl(Path path) throws IOException {
        long fileSize = Files.size(path);
        if (fileSize < 84) {
            return false;
        }

        byte[] header = new byte[84];
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            int read = in.read(header);
            if (read < 84) {
                return false;
            }
        }

        long triangleCount = readUnsignedIntLE(header, 80);
        long expectedSize = 84L + triangleCount * 50L;

        return expectedSize == fileSize;
    }

    /**
     * Читает binary STL.
     */
    private Model3 loadBinary(Path path) throws IOException {
        List<Triangle3> triangles = new ArrayList<>();

        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            // Пропускаем 80-байтный заголовок.
            skipExactly(in, 80);

            long triangleCount = readUnsignedIntLE(in);

            for (long i = 0; i < triangleCount; i++) {
                // Нормаль из файла читаем, но пока не используем.
                readFloatLE(in);
                readFloatLE(in);
                readFloatLE(in);

                Point3 a = new Point3(
                        readFloatLE(in),
                        readFloatLE(in),
                        readFloatLE(in)
                );
                Point3 b = new Point3(
                        readFloatLE(in),
                        readFloatLE(in),
                        readFloatLE(in)
                );
                Point3 c = new Point3(
                        readFloatLE(in),
                        readFloatLE(in),
                        readFloatLE(in)
                );

                // Атрибутный байткаунт в обычном STL почти всегда 0.
                readUnsignedShortLE(in);

                triangles.add(new Triangle3(a, b, c));
            }
        }

        return new Model3(triangles);
    }

    /**
     * Читает ASCII STL.
     *
     * Здесь мы не пытаемся строить большой формальный парсер.
     * Для STL обычно достаточно искать строки вида:
     * vertex x y z
     *
     * Каждые 3 вершины образуют один треугольник.
     */
    private Model3 loadAscii(Path path) throws IOException {
        List<Triangle3> triangles = new ArrayList<>();
        List<Point3> vertices = new ArrayList<>(3);

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // STL ASCII использует строки вида:
                // vertex x y z
                if (line.startsWith("vertex")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 4) {
                        throw new IllegalArgumentException("Invalid ASCII STL vertex line: " + line);
                    }

                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);

                    vertices.add(new Point3(x, y, z));

                    if (vertices.size() == 3) {
                        triangles.add(new Triangle3(
                                vertices.get(0),
                                vertices.get(1),
                                vertices.get(2)
                        ));
                        vertices.clear();
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value in ASCII STL", e);
        }

        if (!vertices.isEmpty()) {
            throw new IllegalArgumentException("Incomplete triangle in ASCII STL");
        }

        return new Model3(triangles);
    }

    /**
     * Пишет binary STL.
     *
     * Формат:
     * - 80 байт заголовка
     * - 4 байта числа треугольников
     * - далее по 50 байт на каждый треугольник:
     *   normal(3 float) + 3 вершины(по 3 float) + attribute byte count(ushort)
     */
    private void writeBinaryStl(java.io.OutputStream out, Model3 model) throws IOException {
        byte[] header = new byte[80];
        byte[] title = "Generated by StlModel3IO".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(title, 0, header, 0, Math.min(title.length, header.length));
        out.write(header);

        writeUnsignedIntLE(out, model.triangles.size());

        for (Triangle3 triangle : model.triangles) {
            Vector3 normal = triangle.normal();
            Vector3 normalToWrite;

            // Если треугольник вырожденный, нормаль записываем нулевую.
            if (normal.lengthSquared() <= Point3.EPSILON * Point3.EPSILON) {
                normalToWrite = new Vector3(0.0, 0.0, 0.0);
            } else {
                normalToWrite = normal.normalized();
            }

            writeFloatLE(out, (float) normalToWrite.x);
            writeFloatLE(out, (float) normalToWrite.y);
            writeFloatLE(out, (float) normalToWrite.z);

            writePoint3LE(out, triangle.a);
            writePoint3LE(out, triangle.b);
            writePoint3LE(out, triangle.c);

            // attribute byte count
            writeUnsignedShortLE(out, 0);
        }
    }

    private void writePoint3LE(java.io.OutputStream out, Point3 p) throws IOException {
        writeFloatLE(out, (float) p.x);
        writeFloatLE(out, (float) p.y);
        writeFloatLE(out, (float) p.z);
    }

    private void skipExactly(InputStream in, int count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                int b = in.read();
                if (b < 0) {
                    throw new IOException("Unexpected end of file");
                }
                remaining--;
            } else {
                remaining -= skipped;
            }
        }
    }

    private float readFloatLE(InputStream in) throws IOException {
        int b0 = readByte(in);
        int b1 = readByte(in);
        int b2 = readByte(in);
        int b3 = readByte(in);

        int bits = (b0)
                | (b1 << 8)
                | (b2 << 16)
                | (b3 << 24);

        return Float.intBitsToFloat(bits);
    }

    private int readUnsignedShortLE(InputStream in) throws IOException {
        int b0 = readByte(in);
        int b1 = readByte(in);
        return b0 | (b1 << 8);
    }

    private long readUnsignedIntLE(InputStream in) throws IOException {
        int b0 = readByte(in);
        int b1 = readByte(in);
        int b2 = readByte(in);
        int b3 = readByte(in);

        return ((long) b0)
                | ((long) b1 << 8)
                | ((long) b2 << 16)
                | ((long) b3 << 24);
    }

    private long readUnsignedIntLE(byte[] bytes, int offset) {
        int b0 = bytes[offset] & 0xFF;
        int b1 = bytes[offset + 1] & 0xFF;
        int b2 = bytes[offset + 2] & 0xFF;
        int b3 = bytes[offset + 3] & 0xFF;

        return ((long) b0)
                | ((long) b1 << 8)
                | ((long) b2 << 16)
                | ((long) b3 << 24);
    }

    private int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new IOException("Unexpected end of file");
        }
        return b;
    }

    private void writeFloatLE(java.io.OutputStream out, float value) throws IOException {
        int bits = Float.floatToIntBits(value);
        out.write(bits & 0xFF);
        out.write((bits >>> 8) & 0xFF);
        out.write((bits >>> 16) & 0xFF);
        out.write((bits >>> 24) & 0xFF);
    }

    private void writeUnsignedShortLE(java.io.OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private void writeUnsignedIntLE(java.io.OutputStream out, long value) throws IOException {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
    }
}