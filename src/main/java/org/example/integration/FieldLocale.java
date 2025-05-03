package org.example.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldLocale {
    String[] path();  // Путь к полю, которое должно быть локализовано
    String locale();  // Локаль, например, "ru-RU", "en-US" и т.д.
}

