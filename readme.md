# Test Data Generator

## Описание проекта
**Test Data Generator** — это инструмент для генерации тестовых данных. Он поддерживает гибкие стратегии, работу с локалями, а также позволяет настраивать форматы и условия генерации данных. Проект идеально подходит для автоматизации тестирования, создания мок-объектов и проверки систем на устойчивость к невалидным данным.

## Возможности
- **Генерация пользовательских данных**:
    - Поддержка локалей (`ru`, `en`, и др.).
    - Фиксированный размер списков.
    - Генерация валидных и невалидных данных по заданным правилам.
- **Работа с аннотациями**:
    - Задание локалей на уровне класса.
    - Настройка правил валидации через `@InvalidDataConfig`.
    - Управление размерами коллекций с помощью `@TestListConfig`.
- **Расширяемость**:
    - Подключение собственных генераторов через SPI.
    - Передача глобальных конфигураций в генераторы.

---

## Компоненты

### 1. CoreDataGenerator
- Центральный класс для генерации тестовых данных.
- Использует `Java Faker` для генерации строк, чисел, имен и пр.
- Поддерживает тонкую настройку: локали, размеры списков, правила валидации.

### 2. Аннотации

#### `@TestDataLocale`
Позволяет задать локаль генерации на уровне DTO:
```java
@TestDataLocale("ru")
public class Customer { ... }
```

#### `@InvalidDataConfig`
Определяет ограничения, при нарушении которых можно сгенерировать невалидные данные:
```java
@InvalidDataConfig(
  invalidDataTypes = {InvalidDataType.TOO_SHORT},
  forbiddenCharacters = "!@#",
  minLength = 3
)
private String firstName;
```

#### `@TestListConfig`
Позволяет управлять размерами списков при генерации:
```java
@TestListConfig(minSize = 1, maxSize = 3)
private List<Address> addresses;
```

---

## Пример использования

```java
Customer customer = CoreDataGenerator.builder(Customer.class)
    .withLocale("ru")
    .setFieldLocale(List.of("addresses", "[1]", "city"), "en")
    .invalidate(List.of("firstName"), InvalidDataType.TOO_SHORT)
    .invalidate(List.of("addresses", "*", "street"), InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS)
    .withFixedListSize(List.of("addresses"), 2)
    .withRussianPassport(true)
    .withInnForUl(false)
    .build();

System.out.println(CoreDataGenerator.toJson(customer));
```

---

## Как добавить собственный генератор

1. **Создайте класс, реализующий `FieldGenerator`:**

```java
public class CustomFieldGenerator implements FieldGenerator {
    @Override
    public boolean supports(Field field) {
        return field.getName().equalsIgnoreCase("customField");
    }

    @Override
    public Object generateValid(Field field, Faker faker, InvalidDataConfig cfg) {
        return "VALID_VALUE";
    }

    @Override
    public Object generateInvalid(Field field, InvalidDataConfig cfg, InvalidDataType type, Faker faker) {
        return "INVALID";
    }
}
```

2. **(Опционально) Реализуйте интерфейс `ConfigurableGenerator`, если требуется доступ к глобальной конфигурации:**

```java
public class CustomFieldGenerator implements FieldGenerator, ConfigurableGenerator {
    private boolean myFlag;

    @Override
    public void configure(GeneratorConfig config) {
        this.myFlag = config.isUseSomethingSpecial();
    }

    // остальная реализация...
}
```
Затем добавьте вызов реализованных конфигураторов, он автоматически проставит значения и соберет все генераторы:
```java 
       public T build() {
  // 1) Собираем глобальный конфиг
  GeneratorConfig ctx = new GeneratorConfig()
          .setUseRussianPassport(useRussianPassport)
          .setUseInnForUl(useInnForUl);

  // 2) Загружаем все FieldGenerator через SPI
  List<FieldGenerator> generators = ServiceLoader
          .load(FieldGenerator.class)
          .stream()
          .map(ServiceLoader.Provider::get)
          .collect(Collectors.toList());

  // 3) Конфигурируем тех, кому нужно
  for (FieldGenerator gen : generators) {
    if (gen instanceof ConfigurableGenerator cg) {
      cg.configure(ctx);
    }
  }

  // 4) Вызываем основной метод генерации, передав список generators
  return generateFilteredData(
          clazz, onlyRequired, requiredTags,
          new ArrayList<>(), new HashSet<>(),
          invalidPatterns, fixedSizes, fieldLocales,
          dtoLocale, new HashMap<>(), generators
  );
}
```

3. **Зарегистрируйте генератор через SPI:**

Создайте файл:
```
src/main/resources/META-INF/services/org.example.generator.dataGenerator.repository.FieldGenerator
```

Добавьте в него FQCN вашего генератора:
```
org.example.generator.dataGenerator.impl.CustomFieldGenerator
```

> ⚠️ Важно: каждый класс должен иметь **публичный конструктор без аргументов**.

---

## Установка и сборка

### Зависимости (Gradle)

```gradle
dependencies {
    implementation 'com.github.javafaker:javafaker:1.0.2'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
    implementation 'javax.validation:validation-api:2.0.1.Final'
}
```

### Сборка проекта

```bash
./gradlew build
```

---

## Тестирование

```bash
./gradlew test
```

---

## Контакты

Вопросы, предложения и баги — через [issues](https://github.com/CyberJhin/Test-Data-Generator).

---

**Test Data Generator** — ваш гибкий помощник в генерации тестовых данных.
