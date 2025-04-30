package org.example.DTO;

import lombok.Data;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;

@Data
public class Address {

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.TOO_LONG},
            minLength = 2,
            maxLength = 10,
            tags = {"basic"}
    )
    private String street;

    @InvalidDataConfig(
            invalidDataTypes = {},
            tags = {"basic"}
    )
    private City city;
}



