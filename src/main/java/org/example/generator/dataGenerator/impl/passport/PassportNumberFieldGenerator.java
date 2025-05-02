package org.example.generator.dataGenerator.impl.passport;

import com.github.javafaker.Faker;
import org.example.ConfigurableGenerator;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.GeneratorConfig;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;

public class PassportNumberFieldGenerator implements FieldGenerator, ConfigurableGenerator {
    private  boolean russianFormat;

    public PassportNumberFieldGenerator( ) {

    }
    @Override
    public void configure(GeneratorConfig config) {
        this.russianFormat = config.isUseRussianPassport();
    }

    @Override
    public boolean supports(Field field) {
        String fname = field.getName().toLowerCase();
        String cls = field.getDeclaringClass().getSimpleName().toLowerCase();
        return (fname.contains("number") && (cls.contains("passport") || fname.contains("passport")));
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return russianFormat ? faker.regexify("\\d{6}") : faker.bothify("########");
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> russianFormat ? faker.regexify("\\d{2}") : "123";
            case TOO_LONG -> russianFormat ? faker.regexify("\\d{10}") : "123456789012";
            case CONTAINS_FORBIDDEN_CHARACTERS -> "NUMB!@#";
            default -> null;
        };
    }
}
