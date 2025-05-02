# Test Data Generator

## Описание проекта
**Test Data Generator** — это инструмент для генерации тестовых данных. Он поддерживает гибкие стратегии, работу с локалями и настройку форматов данных. Проект идеально подходит для автоматизации тестирования, создания мок-объектов и проверки работы систем с различными форматами данных.

## Возможности
- **Генерация пользовательских данных**:
    - Поддержка локалей (например, `ru`, `en`).
    - Настройка фиксированных размеров списков.
    - Генерация данных с нарушением правил для тестирования на устойчивость.
- **Работа с аннотациями**:
    - Установка локалей на уровне классов через аннотации.
    - Использование аннотации `@InvalidDataConfig` для настройки правил валидации.
    - Настройка списков через аннотацию `@TestListConfig`.
- **Гибкость**:
    - Поддержка различных стратегий генерации данных через интерфейсы и классы.
    - Поддержка работы с пользовательскими DTO.

## Компоненты
1. **CoreDataGenerator**:
    - Центральный класс для настройки и генерации данных.
    - Поддерживает настройку стратегий генерации, локалей и форматов данных.
    - Использует библиотеку `Java Faker` для генерации данных.
2. **Аннотация `@TestDataLocale`**:
    - Позволяет задавать локали на уровне классов.
3. **Аннотация `@InvalidDataConfig`**:
    - Определяет правила валидации, включая минимальную/максимальную длину, запрещенные символы и обязательность поля.
4. **Аннотация `@TestListConfig`**:
    - Настраивает параметры генерации списков, включая их минимальный и максимальный размер.
5. **Генераторы полей**:
    - Например, `PassportNumberFieldGenerator`, `DaysCountFieldGenerator`, `NameFieldGenerator` и другие.

## Как работать с аннотациями
### Пример использования аннотации `@TestDataLocale`:
```java
@TestDataLocale({"ru", "en"})
public class Customer {
    private String firstName;
    private String lastName;
    private List<Address> addresses;
}
```

### Пример использования аннотации `@InvalidDataConfig`:
```java
@InvalidDataConfig(
    invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS},
    forbiddenCharacters = "#$%",
    minLength = 3,
    maxLength = 50
)
private String firstName;
```

## Как использовать генераторы
### Пример кода
```java
Customer customer = CoreDataGenerator.builder(Customer.class)
    .withLocale("ru") // Устанавливаем локаль для объекта
    .setFieldLocale(List.of("addresses", "[1]", "city"), "en") // Локаль для конкретного поля
    .invalidate(List.of("firstName"), InvalidDataType.TOO_SHORT) // Генерация данных с ошибкой
    .withFixedListSize(List.of("addresses"), 3) // Фиксируем размер списка
    .withRussianPassport(true) // Используем российский формат паспорта
    .build();

// Печатаем результат в формате JSON
System.out.println(CoreDataGenerator.toJson(customer));
```

### Пример вывода
```json
{
  "firstName": "И",
  "lastName": "Иванов",
  "addresses": [
    {"city": "Москва", "street": "Ленина"},
    {"city": "London", "street": "Baker"},
    {"city": "Казань", "street": "Мира"}
  ]
}
```

## Как расширять
1. **Добавление генераторов**:
    - Реализуйте интерфейс `FieldGenerator`.
    - Зарегистрируйте новый генератор в `CoreDataGenerator`.
2. **Добавление новых DTO**:
    - Создайте класс DTO.
    - Добавьте аннотацию `@TestDataLocale` для указания локалей (если требуется).
    - Используйте аннотации `@InvalidDataConfig` для настройки правил валидации.

## Установка и настройка
### Зависимости
Добавьте следующие зависимости в ваш `build.gradle`:
```gradle
dependencies {
    implementation 'com.github.javafaker:javafaker:1.0.2'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
    implementation 'javax.validation:validation-api:2.0.1.Final'
}
```

### Сборка
Для сборки проекта используйте:
```bash
./gradlew build
```

## Тестирование
Для запуска тестов:
```bash
./gradlew test
```

## Контакты
Если у вас есть вопросы или предложения, создайте issue в [репозитории](https://github.com/CyberJhin/Test-Data-Generator).

---

**Test Data Generator** — ваш надежный помощник для автоматизации тестирования!
