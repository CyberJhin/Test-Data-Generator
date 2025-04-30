package org.example.DTO;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Аннотация для указания дефолтных локалей на уровне DTO-класса.
 * Можно задавать одну или несколько локалей (в формате BCP 47, например "en", "ru", "fr-FR").
 * Если указано несколько, при создании Builder случайным образом выбирается одна, если не переопределена методом withLocale().
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestDataLocale {
    /**
     * Массив строковых тегов локалей BCP-47.
     */
    String[] value();
    //das
    //dfdsfsd
}