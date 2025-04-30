package org.example.DTO;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Конфигурация генерации списка в тестовых данных.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestListConfig {
    int minItems() default 1;
    int maxItems() default 5;

    int deafultMinItems() default 1;

    int deafultMaxItems() default 5;
}
