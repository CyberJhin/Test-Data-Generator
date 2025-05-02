package org.example.generator.dataGenerator.impl.other;
import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

public class DaysCountFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        String name = field.getName().toLowerCase();
        return (Integer.class.equals(field.getType()) || int.class.equals(field.getType())) && name.contains("day");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return faker.number().numberBetween(cfg.minLength(), cfg.maxLength());
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> faker.number().numberBetween(Integer.MIN_VALUE, cfg.minLength() - 1);
            case TOO_LONG -> faker.number().numberBetween(cfg.maxLength() + 1, Integer.MAX_VALUE);
            default -> -1;
        };
    }
}
