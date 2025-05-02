package org.example.DTO;

import lombok.Data;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;

@Data
public class Test {

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.TOO_LONG},
            minLength = 2,
            maxLength = 100,
            tags = {"basic"}
    )
    private String testAtribute;
}
