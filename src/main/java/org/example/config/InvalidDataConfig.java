package org.example.config;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InvalidDataConfig {
    InvalidDataType[] invalidDataTypes();
    String forbiddenCharacters() default "";
    int minLength() default 0;
    int maxLength() default Integer.MAX_VALUE;
    int minYearOffset() default -100;
    int maxYearOffset() default 100;
    boolean required() default true;       // Поле обязательно по дефолту
    String[] tags() default {};             // Теги для поля
}

