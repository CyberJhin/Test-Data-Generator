package org.example.generator;


import com.github.javafaker.Faker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.config.InvalidFieldConfig;
import org.example.config.TestListConfig;
import org.example.generator.dataGenerator.impl.*;
import org.example.generator.dataGenerator.repository.FieldGenerator;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Генератор тестовых данных с поддержкой гибких стратегий и локалей,
 * а также опцией выбора российского паспорта или общего формата.
 */
public class CoreDataGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        private boolean useRussianPassport = false;

        public Builder(Class<T> clazz) {
            this.clazz = clazz;
            if (clazz.isAnnotationPresent(TestDataLocale.class)) {
                String[] vals = clazz.getAnnotation(TestDataLocale.class).value();
                this.dtoLocale = Locale.forLanguageTag(vals[new Random().nextInt(vals.length)]);
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

        public Builder<T> withLocale(String localeTag) {
            this.dtoLocale = Locale.forLanguageTag(localeTag);
            return this;
        }

        public Builder<T> setFieldLocale(List<String> fieldPath, String localeTag) {
            fieldLocales.put(new ArrayList<>(fieldPath), Locale.forLanguageTag(localeTag));
            return this;
        }

        public Builder<T> withRussianPassport(boolean flag) {
            this.useRussianPassport = flag;
            return this;
        }

        public T build() {
            Map<List<String>, InvalidFieldConfig> invalidFieldMap = new HashMap<>();
            invalidConfigs.forEach(cfg -> invalidFieldMap.put(new ArrayList<>(cfg.getFieldPath()), cfg));
            listItemInvalidConfigs.forEach((parent, map) ->
                    map.forEach((idx, cfg) -> {
                        List<String> key = new ArrayList<>(parent);
                        key.add("[" + idx + "]");
                        invalidFieldMap.put(key, cfg);
                    })
            );

            List<FieldGenerator> generators = List.of(
                    new NameFieldGenerator(),
                    new PatronymicFieldGenerator(),
                    new EmailFieldGenerator(),
                    new AddressFieldGenerator(),
                    new AmountFieldGenerator(),
                    new DaysCountFieldGenerator(),
                    new InnFieldGenerator(),
                    new KppFieldGenerator(),
                    new PassportSeriesFieldGenerator(useRussianPassport),
                    new PassportNumberFieldGenerator(useRussianPassport),
                    new PassportCodeFieldGenerator(useRussianPassport),
                    new DefaultFieldGenerator()
            );

            return generateFilteredData(
                    clazz,
                    onlyRequired,
                    requiredTags,
                    new ArrayList<>(),
                    new HashSet<>(),
                    invalidFieldMap,
                    fixedListSizes,
                    listItemInvalidConfigs,
                    dtoLocale,
                    fieldLocales,
                    new HashMap<>(),
                    generators
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
            Map<Locale, Faker> fakerCache,
            List<FieldGenerator> generators
    ) {
        try {
            if (!processedPaths.add(new ArrayList<>(parentPath))) return null;
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                InvalidDataConfig cfg = field.getAnnotation(InvalidDataConfig.class);
                TestListConfig listCfg = field.getAnnotation(TestListConfig.class);
                if (cfg == null
                        || (onlyRequired && !cfg.required())
                        || (!requiredTags.isEmpty() && Collections.disjoint(Arrays.asList(cfg.tags()), requiredTags))) continue;

                List<String> path = new ArrayList<>(parentPath);
                path.add(field.getName());
                Locale fieldLocale = fieldLocales.getOrDefault(path, dtoLocale);
                Faker faker = fakerCache.computeIfAbsent(fieldLocale, Faker::new);

                Class<?> type = field.getType();
                InvalidFieldConfig invCfg = invalidFieldMap.get(path);
                boolean isInvalid = invCfg != null;
                InvalidDataType invType = isInvalid ? invCfg.getInvalidType() : null;

                Object value;
                if (List.class.equals(type)) {
                    value = generateListField(
                            field, cfg, listCfg,
                            parentPath,
                            invalidFieldMap,
                            fixedListSizes,
                            listItemInvalidConfigs,
                            dtoLocale,
                            fieldLocales,
                            fakerCache,
                            generators
                    );
                } else if (isCustomDtoType(type)) {
                    value = generateFilteredData(
                            type,
                            onlyRequired,
                            requiredTags,
                            path,
                            processedPaths,
                            invalidFieldMap,
                            fixedListSizes,
                            listItemInvalidConfigs,
                            fieldLocales.getOrDefault(path, dtoLocale),
                            fieldLocales,
                            fakerCache,
                            generators
                    );
                } else {
                    FieldGenerator generator = generators.stream()
                            .filter(g -> g.supports(field))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No generator for field " + field.getName()));
                    value = isInvalid
                            ? generator.generateInvalid(field, cfg, invType, faker)
                            : generator.generateValid(field, faker, cfg);
                }

                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> generateListField(
            Field field,
            InvalidDataConfig cfg,
            TestListConfig listCfg,
            List<String> parentPath,
            Map<List<String>, InvalidFieldConfig> invalidFieldMap,
            Map<List<String>, Integer> fixedListSizes,
            Map<List<String>, Map<Integer, InvalidFieldConfig>> listItemInvalidConfigs,
            Locale dtoLocale,
            Map<List<String>, Locale> fieldLocales,
            Map<Locale, Faker> fakerCache,
            List<FieldGenerator> generators
    ) {
        List<Object> list = new ArrayList<>();
        try {
            Class<?> elemType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            Faker baseFaker = fakerCache.computeIfAbsent(dtoLocale, Faker::new);
            int size = fixedListSizes.getOrDefault(
                    parentPath,
                    baseFaker.number().numberBetween(listCfg.minItems(), listCfg.maxItems())
            );

            Map<Integer, InvalidFieldConfig> invItems = listItemInvalidConfigs.getOrDefault(parentPath, Collections.emptyMap());

            for (int i = 0; i < size; i++) {
                List<String> idxPath = new ArrayList<>(parentPath);
                idxPath.add("[" + i + "]");
                boolean itemInv = invItems.containsKey(i);
                InvalidDataType itemType = itemInv ? invItems.get(i).getInvalidType() : null;
                Locale elemLocale = fieldLocales.getOrDefault(idxPath, dtoLocale);
                Faker elemFaker = fakerCache.computeIfAbsent(elemLocale, Faker::new);

                Object element;
                if (isCustomDtoType(elemType)) {
                    element = generateFilteredData(
                            elemType,
                            false,
                            Collections.emptySet(),
                            idxPath,
                            new HashSet<>(),
                            invalidFieldMap,
                            fixedListSizes,
                            listItemInvalidConfigs,
                            elemLocale,
                            fieldLocales,
                            fakerCache,
                            generators
                    );
                } else {
                    FieldGenerator generator = generators.stream()
                            .filter(g -> g.supports(field))
                            .findFirst()
                            .orElse(new DefaultFieldGenerator());
                    element = itemInv
                            ? generator.generateInvalid(field, cfg, itemType, elemFaker)
                            : generator.generateValid(field, elemFaker, cfg);
                }
                list.add(element);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }
}

