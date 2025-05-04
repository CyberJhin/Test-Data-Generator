package org.example;

import org.example.DTO.Customer;

import org.example.config.InvalidDataType;
import org.example.generator.*;

import java.util.List;




public class Main {
    public static void main(String[] args) {
        List<Customer> customers = CoreDataGenerator.builder(Customer.class)
                .withLocale("ru")
                .invalidate(Path.of("addresses", "*", "street"), InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS)
                .setValue(Path.of("addresses", "[1]", "city", "city"), "Moscow")
                .withFixedListSize(Path.of("addresses"), 2)
                .buildList(5);

        // Печатаем результат
        System.out.println(CoreDataGenerator.toJson(customers));
    }
}