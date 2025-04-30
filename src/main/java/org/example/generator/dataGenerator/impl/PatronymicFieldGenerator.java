package org.example.generator.dataGenerator.impl;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

/** Генератор отчества (патронимика) */
public class PatronymicFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        String name = field.getName().toLowerCase();
        return name.contains("patronymic") || name.contains("middlename");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return faker.name().nameWithMiddle().split(" ")[1];
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        // Слишком короткое или слишком длинное отчество
        return switch (type) {
            case TOO_SHORT -> faker.lorem().characters(1, Math.max(1, cfg.minLength() - 1));
            case TOO_LONG -> faker.lorem().characters(cfg.maxLength() + 1, cfg.maxLength() + 5);
            default -> "!invalidPat!";
        };
    }
}