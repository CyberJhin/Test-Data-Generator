package org.example.utils;

import com.github.javafaker.Faker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.DTO.InvalidDataConfig;
import org.example.DTO.InvalidDataType;
import org.example.DTO.InvalidFieldConfig;
import org.example.DTO.TestListConfig;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TestDataGenerator {

    private static final Faker faker = new Faker();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> Builder<T> builder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public static class Builder<T> {
        private final Class<T> clazz;
        private final List<InvalidFieldConfig> invalidConfigs = new ArrayList<>();
        private final Map<List<String>, Integer> fixedListSizes = new HashMap<>();
        private final Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs = new HashMap<>();
        private boolean onlyRequired = false;
        private Set<String> requiredTags = new HashSet<>();

        private Builder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Builder<T> invalidate(List<String> fieldPath, InvalidDataType invalidType) {
            invalidConfigs.add(new InvalidFieldConfig(fieldPath, invalidType));
            return this;
        }

        public Builder<T> invalidateListItemAtIndex(List<String> listFieldPath, int index, InvalidDataType invalidType) {
            System.out.println("Adding invalidation for list field path: " + listFieldPath + " at index: " + index + " with type: " + invalidType);
            listItemInvalidConfigs.computeIfAbsent(new ArrayList<>(listFieldPath), k -> new HashMap<>())
                    .put(index, new InvalidFieldConfig(listFieldPath, invalidType));
            System.out.println("Updated listItemInvalidConfigs: " + listItemInvalidConfigs);
            return this;
        }

        public Builder<T> withFixedListSize(List<String> fieldPath, int size) {
            fixedListSizes.put(new ArrayList<>(fieldPath), size);
            return this;
        }

        public Builder<T> onlyRequired() {
            this.onlyRequired = true;
            return this;
        }

        public Builder<T> withTag(String tag) {
            this.requiredTags.add(tag);
            return this;
        }

        public T build() {
            Map<List<String>, InvalidFieldConfig> invalidFieldMap = new HashMap<>();

            // Заполняем invalidFieldMap из invalidConfigs
            invalidConfigs.forEach(cfg -> invalidFieldMap.put(new ArrayList<>(cfg.getFieldPath()), cfg));
            System.out.println("Initial invalidFieldMap from invalidConfigs: " + invalidFieldMap);

            // Добавляем данные из listItemInvalidConfigs
            listItemInvalidConfigs.forEach((parentPath, invalidItems) -> {
                invalidItems.forEach((index, config) -> {
                    // Генерируем уникальный путь с учетом индекса
                    List<String> pathWithIndex = new ArrayList<>(parentPath);
                    pathWithIndex.add(0,"[" + index + "]"); // Добавляем индекс как часть ключа

                    // Храним значение с индексом для точного сопоставления
                    invalidFieldMap.put(pathWithIndex, config);
                });
            });

            // Логируем состояние после добавления listItemInvalidConfigs
            System.out.println("Final invalidFieldMap after adding listItemInvalidConfigs: " + invalidFieldMap);

            // Вызываем generateFilteredData с обновленным invalidFieldMap
            return generateFilteredData(clazz, invalidConfigs, onlyRequired, requiredTags, new ArrayList<>(), new HashSet<>(), invalidFieldMap, fixedListSizes, listItemInvalidConfigs);
        }
    }

    private static <T> T generateFilteredData(Class<T> clazz,
                                              List<InvalidFieldConfig> invalidConfigs,
                                              boolean onlyRequired,
                                              Set<String> requiredTags,
                                              List<String> parentPath,
                                              Set<List<String>> processedPaths,
                                              Map<List<String>, InvalidFieldConfig> invalidFieldMap,
                                              Map<List<String>, Integer> fixedListSizes,
                                              Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs) {
        try {
            if (processedPaths.contains(parentPath)) {
                return null;
            }
            processedPaths.add(new ArrayList<>(parentPath));

            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                InvalidDataConfig config = field.getAnnotation(InvalidDataConfig.class);
                TestListConfig listConfig = field.getAnnotation(TestListConfig.class);
                System.out.println("CHECKING CONFIG -------:" + invalidFieldMap);
                if (config == null || (onlyRequired && !config.required()) ||
                        (!requiredTags.isEmpty() && Collections.disjoint(Arrays.asList(config.tags()), requiredTags))) {
                    continue;
                }

                List<String> fieldPath = new ArrayList<>(parentPath);
                fieldPath.add(field.getName());

                Object value = generateFieldValue(field, config, listConfig, invalidFieldMap, onlyRequired, requiredTags, fieldPath, processedPaths, fixedListSizes, listItemInvalidConfigs);
                field.set(instance, value);
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object generateFieldValue(Field field, InvalidDataConfig config, TestListConfig listConfig,
                                             Map<List<String>, InvalidFieldConfig> invalidFieldMap,
                                             boolean onlyRequired, Set<String> requiredTags,
                                             List<String> parentPath, Set<List<String>> processedPaths,
                                             Map<List<String>, Integer> fixedListSizes,
                                             Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs) {
        // Логируем начальную информацию
        System.out.println("generateFieldValue called for field: " + field.getName());
        System.out.println("Parent path: " + parentPath);

        // Формируем полный путь
        List<String> fullPath = new ArrayList<>(parentPath);
        boolean isInvalidField = invalidFieldMap.containsKey(fullPath);

        // Логируем состояние
        InvalidDataType invalidType = isInvalidField ? invalidFieldMap.get(fullPath).getInvalidType() : null;
        System.out.println("Full path: " + fullPath);
        System.out.println("Is invalid field: " + isInvalidField);
        System.out.println("Invalid type: " + (invalidType != null ? invalidType : "null"));

        // Логируем тип данных
        Class<?> type = field.getType();
        System.out.println("Field type: " + type);

        // Логика осталась неизменной
        if (List.class.equals(type)) {
            return generateListField(field, config, listConfig, onlyRequired, requiredTags, invalidFieldMap, fullPath, fixedListSizes, listItemInvalidConfigs);
        } else if (isCustomDtoType(type)) {
            return generateFilteredData(type, Collections.emptyList(), onlyRequired, requiredTags, fullPath, processedPaths, invalidFieldMap, fixedListSizes, listItemInvalidConfigs);
        } else {
            return generatePrimitiveFieldValue(field, config, isInvalidField, invalidType);
        }
    }
    private static Object generatePrimitiveFieldValue(Field field, InvalidDataConfig config, boolean isInvalidField, InvalidDataType invalidType) {
        System.out.println("generatePrimitiveFieldValue called for field: " + field.getName() + ", isInvalidField: " + isInvalidField + ", invalidType: " + invalidType);
        if (isInvalidField) {
            switch (field.getType().getSimpleName()) {
                case "String":
                    System.out.println("Calling generateInvalidString for field: " + field.getName());
                    String invalidString = generateInvalidString(config, invalidType);
                    System.out.println("Generated invalid string for field: " + field.getName() + " -> " + invalidString);
                    return invalidString;
                case "Integer":
                    System.out.println("Calling generateInvalidInteger for field: " + field.getName());
                    Integer invalidInteger = generateInvalidInteger(config, invalidType);
                    System.out.println("Generated invalid integer for field: " + field.getName() + " -> " + invalidInteger);
                    return invalidInteger;
                case "BigDecimal":
                    System.out.println("Calling generateInvalidBigDecimal for field: " + field.getName());
                    BigDecimal invalidBigDecimal = generateInvalidBigDecimal(config, invalidType);
                    System.out.println("Generated invalid BigDecimal for field: " + field.getName() + " -> " + invalidBigDecimal);
                    return invalidBigDecimal;
                case "LocalDate":
                    System.out.println("Calling generateInvalidDate for field: " + field.getName());
                    LocalDate invalidDate = generateInvalidDate(config, invalidType);
                    System.out.println("Generated invalid LocalDate for field: " + field.getName() + " -> " + invalidDate);
                    return invalidDate;
                default:
                    System.out.println("Unsupported type for invalid value generation: " + field.getType().getSimpleName());
                    return null;
            }
        }

        // Генерация валидных данных
        switch (field.getType().getSimpleName()) {
            case "String":
                System.out.println("Calling generateValidString for field: " + field.getName());
                String validString = generateValidString(config, field);
                System.out.println("Generated valid string for field: " + field.getName() + " -> " + validString);
                return validString;
            case "Integer":
                System.out.println("Calling generateValidInteger for field: " + field.getName());
                Integer validInteger = generateValidInteger(config);
                System.out.println("Generated valid integer for field: " + field.getName() + " -> " + validInteger);
                return validInteger;
            case "BigDecimal":
                System.out.println("Calling generateValidBigDecimal for field: " + field.getName());
                BigDecimal validBigDecimal = generateValidBigDecimal(config);
                System.out.println("Generated valid BigDecimal for field: " + field.getName() + " -> " + validBigDecimal);
                return validBigDecimal;
            case "LocalDate":
                System.out.println("Calling generateValidDate for field: " + field.getName());
                LocalDate validDate = generateValidDate(config);
                System.out.println("Generated valid LocalDate for field: " + field.getName() + " -> " + validDate);
                return validDate;
            default:
                System.out.println("Unsupported type for valid value generation: " + field.getType().getSimpleName());
                return null;
        }
    }

    private static List<Object> generateListField(Field field, InvalidDataConfig config, TestListConfig listConfig,
                                                  boolean onlyRequired, Set<String> requiredTags,
                                                  Map<List<String>, InvalidFieldConfig> invalidFieldMap,
                                                  List<String> parentPath, Map<List<String>, Integer> fixedListSizes,
                                                  Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs) {
        System.out.println("Generating list for field: " + field.getName() + " at path: " + parentPath);
        System.out.println("listItemInvalidConfigs received: " + listItemInvalidConfigs);

        List<Object> list = new ArrayList<>();
        try {
            Class<?> genericType = (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType())
                    .getActualTypeArguments()[0];

            int numberOfItems = fixedListSizes.getOrDefault(parentPath, faker.number().numberBetween(
                    listConfig != null ? listConfig.minItems() : listConfig.deafultMinItems(),
                    listConfig != null ? listConfig.maxItems() : listConfig.deafultMaxItems()
            ));

            System.out.println("Number of items to generate: " + numberOfItems);
            System.out.println("listItemInvalidConfigs items for field: " + listItemInvalidConfigs);

            // Поиск соответствующих ключей для вложенных путей
            Map<Integer, InvalidFieldConfig> invalidItemsForField = listItemInvalidConfigs.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().size() > parentPath.size() && entry.getKey().subList(0, parentPath.size()).equals(parentPath))
                    .map(Map.Entry::getValue)
                    .flatMap(map -> map.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            System.out.println("Invalid items for field: " + invalidItemsForField);

            for (int i = 0; i < numberOfItems; i++) {
                // Создаем путь с индексом для поиска в invalidFieldMap
                List<String> pathWithIndex = new ArrayList<>(parentPath);
                pathWithIndex.add(0,"[" + i + "]");

                System.out.println("LOOH Path with index: " + pathWithIndex);

                System.out.println("Generating item at index: " + i + " invalidItemsForField.containsKey(i) " + invalidItemsForField.containsKey(i));
                System.out.println("invalidItemsForField: " + invalidItemsForField);
                if (invalidItemsForField.containsKey(i)) {
                    InvalidDataType invalidType = invalidItemsForField.get(i).getInvalidType();
                    System.out.println("Invalid data requested for index " + i + ", type: " + invalidType + " genericType " + genericType + " " + genericType.equals(String.class) + " " + isCustomDtoType(genericType));
                    System.out.println(listItemInvalidConfigs + " " + invalidFieldMap);
                    if (genericType.equals(String.class)) {
                        String invalidValue = generateInvalidString(config, invalidType);
                        System.out.println("Generated invalid string for index " + i + ": " + invalidValue);
                        list.add(invalidValue);
                    } else if (genericType.equals(Integer.class)) {
                        Integer invalidValue = generateInvalidInteger(config, invalidType);
                        System.out.println("Generated invalid integer for index " + i + ": " + invalidValue);
                        list.add(invalidValue);
                    } else if (isCustomDtoType(genericType)) {
                        Object invalidDto = generateFilteredData(genericType, List.of(new InvalidFieldConfig(pathWithIndex, invalidType)),
                                onlyRequired, requiredTags, pathWithIndex, new HashSet<>(), invalidFieldMap, fixedListSizes, listItemInvalidConfigs);
                        System.out.println("Generated invalid DTO for index " + i + ": " + invalidDto);
                        list.add(invalidDto);
                    }
                } else {
                    if (genericType.equals(String.class)) {
                        String validValue = faker.lorem().word();
                        System.out.println("Generated valid string for index " + i + ": " + validValue);
                        list.add(validValue);
                    } else if (genericType.equals(Integer.class)) {
                        Integer validValue = faker.number().numberBetween(1, 100);
                        System.out.println("Generated valid integer for index " + i + ": " + validValue);
                        list.add(validValue);
                    } else if (isCustomDtoType(genericType)) {
                        Object validDto = generateFilteredData(genericType, Collections.emptyList(), onlyRequired, requiredTags,
                                parentPath, new HashSet<>(), invalidFieldMap, fixedListSizes, listItemInvalidConfigs);
                        System.out.println("Generated valid DTO for index " + i + ": " + validDto);
                        list.add(validDto);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error generating list field: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate list field", e);
        }
        return list;
    }

    private static boolean isCustomDtoType(Class<?> type) {
        return type.getPackageName().equals("org.example.DTO");
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    private static String generateValidString(InvalidDataConfig config, Field field) {
        if (field.isAnnotationPresent(Pattern.class)) {
            return faker.bothify(generateFromPattern(field.getAnnotation(Pattern.class).regexp()));
        } else if (field.isAnnotationPresent(Email.class)) {
            return faker.internet().emailAddress();
        }
        int length = faker.number().numberBetween(config.minLength(), config.maxLength());
        return faker.lorem().characters(length);
    }

    private static String generateInvalidString(InvalidDataConfig config, InvalidDataType invalidType) {
        System.out.println("Generating invalid string with config: " + config + " and invalidType: " + invalidType);
        switch (invalidType) {
            case INVALID_EMAIL:
                return "invalid-email";
            case TOO_SHORT:
                int tooShortLength = Math.max(1, config.minLength() - 1);
                String tooShortString = faker.lorem().characters(tooShortLength);
                System.out.println("Generated TOO_SHORT string: " + tooShortString + " with length: " + tooShortLength);
                return tooShortString;
            case TOO_LONG:
                int tooLongLength = config.maxLength() + 10;
                String tooLongString = faker.lorem().characters(tooLongLength);
                System.out.println("Generated TOO_LONG string: " + tooLongString + " with length: " + tooLongLength);
                return tooLongString;
            case CONTAINS_FORBIDDEN_CHARACTERS:
                String forbiddenString = faker.lorem().characters(5) + config.forbiddenCharacters().charAt(0);
                System.out.println("Generated string with forbidden characters: " + forbiddenString);
                return forbiddenString;
            default:
                System.out.println("Default invalid string generated: !!!invalid_data!!!");
                return "!!!invalid_data!!!";
        }
    }

    private static Integer generateValidInteger(InvalidDataConfig config) {
        return faker.number().numberBetween(config.minLength(), config.maxLength());
    }

    private static Integer generateInvalidInteger(InvalidDataConfig config, InvalidDataType invalidType) {
        return switch (invalidType) {
            case TOO_SHORT -> faker.number().numberBetween(Integer.MIN_VALUE, config.minLength() - 1);
            case TOO_LONG -> faker.number().numberBetween(config.maxLength() + 1, Integer.MAX_VALUE);
            default -> faker.number().numberBetween(config.minLength(), config.maxLength());
        };
    }

    private static BigDecimal generateValidBigDecimal(InvalidDataConfig config) {
        return BigDecimal.valueOf(faker.number().randomDouble(2, config.minLength(), config.maxLength()));
    }

    private static BigDecimal generateInvalidBigDecimal(InvalidDataConfig config, InvalidDataType invalidType) {
        return switch (invalidType) {
            case TOO_SHORT -> BigDecimal.valueOf(faker.number().randomDouble(2, 1, config.minLength() - 1));
            case TOO_LONG -> BigDecimal.valueOf(faker.number().randomDouble(2, config.maxLength() + 1, 10000));
            case CONTAINS_FORBIDDEN_CHARACTERS -> new BigDecimal("NaN");
            default -> BigDecimal.valueOf(faker.number().randomDouble(2, config.minLength(), config.maxLength()));
        };
    }

    private static String generateFromPattern(String regex) {
        if (regex.contains("\\d")) {
            return faker.number().digits(5);
        }
        return faker.letterify("?????");
    }

    private static LocalDate generateValidDate(InvalidDataConfig config) {
        int yearOffset = faker.number().numberBetween(config.minYearOffset(), config.maxYearOffset());
        return LocalDate.now().plusYears(yearOffset);
    }

    private static LocalDate generateInvalidDate(InvalidDataConfig config, InvalidDataType invalidType) {
        return switch (invalidType) {
            case TOO_FAR_IN_FUTURE_DATE -> LocalDate.now().plusYears(config.maxYearOffset() + 10);
            case TOO_OLD_DATE -> LocalDate.now().plusYears(config.minYearOffset() - 10);
            default -> LocalDate.now();
        };
    }
}