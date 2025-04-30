package org.example.generator.dataGenerator.impl;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;
import java.util.Objects;

public class AddressFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        String name = field.getName().toLowerCase();
        return name.equals("street") || name.equals("city") || name.equals("region");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return switch (field.getName().toLowerCase()) {
            case "street" -> faker.address().streetAddress();
            case "city" -> faker.address().city();
            case "region" -> faker.address().state();
            default -> "";
        };
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        if (Objects.requireNonNull(type) == InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS) {
            String base = faker.address().streetName();
            return base + cfg.forbiddenCharacters().charAt(0);
        }
        return generateValid(field, faker, cfg);
    }
}

