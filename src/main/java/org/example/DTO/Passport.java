package org.example.DTO;

import lombok.Data;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;

@Data
public class Passport {

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.TOO_LONG},
            minLength = 1,
            maxLength = 10000000,
            tags = {"finance"}
    )
    private String series;

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.TOO_LONG},
            minLength = 1,
            maxLength = 10000000
    )
    private String number;
}
