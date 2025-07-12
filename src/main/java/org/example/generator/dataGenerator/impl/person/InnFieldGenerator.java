package org.example.generator.dataGenerator.impl.person;

import com.github.javafaker.Faker;
import org.example.ConfigurableGenerator;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.generator.GeneratorConfig;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.reflect.Field;
import java.util.Random;

public class InnFieldGenerator implements FieldGenerator, ConfigurableGenerator {

    private static final int[] COEF_10 = {2,4,10,3,5,9,4,6,8};
    private static final int[] COEF_11_1 = {7,2,4,10,3,5,9,4,6,8};       // для первой контрольной цифры 12-значного ИНН
    private static final int[] COEF_11_2 = {3,7,2,4,10,3,5,9,4,6,8};    // для второй контрольной цифры 12-значного ИНН

    private static final Random random = new Random();

    /**
     * If {@code true}, generate INN for legal entities (10 digits);
     * otherwise generate individual INN (12 digits).
     */
    private boolean innForUl;

    public InnFieldGenerator() {

    }
    @Override
    public boolean supports(Field field) {
        return field.getName().toLowerCase().contains("inn");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return innForUl ? generateInn10() : generateInn12();
    }

    public static String generateInn10() {
        int[] digits = new int[10];
        // Генерируем первые 9 цифр случайно
        for (int i = 0; i < 9; i++) {
            digits[i] = random.nextInt(10);
        }
        // Вычисляем контрольную цифру
        digits[9] = calculateChecksum(digits, COEF_10);
        // Формируем строку ИНН
        StringBuilder inn = new StringBuilder();
        for (int d : digits) {
            inn.append(d);
        }
        return inn.toString();
    }

    // Метод генерации 12-значного ИНН
    public static String generateInn12() {
        int[] digits = new int[12];
        // Генерируем первые 10 цифр случайно
        for (int i = 0; i < 10; i++) {
            digits[i] = random.nextInt(10);
        }
        // Вычисляем первую контрольную цифру (11-я)
        digits[10] = calculateChecksum(digits, COEF_11_1);
        // Вычисляем вторую контрольную цифру (12-я)
        digits[11] = calculateChecksum(digits, COEF_11_2);
        // Формируем строку ИНН
        StringBuilder inn = new StringBuilder();
        for (int d : digits) {
            inn.append(d);
        }
        return inn.toString();
    }

    // Метод вычисления контрольной цифры с учетом правил
    private static int calculateChecksum(int[] digits, int[] coef) {
        int sum = 0;
        for (int i = 0; i < coef.length; i++) {
            sum += digits[i] * coef[i];
        }
        int remainder = sum % 11;
        if (remainder == 10) {
            remainder = 0;
        }
        return remainder;
    }


    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return switch (type) {
            case TOO_SHORT -> faker.number().digits(5);
            case TOO_LONG -> faker.number().digits(15);
            default -> "0000000000";
        };
    }

    @Override
    public void configure(GeneratorConfig config) {
        this.innForUl = config.isUseInnForUl();
    }
}