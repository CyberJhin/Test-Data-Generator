package org.example.generator.dataGenerator.impl.address;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

/**
 * Генератор для поля region внутри City или standalone поля region.
 */
public class CityFieldGenerator implements FieldGenerator {
    public CityFieldGenerator() {
    }

    @Override
    public boolean supports(Field field) {
        // Поле называется region
        String fname = field.getName().toLowerCase();
        return fname.equals("city");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        // Используем address().state(), которое учитывает locale
        return faker.address().state();
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        switch (type) {
            case TOO_SHORT:
                // короче минимальной длины
                int min = Math.max(1, cfg.minLength() - 1);
                return faker.lorem().characters(min);
            case TOO_LONG:
                // длиннее максимальной длины
                int max = cfg.maxLength() + 1;
                return faker.lorem().characters(max);
            case CONTAINS_FORBIDDEN_CHARACTERS:
                String base = faker.address().state();
                char ch = cfg.forbiddenCharacters().charAt(0);
                return base + ch;
            default:
                return generateValid(field, faker, cfg);
        }
    }
}

