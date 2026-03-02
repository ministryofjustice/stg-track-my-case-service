package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class AddressDtoTest {

    @Test
    void testJsonInclude() throws JsonProcessingException {
        AddressDto a1 = AddressDto.builder()
            .address1("Line 1")
            .address2("Line 2")
            .address3("Line 3")
            .address4("Line 4")
            .postalCode("SW1A 1AA")
            .country("UK")
            .build();
        AddressDto a2 = AddressDto.builder().build();

        assertEquals(
            "{\"address1\":\"Line 1\",\"address2\":\"Line 2\",\"address3\":\"Line 3\",\"address4\":\"Line 4\",\"postalCode\":\"SW1A 1AA\",\"country\":\"UK\"}",
            objectMapper.writeValueAsString(a1)
        );
        assertEquals("{}", objectMapper.writeValueAsString(a2));
    }

    @Test
    void testBuilderAndEquals() {
        AddressDto a1 = AddressDto.builder()
            .address1("Line 1")
            .address2("Line 2")
            .address3("Line 3")
            .address4("Line 4")
            .postalCode("SW1A 1AA")
            .country("UK")
            .build();
        AddressDto a2 = AddressDto.builder()
            .address1("Line 1")
            .address2("Line 2")
            .address3("Line 3")
            .address4("Line 4")
            .postalCode("SW1A 1AA")
            .country("UK")
            .build();

        assertEquals(a1.getAddress1(), a2.getAddress1());
        assertEquals(a1.getAddress2(), a2.getAddress2());
        assertEquals(a1.getAddress3(), a2.getAddress3());
        assertEquals(a1.getAddress4(), a2.getAddress4());
        assertEquals(a1.getPostalCode(), a2.getPostalCode());
        assertEquals(a1.getCountry(), a2.getCountry());

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }
}
