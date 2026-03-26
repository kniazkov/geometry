package com.kniazkov.geometry;

import java.io.IOException;
import java.nio.file.Paths;

public class Test {
    public static void main(String[] args) throws IOException {
        Model3IO io = Model3IOFactory.forFormat("stl");
        Model3 model = io.load(Paths.get("D:\\skull.stl"));
        System.out.println("Loaded " + model.triangles.size() + " triangles");
        io.save(Paths.get("D:\\skull2.stl"), model);
    }
}
