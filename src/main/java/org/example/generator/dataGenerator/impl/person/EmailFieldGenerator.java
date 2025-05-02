package org.example.generator.dataGenerator.impl.person;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import javax.validation.constraints.Email;
import java.lang.reflect.Field;

public class EmailFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Email.class) || field.getName().toLowerCase().contains("email");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return faker.internet().emailAddress();
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        if (type == InvalidDataType.INVALID_EMAIL) {
            return "invalid-email@";
        }
        // fallback
        return "bad@domain";
    }
}