import org.example.DTO.Customer;
import org.example.generator.CoreDataGenerator;
import org.example.integration.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestDataExtension.class)
class CustomerTest {

    @Test
    void testWithInvalidPassport(
            @GenerateTestData(
                    value = Customer.class,
                    useRussianPassport = true,
                    locale = "ru",
                    useInnForUl = true,
                    invalidate = {
                    @FieldInvalidation(path = {"addresses", "[1]", "city", "city"}, type = "TOO_SHORT"),
                    @FieldInvalidation(path = {"passport", "number"}, type = "TOO_SHORT")
            }
            ) Customer customer
    )
    {
        System.out.println(CoreDataGenerator.toJson(customer));
    }
}