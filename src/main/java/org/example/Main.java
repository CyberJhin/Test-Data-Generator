package org.example;

import org.example.DTO.Customer;

import org.example.DTO.InvalidDataType;
import org.example.utils.*;

import java.util.Arrays;
import java.util.List;




public class Main {
    public static void main(String[] args) {
        Customer customer = TestDataGenerator.builder(Customer.class)
                // 2) Принудительно ставим локаль для всего объекта
                .withLocale("ru")
                // 3) Для конкретного поля — город во втором адресе сделаем на en
                .setFieldLocale(List.of("addresses", "[1]", "city"), "en")
                // Добавляем пару невалидных полей
                .invalidate(List.of("firstName"), InvalidDataType.TOO_SHORT)
                .invalidateListItemAtIndex(List.of("addresses", "street"), 2, InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS)
                // Фиксируем ровно 3 адреса
                .withFixedListSize(List.of("addresses"), 3)
                .build();

        // Печатаем результат
        System.out.println(TestDataGenerator.toJson(customer));
    }
}