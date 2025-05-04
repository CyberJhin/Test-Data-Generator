// CoreDataGenerator.java
package org.example.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.example.ConfigurableGenerator;
import org.example.config.*;
import org.example.generator.dataGenerator.impl.DefaultFieldGenerator;
import org.example.generator.dataGenerator.repository.FieldGenerator;


import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

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

    public static class PathPattern {
        private final List<String> segments;

        public PathPattern(List<String> segments) {
            this.segments = new ArrayList<>(segments);
        }

        public boolean matches(List<String> path) {
            if (segments.size() != path.size()) return false;
            for (int i = 0; i < segments.size(); i++) {
                String pat = segments.get(i);
                if (!pat.equals("*") && !pat.equals(path.get(i))) return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return segments.toString();
        }
    }

    public static class Builder<T> {
        private final Class<T> clazz;
        private final List<Map.Entry<PathPattern, InvalidFieldConfig>> invalidPatterns = new ArrayList<>();
        private final List<Map.Entry<PathPattern, Integer>> fixedSizes = new ArrayList<>();
        private final Map<PathPattern, Object> manualValues = new LinkedHashMap<>();
        private final Map<PathPattern, Locale> fieldLocales = new LinkedHashMap<>();
        private Locale dtoLocale;
        private boolean onlyRequired = false;
        private final Set<String> requiredTags = new HashSet<>();
        private boolean useRussianPassport = false;
        private boolean useInnForUl = false;

        public Builder(Class<T> clazz) {
            this.clazz = clazz;
            if (clazz.isAnnotationPresent(TestDataLocale.class)) {
                String[] vals = clazz.getAnnotation(TestDataLocale.class).value();
                this.dtoLocale = Locale.forLanguageTag(vals[new Random().nextInt(vals.length)]);
            }
        }

        public Builder<T> invalidate(List<String> path, InvalidDataType invalidType) {
            invalidPatterns.add(Map.entry(new PathPattern(path), new InvalidFieldConfig(path, invalidType)));
            return this;
        }

        public Builder<T> withFixedListSize(List<String> path, int size) {
            fixedSizes.add(Map.entry(new PathPattern(path), size));
            return this;
        }

        public Builder<T> setValue(List<String> path, Object value) {
            manualValues.put(new PathPattern(path), value);
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

        public Builder<T> setFieldLocale(List<String> path, String localeTag) {
            fieldLocales.put(new PathPattern(path), Locale.forLanguageTag(localeTag));
            return this;
        }

        public Builder<T> withRussianPassport(boolean flag) {
            this.useRussianPassport = flag;
            return this;
        }

        public Builder<T> withInnForUl(boolean flag) {
            this.useInnForUl = flag;
            return this;
        }

        public T build() {
            return buildList(1).get(0);
        }

        public List<T> buildList(int count) {
            GeneratorConfig ctx = new GeneratorConfig()
                    .setUseRussianPassport(useRussianPassport)
                    .setUseInnForUl(useInnForUl);

            List<FieldGenerator> generators = ServiceLoader
                    .load(FieldGenerator.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toList());

            for (FieldGenerator gen : generators) {
                if (gen instanceof ConfigurableGenerator cg) {
                    cg.configure(ctx);
                }
            }

            return Collections.nCopies(count, clazz).stream()
                    .map(c -> generateFilteredData(
                            clazz, onlyRequired, requiredTags,
                            new ArrayList<>(), new HashSet<>(),
                            invalidPatterns, fixedSizes, fieldLocales,
                            dtoLocale, new HashMap<>(), generators, manualValues
                    ))
                    .collect(Collectors.toList());
        }
    }

    private static <T> T generateFilteredData(
            Class<T> clazz, boolean onlyRequired, Set<String> requiredTags,
            List<String> parentPath, Set<List<String>> processedPaths,
            List<Map.Entry<PathPattern, InvalidFieldConfig>> invalidPatterns,
            List<Map.Entry<PathPattern, Integer>> fixedSizes,
            Map<PathPattern, Locale> fieldLocales,
            Locale dtoLocale, Map<Locale, Faker> fakerCache,
            List<FieldGenerator> generators,
            Map<PathPattern, Object> manualValues
    ) {
        try {
            if (!processedPaths.add(new ArrayList<>(parentPath))) return null;
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);

                List<String> path = new ArrayList<>(parentPath);
                path.add(field.getName());

                // 1. Установка ручного значения
                Optional<Map.Entry<PathPattern, Object>> manual = manualValues.entrySet().stream()
                        .filter(e -> e.getKey().matches(path)).findFirst();
                if (manual.isPresent()) {
                    field.set(instance, manual.get().getValue());
                    continue;
                }

                InvalidDataConfig cfg = field.getAnnotation(InvalidDataConfig.class);
                TestListConfig listCfg = field.getAnnotation(TestListConfig.class);
                if (cfg == null || (onlyRequired && !cfg.required()) ||
                        (!requiredTags.isEmpty() && Collections.disjoint(Arrays.asList(cfg.tags()), requiredTags)))
                    continue;

                // 2. Определяем локаль
                Locale fieldLocale = fieldLocales.entrySet().stream()
                        .filter(e -> e.getKey().matches(path))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(dtoLocale);
                Faker faker = fakerCache.computeIfAbsent(fieldLocale, Faker::new);

                // 3. Генерация данных
                Class<?> type = field.getType();
                InvalidFieldConfig invCfg = invalidPatterns.stream()
                        .filter(e -> e.getKey().matches(path))
                        .map(Map.Entry::getValue)
                        .findFirst().orElse(null);
                boolean isInvalid = invCfg != null;
                InvalidDataType invType = isInvalid ? invCfg.getInvalidType() : null;

                if (List.class.equals(type)) {
                    int size = fixedSizes.stream()
                            .filter(e -> e.getKey().matches(path))
                            .map(Map.Entry::getValue)
                            .findFirst().orElse(-1);
                    Object listObj = generateListField(field, cfg, listCfg, path, size,
                            invalidPatterns, fieldLocales, dtoLocale, fakerCache, generators, manualValues);
                    field.set(instance, listObj);
                } else if (isCustomDtoType(type)) {
                    Object nested = generateFilteredData(type, onlyRequired, requiredTags,
                            path, processedPaths, invalidPatterns, fixedSizes, fieldLocales,
                            fieldLocale, fakerCache, generators, manualValues);
                    field.set(instance, nested);
                } else {
                    FieldGenerator gen = generators.stream()
                            .filter(g -> g.supports(field)).findFirst()
                            .orElse(new DefaultFieldGenerator());
                    Object val = isInvalid ? gen.generateInvalid(field, cfg, invType, faker)
                            : gen.generateValid(field, faker, cfg);
                    field.set(instance, val);
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> generateListField(
            Field field, InvalidDataConfig cfg, TestListConfig listCfg,
            List<String> path, int fixedSize,
            List<Map.Entry<PathPattern, InvalidFieldConfig>> invalidPatterns,
            Map<PathPattern, Locale> fieldLocales,
            Locale dtoLocale, Map<Locale, Faker> fakerCache,
            List<FieldGenerator> generators,
            Map<PathPattern, Object> manualValues
    ) {
        List<Object> list = new ArrayList<>();
        Faker baseFaker = fakerCache.computeIfAbsent(dtoLocale, Faker::new);
        int size = fixedSize >= 0 ? fixedSize :
                baseFaker.number().numberBetween(listCfg.minItems(), listCfg.maxItems());
        Class<?> elemType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

        for (int i = 0; i < size; i++) {
            List<String> idxPath = new ArrayList<>(path);
            idxPath.add("[" + i + "]");
            Object elem;
            if (isCustomDtoType(elemType)) {
                elem = generateFilteredData(elemType, false, Collections.emptySet(),
                        idxPath, new HashSet<>(), invalidPatterns, Collections.emptyList(),
                        fieldLocales, dtoLocale, fakerCache, generators, manualValues);
            } else {
                InvalidFieldConfig inv = invalidPatterns.stream()
                        .filter(e -> e.getKey().matches(idxPath))
                        .map(Map.Entry::getValue)
                        .findFirst().orElse(null);
                boolean invFlag = inv != null;
                InvalidDataType invType = invFlag ? inv.getInvalidType() : null;

                Locale elemLocale = fieldLocales.entrySet().stream()
                        .filter(e -> e.getKey().matches(idxPath))
                        .map(Map.Entry::getValue)
                        .findFirst().orElse(dtoLocale);
                Faker faker = fakerCache.computeIfAbsent(elemLocale, Faker::new);
                FieldGenerator gen = generators.stream().filter(g -> g.supports(field)).findFirst().orElse(new DefaultFieldGenerator());
                elem = invFlag ? gen.generateInvalid(field, cfg, invType, faker) : gen.generateValid(field, faker, cfg);
            }
            list.add(elem);
        }
        return list;
    }

    private static boolean isCustomDtoType(Class<?> type) {
        return type.getPackageName().startsWith("org.example.DTO");
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
