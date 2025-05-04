package org.example.generator;


import java.util.Arrays;
import java.util.List;

public class Path {
    public static List<String> of(String... segments) {
        return Arrays.asList(segments);
    }
}