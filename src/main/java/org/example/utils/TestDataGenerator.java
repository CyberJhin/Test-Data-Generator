package org.example.utils;

import com.github.javafaker.Faker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.DTO.InvalidDataConfig;
import org.example.DTO.InvalidDataType;
import org.example.DTO.InvalidFieldConfig;
import org.example.DTO.TestListConfig;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Генератор тестовых данных с поддержкой локалей.
 */
public class TestDataGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Аннотация для указания дефолтных локалей на уровне DTO-класса.
     * Можно задать одну или несколько (BCP47: "en", "ru", "fr-FR").
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TestDataLocale {
        String[] value();
    }

    public static <T> Builder<T> builder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public static class Builder<T> {
        private final Class<T> clazz;
        private final List<InvalidFieldConfig> invalidConfigs = new ArrayList<>();
        private final Map<List<String>, Integer> fixedListSizes = new HashMap<>();
        private final Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs = new HashMap<>();
        private Locale dtoLocale;
        private final Map<List<String>, Locale> fieldLocales = new HashMap<>();
        private boolean onlyRequired = false;
        private final Set<String> requiredTags = new HashSet<>();

        public Builder(Class<T> clazz) {
            this.clazz = clazz;
            if (clazz.isAnnotationPresent(TestDataLocale.class)) {
                String[] vals = clazz.getAnnotation(TestDataLocale.class).value();
                String pick = vals[new Random().nextInt(vals.length)];
                this.dtoLocale = Locale.forLanguageTag(pick);
            }
        }

        public Builder<T> invalidate(List<String> fieldPath, InvalidDataType invalidType) {
            invalidConfigs.add(new InvalidFieldConfig(fieldPath, invalidType));
            return this;
        }

        public Builder<T> invalidateListItemAtIndex(List<String> listFieldPath, int index, InvalidDataType invalidType) {
            listItemInvalidConfigs
                    .computeIfAbsent(new ArrayList<>(listFieldPath), k -> new HashMap<>())
                    .put(index, new InvalidFieldConfig(listFieldPath, invalidType));
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

        /** Устанавливает локаль для всего DTO (overrides @TestDataLocale). */
        public Builder<T> withLocale(String localeTag) {
            this.dtoLocale = Locale.forLanguageTag(localeTag);
            return this;
        }

        /** Устанавливает локаль для конкретного поля или пути. */
        public Builder<T> setFieldLocale(List<String> fieldPath, String localeTag) {
            fieldLocales.put(new ArrayList<>(fieldPath), Locale.forLanguageTag(localeTag));
            return this;
        }

        public T build() {
            // Собираем invalidFieldMap
            Map<List<String>, InvalidFieldConfig> invalidFieldMap = new HashMap<>();
            invalidConfigs.forEach(cfg ->
                    invalidFieldMap.put(new ArrayList<>(cfg.getFieldPath()), cfg)
            );
            listItemInvalidConfigs.forEach((parent, map) ->
                    map.forEach((idx, cfg) -> {
                        List<String> key = new ArrayList<>(parent);
                        key.add("[" + idx + "]");
                        invalidFieldMap.put(key, cfg);
                    })
            );

            return generateFilteredData(
                    clazz,
                    onlyRequired, requiredTags,
                    new ArrayList<>(), new HashSet<>(),
                    invalidFieldMap, fixedListSizes, listItemInvalidConfigs,
                    dtoLocale, fieldLocales,
                    new HashMap<>()
            );
        }
    }

    private static <T> T generateFilteredData(
            Class<T> clazz,
            boolean onlyRequired,
            Set<String> requiredTags,
            List<String> parentPath,
            Set<List<String>> processedPaths,
            Map<List<String>, InvalidFieldConfig> invalidFieldMap,
            Map<List<String>, Integer> fixedListSizes,
            Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs,
            Locale dtoLocale,
            Map<List<String>, Locale> fieldLocales,
            Map<Locale, Faker> fakerCache
    ) {
        try {
            if (!processedPaths.add(new ArrayList<>(parentPath))) {
                return null;
            }
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);

                InvalidDataConfig cfg = field.getAnnotation(InvalidDataConfig.class);
                TestListConfig listCfg = field.getAnnotation(TestListConfig.class);
                if (cfg == null
                        || (onlyRequired && !cfg.required())
                        || (!requiredTags.isEmpty() && Collections.disjoint(Arrays.asList(cfg.tags()), requiredTags))
                ) continue;

                List<String> path = new ArrayList<>(parentPath);
                path.add(field.getName());
                Locale fieldLocale = fieldLocales.getOrDefault(path, dtoLocale);
                Faker faker = fakerCache.computeIfAbsent(fieldLocale, Faker::new);

                Object value = generateFieldValue(
                        field, cfg, listCfg,
                        invalidFieldMap, onlyRequired, requiredTags,
                        path, processedPaths,
                        fixedListSizes, listItemInvalidConfigs,
                        faker, dtoLocale, fieldLocales, fakerCache
                );
                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object generateFieldValue(
            Field field,
            InvalidDataConfig cfg,
            TestListConfig listCfg,
            Map<List<String>, InvalidFieldConfig> invalidFieldMap,
            boolean onlyRequired,
            Set<String> requiredTags,
            List<String> fieldPath,
            Set<List<String>> processedPaths,
            Map<List<String>, Integer> fixedListSizes,
            Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs,
            Faker faker,
            Locale dtoLocale,
            Map<List<String>, Locale> fieldLocales,
            Map<Locale, Faker> fakerCache
    ) {
        InvalidFieldConfig invCfg = invalidFieldMap.get(fieldPath);
        boolean isInvalid = invCfg != null;
        InvalidDataType invType = isInvalid ? invCfg.getInvalidType() : null;
        Class<?> type = field.getType();

        if (List.class.equals(type)) {
            return generateListField(
                    field, cfg, listCfg,
                    invalidFieldMap, fieldPath,
                    fixedListSizes, listItemInvalidConfigs,
                    faker, dtoLocale, fieldLocales, fakerCache
            );
        } else if (isCustomDtoType(type)) {
            return generateFilteredData(
                    type,
                    onlyRequired, requiredTags,
                    fieldPath, processedPaths,
                    invalidFieldMap, fixedListSizes, listItemInvalidConfigs,
                    fieldLocales.getOrDefault(fieldPath, dtoLocale),
                    fieldLocales, fakerCache
            );
        } else {
            return generatePrimitiveFieldValue(field, cfg, isInvalid, invType, faker);
        }
    }

    private static List<Object> generateListField(
            Field field,
            InvalidDataConfig cfg,
            TestListConfig listCfg,
            Map<List<String>, InvalidFieldConfig> invalidFieldMap,
            List<String> parentPath,
            Map<List<String>, Integer> fixedListSizes,
            Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs,
            Faker faker,
            Locale dtoLocale,
            Map<List<String>, Locale> fieldLocales,
            Map<Locale, Faker> fakerCache
    ) {
        List<Object> list = new ArrayList<>();
        try {
            Class<?> elemType = (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType())
                    .getActualTypeArguments()[0];
            int size = fixedListSizes.getOrDefault(
                    parentPath,
                    faker.number().numberBetween(listCfg.minItems(), listCfg.maxItems())
            );
            Map<Integer, InvalidFieldConfig> invItems = listItemInvalidConfigs
                    .getOrDefault(parentPath, Collections.emptyMap());

            for (int i = 0; i < size; i++) {
                List<String> idxPath = new ArrayList<>(parentPath);
                idxPath.add("[" + i + "]");
                boolean itemInv = invItems.containsKey(i);
                InvalidDataType itemType = itemInv ? invItems.get(i).getInvalidType() : null;
                Locale elemLocale = fieldLocales.getOrDefault(idxPath, dtoLocale);
                Faker elemFaker = fakerCache.computeIfAbsent(elemLocale, Faker::new);

                if (itemInv && elemType.equals(String.class)) {
                    list.add(generateInvalidString(cfg, itemType, elemFaker));
                } else if (itemInv && elemType.equals(Integer.class)) {
                    list.add(generateInvalidInteger(cfg, itemType, elemFaker));
                } else if (elemType.equals(String.class)) {
                    list.add(elemFaker.lorem().word());
                } else if (elemType.equals(Integer.class)) {
                    list.add(elemFaker.number().numberBetween(1, 100));
                } else if (isCustomDtoType(elemType)) {
                    list.add(generateFilteredData(
                            elemType,
                            false, Collections.emptySet(),
                            idxPath, new HashSet<>(),
                            invalidFieldMap, fixedListSizes, listItemInvalidConfigs,
                            elemLocale, fieldLocales, fakerCache
                    ));
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object generatePrimitiveFieldValue(
            Field field,
            InvalidDataConfig cfg,
            boolean isInvalid,
            InvalidDataType invType,
            Faker faker
    ) {
        if (isInvalid) {
            switch (field.getType().getSimpleName()) {
                case "String":
                    return generateInvalidString(cfg, invType, faker);
                case "Integer":
                    return generateInvalidInteger(cfg, invType, faker);
                case "BigDecimal":
                    return generateInvalidBigDecimal(cfg, invType, faker);
                case "LocalDate":
                    return generateInvalidDate(cfg, invType);
                default:
                    return null;
            }
        }
        switch (field.getType().getSimpleName()) {
            case "String":
                return generateValidString(cfg, field, faker);
            case "Integer":
                return generateValidInteger(cfg, faker);
            case "BigDecimal":
                return generateValidBigDecimal(cfg, faker);
            case "LocalDate":
                return generateValidDate(cfg, faker);
            default:
                return null;
        }
    }

    // —————— valid / invalid generators with Faker ——————

    private static String generateValidString(InvalidDataConfig cfg, Field field, Faker faker) {
        // 1) Сначала — валидируем под аннотации, как было
        if (field.isAnnotationPresent(Pattern.class)) {
            return faker.bothify(generateFromPattern(field.getAnnotation(Pattern.class).regexp()));
        }
        if (field.isAnnotationPresent(Email.class)) {
            return faker.internet().emailAddress();
        }

        // 2) Динамически смотрим на имя поля и выбираем провайдер
        String name = field.getName();
        switch (name) {
            case "firstName":
                return faker.name().firstName();        // локализованный first name
            case "lastName":
                return faker.name().lastName();         // локализованный last name
            case "street":
                return faker.address().streetAddress(); // локализованный адрес
            case "city":
                return faker.address().city();          // локализованный город
            case "region":
                return faker.address().state();         // локализованная область/штат
            default:
                // 3) fallback на lorem, если ничего более подходящего
                int len = faker.number().numberBetween(cfg.minLength(), cfg.maxLength());
                return faker.lorem().characters(len);
        }
    }

    private static String generateInvalidString(
            InvalidDataConfig cfg,
            InvalidDataType type,
            Faker faker
    ) {
        switch (type) {
            case INVALID_EMAIL:
                return "invalid-email";
            case TOO_SHORT:
                int sh = Math.max(1, cfg.minLength() - 1);
                return faker.lorem().characters(sh);
            case TOO_LONG:
                int lg = cfg.maxLength() + 10;
                return faker.lorem().characters(lg);
            case CONTAINS_FORBIDDEN_CHARACTERS:
                String base = faker.lorem().characters(5);
                return base + cfg.forbiddenCharacters().charAt(0);
            default:
                return "!!!invalid_data!!!";
        }
    }

    private static Integer generateValidInteger(InvalidDataConfig cfg, Faker faker) {
        return faker.number().numberBetween(cfg.minLength(), cfg.maxLength());
    }

    private static Integer generateInvalidInteger(
            InvalidDataConfig cfg,
            InvalidDataType type,
            Faker faker
    ) {
        switch (type) {
            case TOO_SHORT:
                return faker.number().numberBetween(Integer.MIN_VALUE, cfg.minLength() - 1);
            case TOO_LONG:
                return faker.number().numberBetween(cfg.maxLength() + 1, Integer.MAX_VALUE);
            default:
                return faker.number().numberBetween(cfg.minLength(), cfg.maxLength());
        }
    }

    private static BigDecimal generateValidBigDecimal(InvalidDataConfig cfg, Faker faker) {
        return BigDecimal.valueOf(faker.number().randomDouble(2, cfg.minLength(), cfg.maxLength()));
    }

    private static BigDecimal generateInvalidBigDecimal(
            InvalidDataConfig cfg,
            InvalidDataType type,
            Faker faker
    ) {
        switch (type) {
            case TOO_SHORT:
                return BigDecimal.valueOf(faker.number().randomDouble(2, 1, cfg.minLength() - 1));
            case TOO_LONG:
                return BigDecimal.valueOf(faker.number().randomDouble(2, cfg.maxLength() + 1, 10000));
            case CONTAINS_FORBIDDEN_CHARACTERS:
                return new BigDecimal("NaN");
            default:
                return BigDecimal.valueOf(faker.number().randomDouble(2, cfg.minLength(), cfg.maxLength()));
        }
    }

    private static LocalDate generateValidDate(InvalidDataConfig cfg, Faker faker) {
        int off = faker.number().numberBetween(cfg.minYearOffset(), cfg.maxYearOffset());
        return LocalDate.now().plusYears(off);
    }

    private static LocalDate generateInvalidDate(
            InvalidDataConfig cfg,
            InvalidDataType type
    ) {
        switch (type) {
            case TOO_FAR_IN_FUTURE_DATE:
                return LocalDate.now().plusYears(cfg.maxYearOffset() + 10);
            case TOO_OLD_DATE:
                return LocalDate.now().plusYears(cfg.minYearOffset() - 10);
            default:
                return LocalDate.now();
        }
    }

    private static String generateFromPattern(String regex) {
        if (regex.contains("\\\\d")) {
            return new Faker().number().digits(5);
        }
        return new Faker().letterify("?????");
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCustomDtoType(Class<?> type) {
        return type.getPackageName().equals("org.example.DTO");
    }
}
