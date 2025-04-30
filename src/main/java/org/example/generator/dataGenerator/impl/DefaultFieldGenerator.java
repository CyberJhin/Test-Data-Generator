package org.example.generator.dataGenerator.impl;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

public class DefaultFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        return true; // fallback
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        Class<?> type = field.getType();
        if (String.class.equals(type)) {
            return faker.lorem().word();
        }
        if (Integer.class.equals(type) || int.class.equals(type)) {
            return faker.number().numberBetween(cfg.minLength(), cfg.maxLength());
        }
        // other types handled elsewhere
        return null;
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return generateValid(field, faker, cfg);
    }
}