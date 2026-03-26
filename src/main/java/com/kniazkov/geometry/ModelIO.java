package com.kniazkov.geometry;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Интерфейс для загрузки и сохранения 3D-моделей.
 */
public interface ModelIO {

    /**
     * Загружает модель из файла.
     *
     * @param path путь к файлу модели
     * @return загруженная модель
     * @throws IOException если произошла ошибка чтения файла
     * @throws IllegalArgumentException если содержимое файла некорректно
     */
    Model load(Path path) throws IOException;

    /**
     * Сохраняет модель в файл.
     *
     * @param path путь к файлу модели
     * @param model модель для сохранения
     * @throws IOException если произошла ошибка записи файла
     */
    void save(Path path, Model model) throws IOException;
}
