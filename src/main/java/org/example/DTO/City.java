package org.example.DTO;

import lombok.Data;

@Data
public class City {

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.TOO_LONG},
            minLength = 2,
            maxLength = 100,
            tags = {"basic"}
    )
    private String region;
}
