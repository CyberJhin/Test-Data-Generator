package org.example;

import org.example.DTO.Customer;

import org.example.config.InvalidDataType;
import org.example.generator.*;

import java.util.List;




public class Main {
    public static void main(String[] args) {
        Customer customer = CoreDataGenerator.builder(Customer.class)
                // 1) Принудительно ставим локаль для всего объекта
                .withLocale("ru")
                // 2) Для всех элементов списка addresses — для поля city ставим en
                .setFieldLocale(List.of("addresses", "[1]", "city"), "en")
                // 3) Делаем firstName явно слишком коротким
                .invalidate(List.of("firstName"), InvalidDataType.TOO_SHORT)
                // 4) Для любого адреса делаем поле street с forbidden chars
                .invalidate(List.of("addresses", "*", "street"), InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS)
                // 5) Фиксируем ровно 10 элементов в списке addresses
                .withFixedListSize(List.of("addresses"), 2)
                // 6) Генерировать паспорт не по российскому формату
                .withRussianPassport(false)
                // 7) Для поля INN (standalone) делаем too_short
                .invalidate(List.of("inn"), InvalidDataType.TOO_SHORT)
                .build();

        // Печатаем результат
        System.out.println(CoreDataGenerator.toJson(customer));
    }
}