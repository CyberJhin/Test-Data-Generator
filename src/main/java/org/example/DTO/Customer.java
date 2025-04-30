package org.example.DTO;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

@Data
public class Customer {

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS},
            forbiddenCharacters = "#$%",
            minLength = 3,
            maxLength = 50,
            tags = {"basic", "test1"}
    )
    @Pattern(regexp = "^[A-Za-z]+$")
    private String firstName;

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.INVALID_EMAIL},
            minLength = 5,
            maxLength = 100,
            tags = {"basic", "test2"}
    )
    @Email
    private String email;

    @InvalidDataConfig(
            invalidDataTypes = {}
    )
    @TestListConfig(minItems = 1, maxItems = 2)
    private List<Address> addresses;

    @InvalidDataConfig(
            invalidDataTypes = {}
    )
    private Address addressesNoneList;

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.CONTAINS_FORBIDDEN_CHARACTERS},
            minLength = 5,
            maxLength = 7
    )
    private Integer customerId;

    @InvalidDataConfig(
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.TOO_LONG},
            minLength = 1,
            maxLength = 10000000,
            required = false,
            tags = {"finance"}
    )
    private BigDecimal balance;
}