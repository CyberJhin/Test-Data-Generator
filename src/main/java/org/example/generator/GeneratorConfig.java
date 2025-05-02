package org.example.generator;

import lombok.Getter;

/** Централизованные флаги для генераторов. */
@Getter
public class GeneratorConfig {
    private boolean useRussianPassport;
    private boolean useInnForUl;

    public GeneratorConfig setUseRussianPassport(boolean f) { this.useRussianPassport = f; return this; }

    public GeneratorConfig setUseInnForUl(boolean f) { this.useInnForUl = f; return this; }
}
