package org.example.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.example.ConfigurableGenerator;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.config.InvalidFieldConfig;
import org.example.config.TestListConfig;
import org.example.generator.dataGenerator.impl.DefaultFieldGenerator;
import org.example.generator.dataGenerator.repository.FieldGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CoreDataGenerator с улучшенной логикой path-selection: поддержка wildcard '*' на любом уровне.
 */
public class CoreDataGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TestDataLocale { String[] value(); }

    public static <T> Builder<T> builder(Class<T> clazz) { return new Builder<>(clazz); }

    /**
     * Паттерн пути с поддержкой '*' как wildcard сегмента.
     */
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
        @Override public String toString() { return segments.toString(); }
    }

    public static class Builder<T> {
        private final Class<T> clazz;
        private final List<Map.Entry<PathPattern, InvalidFieldConfig>> invalidPatterns = new ArrayList<>();
        private final List<Map.Entry<PathPattern, Integer>> fixedSizes = new ArrayList<>();
        private Locale dtoLocale;
        private final Map<PathPattern, Locale> fieldLocales = new LinkedHashMap<>();
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

        public Builder<T> invalidate(List<String> fieldPath, InvalidDataType invalidType) {
            invalidPatterns.add(Map.entry(new PathPattern(fieldPath), new InvalidFieldConfig(fieldPath, invalidType)));
            return this;
        }

        public Builder<T> withFixedListSize(List<String> fieldPath, int size) {
            fixedSizes.add(Map.entry(new PathPattern(fieldPath), size));
            return this;
        }

        public Builder<T> onlyRequired() { this.onlyRequired = true; return this; }
        public Builder<T> withTag(String tag) { this.requiredTags.add(tag); return this; }
        public Builder<T> withLocale(String localeTag) { this.dtoLocale = Locale.forLanguageTag(localeTag); return this; }
        public Builder<T> setFieldLocale(List<String> fieldPath, String localeTag) {
            fieldLocales.put(new PathPattern(fieldPath), Locale.forLanguageTag(localeTag));
            return this;
        }
        public Builder<T> withRussianPassport(boolean flag) { this.useRussianPassport = flag; return this; }
        public Builder<T> withInnForUl(boolean flag) { this.useInnForUl = flag; return this; }

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
    }

    private static <T> T generateFilteredData(
            Class<T> clazz, boolean onlyRequired, Set<String> requiredTags,
            List<String> parentPath, Set<List<String>> processedPaths,
            List<Map.Entry<PathPattern, InvalidFieldConfig>> invalidPatterns,
            List<Map.Entry<PathPattern, Integer>> fixedSizes,
            Map<PathPattern, Locale> fieldLocales,
            Locale dtoLocale, Map<Locale, Faker> fakerCache,
            List<FieldGenerator> generators
    ) {
        try {
            if (!processedPaths.add(new ArrayList<>(parentPath))) return null;
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                InvalidDataConfig cfg = field.getAnnotation(InvalidDataConfig.class);
                TestListConfig listCfg = field.getAnnotation(TestListConfig.class);
                if (cfg == null || (onlyRequired && !cfg.required()) || (!requiredTags.isEmpty() && Collections.disjoint(Arrays.asList(cfg.tags()), requiredTags))) continue;

                List<String> path = new ArrayList<>(parentPath);
                path.add(field.getName());
                // Определяем локаль для поля по первому подходящему шаблону
                Locale fieldLocale = dtoLocale;
                for (var e : fieldLocales.entrySet()) {
                    if (e.getKey().matches(path)) { fieldLocale = e.getValue(); break; }
                }
                Faker faker = fakerCache.computeIfAbsent(fieldLocale, Faker::new);

                Class<?> type = field.getType();
                // Ищем invalidConfig по шаблону
                InvalidFieldConfig invCfg = null;
                for (var entry : invalidPatterns) {
                    if (entry.getKey().matches(path)) { invCfg = entry.getValue(); break; }
                }
                boolean isInvalid = invCfg != null;
                InvalidDataType invType = isInvalid ? invCfg.getInvalidType() : null;

                if (List.class.equals(type)) {
                    // фиксированный размер списка по шаблону
                    int size = -1;
                    for (var e : fixedSizes) {
                        if (e.getKey().matches(path)) { size = e.getValue(); break; }
                    }
                    Object listObj = generateListField(field, cfg, listCfg, path, size,
                            invalidPatterns, fieldLocales, dtoLocale, fakerCache, generators);
                    field.set(instance, listObj);
                } else if (isCustomDtoType(type)) {
                    Object nested = generateFilteredData(type, onlyRequired, requiredTags,
                            path, processedPaths, invalidPatterns, fixedSizes, fieldLocales,
                            fieldLocale, fakerCache, generators);
                    field.set(instance, nested);
                } else {
                    // Примитив
                    FieldGenerator gen = generators.stream().filter(g -> g.supports(field)).findFirst().orElseThrow();
                    Object val = isInvalid ? gen.generateInvalid(field, cfg, invType, faker)
                            : gen.generateValid(field, faker, cfg);
                    field.set(instance, val);
                }
            }
            return instance;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static List<Object> generateListField(
            Field field, InvalidDataConfig cfg, TestListConfig listCfg,
            List<String> path, int fixedSize,
            List<Map.Entry<PathPattern, InvalidFieldConfig>> invalidPatterns,
            Map<PathPattern, Locale> fieldLocales,
            Locale dtoLocale, Map<Locale, Faker> fakerCache,
            List<FieldGenerator> generators
    ) {
        List<Object> list = new ArrayList<>();
        Faker baseFaker = fakerCache.computeIfAbsent(dtoLocale, Faker::new);
        int size = fixedSize >= 0 ? fixedSize : baseFaker.number().numberBetween(listCfg.minItems(), listCfg.maxItems());
        Class<?> elemType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        for (int i = 0; i < size; i++) {
            List<String> idxPath = new ArrayList<>(path);
            idxPath.add("[" + i + "]");
            // рекусивная генерация
            Object elem;
            if (isCustomDtoType(elemType)) {
                elem = generateFilteredData(elemType, false, Collections.emptySet(), idxPath, new HashSet<>(), invalidPatterns, Collections.emptyList(), fieldLocales, dtoLocale, fakerCache, generators);
            } else {
                // invalid для элементов списков поддерживается через шаблон
                InvalidFieldConfig inv = null;
                for (var e : invalidPatterns) if (e.getKey().matches(idxPath)) { inv = e.getValue(); break; }
                boolean invFlag = inv != null;
                InvalidDataType invType = invFlag ? inv.getInvalidType() : null;
                // локаль элемента
                Locale elemLocale = dtoLocale;
                for (var e : fieldLocales.entrySet()) if (e.getKey().matches(idxPath)) { elemLocale = e.getValue(); break; }
                Faker faker = fakerCache.computeIfAbsent(elemLocale, Faker::new);
                FieldGenerator gen = generators.stream().filter(g -> g.supports(field)).findFirst().orElse(new DefaultFieldGenerator());
                elem = invFlag ? gen.generateInvalid(field, cfg, invType, faker) : gen.generateValid(field, faker, cfg);
            }
            list.add(elem);
        }
        return list;
    }

    private static boolean isCustomDtoType(Class<?> type) { return type.getPackageName().startsWith("org.example.DTO"); }

    public static String toJson(Object obj) {
        try { return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
