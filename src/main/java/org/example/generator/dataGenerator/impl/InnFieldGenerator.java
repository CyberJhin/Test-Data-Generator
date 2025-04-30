package org.example.generator.dataGenerator.impl;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

public class InnFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        return field.getName().toLowerCase().contains("inn");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        // Российский ИНН: 10 или 12 цифр
        int length = faker.bool().bool() ? 10 : 12;
        return faker.number().digits(length);
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> faker.number().digits(5);
            case TOO_LONG -> faker.number().digits(15);
            default -> "0000000000";
        };
    }
}