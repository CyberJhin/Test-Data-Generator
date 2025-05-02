package org.example;

import org.example.generator.GeneratorConfig;
import org.example.generator.dataGenerator.repository.FieldGenerator;

/** Генератор, поддерживающий глобальную конфигурацию. */
public interface ConfigurableGenerator extends FieldGenerator {
    /** Вызывается один раз до генерации, чтобы передать глобальные флаги */
    void configure(GeneratorConfig config);
}
