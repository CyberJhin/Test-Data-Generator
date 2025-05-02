package org.example.DTO;

import lombok.Data;
import org.example.config.InvalidDataConfig;
import org.example.config.InvalidDataType;
import org.example.config.TestDataLocale;
import org.example.config.TestListConfig;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import java.util.List;

@TestDataLocale({"en", "ru"})
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
    @TestListConfig(minItems = 1, maxItems = 6)
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
            invalidDataTypes = {InvalidDataType.TOO_SHORT, InvalidDataType.TOO_LONG}
    )
    private String INN;

    @InvalidDataConfig(
            invalidDataTypes = {}
    )
    private Passport passport;


    @InvalidDataConfig(
            invalidDataTypes = {}
    )
    private Test testObject;
}