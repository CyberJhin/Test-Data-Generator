package org.example.generator.dataGenerator.impl;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;
import java.math.BigDecimal;

public class AmountFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        String name = field.getName().toLowerCase();
        return BigDecimal.class.equals(field.getType()) && (name.contains("amount") || name.contains("sum") || name.contains("suma"));
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        double value = faker.number().randomDouble(2, cfg.minLength(), (long)cfg.maxLength());
        return BigDecimal.valueOf(value);
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> BigDecimal.valueOf(faker.number().randomDouble(2, 0, Math.max(0, cfg.minLength() - 1)));
            case TOO_LONG ->
                    BigDecimal.valueOf(faker.number().randomDouble(2, cfg.maxLength() + 1, cfg.maxLength() + 1000));
            default -> BigDecimal.ZERO;
        };
    }
}
