package org.example.generator.dataGenerator.repository;

import com.github.javafaker.Faker;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;

import java.lang.reflect.Field;

/**
 * Strategy interface for field value generation.
 */
public interface FieldGenerator {
    /**
     * @return {@code true} if this generator supports the given field.
     */
    boolean supports(Field field);

    /**
     * Generate a valid value for the field.
     */
    Object generateValid(Field field, Faker faker, InvalidDataConfig cfg);

    /**
     * Generate an invalid value for the field with specified invalid type.
     */
    Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker);
}