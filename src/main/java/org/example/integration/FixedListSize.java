package org.example.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FixedListSize {
    String[] path();  // Путь к полю, которое должно быть списком
    int size();       // Размер списка
}