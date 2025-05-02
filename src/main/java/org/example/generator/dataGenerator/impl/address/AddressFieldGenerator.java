package org.example.generator.dataGenerator.impl.address;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

/**
 * Генератор поля street (Address.street), поддерживает вложенные и standalone поля.
 */
public class AddressFieldGenerator implements FieldGenerator {
    public AddressFieldGenerator() {
    }

    @Override
    public boolean supports(Field field) {
        String fname = field.getName().toLowerCase();
        String cls = field.getDeclaringClass().getSimpleName().toLowerCase();
        return (fname.equals("street") && (cls.contains("address") || fname.contains("address")));
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        // Локализованная улица
        return faker.address().streetAddress();
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        switch (type) {
            case TOO_SHORT:
                // Короче минимальной длины
                int min = Math.max(1, cfg.minLength() - 1);
                return faker.lorem().characters(min);
            case TOO_LONG:
                // Длиннее максимальной длины
                int max = cfg.maxLength() + 1;
                return faker.lorem().characters(max);
            case CONTAINS_FORBIDDEN_CHARACTERS:
                String base = faker.address().streetName();
                String forbidden = cfg.forbiddenCharacters();
                char ch = (forbidden != null && !forbidden.isEmpty())
                        ? forbidden.charAt(0)
                        : '!';
                return base + ch;
            default:
                // По умолчанию возвращаем валидное, чтобы не ломать логику
                return generateValid(field, faker, cfg);
        }
    }
}


