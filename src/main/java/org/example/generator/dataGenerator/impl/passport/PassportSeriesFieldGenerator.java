package org.example.generator.dataGenerator.impl.passport;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

public class PassportSeriesFieldGenerator implements FieldGenerator {
    private final boolean russianFormat;

    public PassportSeriesFieldGenerator(boolean russianFormat) {
        this.russianFormat = russianFormat;
    }

    @Override
    public boolean supports(Field field) {
        String fname = field.getName().toLowerCase();
        String cls = field.getDeclaringClass().getSimpleName().toLowerCase();
        // Подходит, если это nested Passport.series или поле типа passportSeries, russianPassport etc.
        return (fname.contains("series") && (cls.contains("passport") || fname.contains("passport")));
    }


    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return russianFormat ? faker.regexify("\\d{4}") : faker.bothify("??-###");
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> russianFormat ? faker.regexify("\\d{2}") : "1";
            case TOO_LONG -> russianFormat ? faker.regexify("\\d{6}") : "LONGSERIES123";
            case CONTAINS_FORBIDDEN_CHARACTERS -> "ABCD";
            default -> null;
        };
    }
}
