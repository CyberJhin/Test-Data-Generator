package org.example.DTO;

import lombok.Data;

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



