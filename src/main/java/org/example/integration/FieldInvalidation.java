package org.example.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)  // Мы будем использовать их в методах
public @interface FieldInvalidation {
    String[] path();  // Путь к полю, которое будет invalid
    String type();    // Тип невалидных данных (например, "EMPTY", "INVALID_FORMAT", и т.д.)
}