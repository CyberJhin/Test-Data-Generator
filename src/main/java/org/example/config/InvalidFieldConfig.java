package org.example.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class InvalidFieldConfig {
    private final List<String> fieldPath;
    private InvalidDataType invalidType;
}