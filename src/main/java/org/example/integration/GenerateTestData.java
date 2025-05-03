package org.example.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Аннотация для генерации данных для тестов
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface GenerateTestData {

    Class<?> value();  // Класс, данные для которого генерируются

    String locale() default "";  // Локаль для генерации данных

    boolean onlyRequired() default false;  // Генерация только обязательных полей

    boolean useRussianPassport() default false;  // Использовать российский паспорт

    boolean useInnForUl() default false;  // Использовать ИНН для юридических лиц

    // Массив для настройки невалидных данных
    FieldInvalidation[] invalidate() default {};

    FixedListSize[] fixedListSizes() default {};  // Размеры фиксированных списков

    FieldLocale[] fieldLocales() default {};  // Локализация полей
}