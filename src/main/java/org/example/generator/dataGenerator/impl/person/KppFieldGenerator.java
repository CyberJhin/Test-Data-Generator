package org.example.generator.dataGenerator.impl.person;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

public class KppFieldGenerator implements FieldGenerator {
    public KppFieldGenerator() {
    }

    @Override
    public boolean supports(Field field) {
        return field.getName().toLowerCase().contains("kpp");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        // КПП: 9 цифр
        return faker.number().digits(9);
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> faker.number().digits(3);
            case TOO_LONG -> faker.number().digits(12);
            default -> "123456789";
        };
    }
}