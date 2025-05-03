package org.example.integration;

import org.example.DTO.Customer;
import org.example.config.InvalidDataType;
import org.example.generator.CoreDataGenerator;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Parameter;
import java.util.List;

public class TestDataExtension implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Parameter parameter = parameterContext.getParameter();
        return parameter.isAnnotationPresent(GenerateTestData.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Parameter parameter = parameterContext.getParameter();
        GenerateTestData annotation = parameter.getAnnotation(GenerateTestData.class);

        // Создание билдера
        CoreDataGenerator.Builder<?> builder = CoreDataGenerator.builder(annotation.value());

        // Применение параметров из аннотации
        if (!annotation.locale().isEmpty()) builder.withLocale(annotation.locale());
        if (annotation.onlyRequired()) builder.onlyRequired();
        if (annotation.useRussianPassport()) builder.withRussianPassport(true);
        if (annotation.useInnForUl()) builder.withInnForUl(true);

        for (FieldInvalidation inv : annotation.invalidate()) {
            builder.invalidate(List.of(inv.path()), InvalidDataType.valueOf(inv.type()));
        }

        for (FixedListSize size : annotation.fixedListSizes()) {
            builder.withFixedListSize(List.of(size.path()), size.size());
        }

        for (FieldLocale locale : annotation.fieldLocales()) {
            builder.setFieldLocale(List.of(locale.path()), locale.locale());
        }

        return builder.build();
    }
}
