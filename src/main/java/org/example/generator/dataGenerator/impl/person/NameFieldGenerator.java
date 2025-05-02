package org.example.generator.dataGenerator.impl.person;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import javax.validation.constraints.Pattern;
import java.lang.reflect.Field;

public class NameFieldGenerator implements FieldGenerator {
    public NameFieldGenerator() {
    }

    @Override
    public boolean supports(Field field) {
        String name = field.getName().toLowerCase();
        return name.contains("firstname") || name.contains("lastname") || field.isAnnotationPresent(Pattern.class);
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        if (field.getName().toLowerCase().contains("firstname")) {
            return faker.name().firstName();
        }
        if (field.getName().toLowerCase().contains("lastname")) {
            return faker.name().lastName();
        }
        // fallback to pattern or lorem
        return faker.lorem().characters(cfg.minLength(), cfg.maxLength());
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> faker.lorem().characters(1, cfg.minLength() - 1);
            case TOO_LONG -> faker.lorem().characters(cfg.maxLength() + 1, cfg.maxLength() + 10);
            default -> "!!invalid!!";
        };
    }
}
