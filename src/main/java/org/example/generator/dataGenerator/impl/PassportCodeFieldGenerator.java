package org.example.generator.dataGenerator.impl;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

public class PassportCodeFieldGenerator implements FieldGenerator {
    private final boolean russianFormat;

    public PassportCodeFieldGenerator(boolean russianFormat) {
        this.russianFormat = russianFormat;
    }

    @Override
    public boolean supports(Field field) {
        String fname = field.getName().toLowerCase();
        String cls = field.getDeclaringClass().getSimpleName().toLowerCase();
        return (fname.contains("code") && (cls.contains("passport") || fname.contains("passport")));
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return russianFormat ? faker.regexify("\\d{3}-\\d{3}") : faker.bothify("??-??-###");
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> "123";
            case TOO_LONG -> "123-456-789-000";
            case CONTAINS_FORBIDDEN_CHARACTERS -> "@@@-###";
            default -> null;
        };
    }
}
