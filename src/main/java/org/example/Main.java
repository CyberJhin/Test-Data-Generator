package org.example;

import org.example.DTO.Customer;

import org.example.DTO.InvalidDataType;
import org.example.utils.*;

import java.util.Arrays;
import java.util.List;




public class Main {
    public static void main(String[] args) {
        Customer customer = TestDataGenerator.builder(Customer.class)
                .invalidateListItemAtIndex(List.of("addresses", "city", "region"), 0, InvalidDataType.TOO_SHORT) // Первый элемент списка
                .invalidateListItemAtIndex(List.of("addresses", "street"), 2, InvalidDataType.TOO_LONG)  // Третий элемент списка
                .withFixedListSize(List.of("addresses"), 4) // Всего будет 4 адреса
                .invalidate(Arrays.asList("addressesNoneList", "street"), InvalidDataType.TOO_SHORT)  // Указываем невалидное поле
                .invalidate(Arrays.asList("balance"), InvalidDataType.TOO_LONG) // Указываем невалидное поле), InvalidDataType.TOO_SHORT)  // Указываем невалидное поле
                .build();

        // Печатаем результат
        System.out.println(TestDataGenerator.toJson(customer));
    }
}