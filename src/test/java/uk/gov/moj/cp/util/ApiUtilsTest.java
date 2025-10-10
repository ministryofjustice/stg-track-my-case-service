package uk.gov.moj.cp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiUtilsTest {

    @Test
    void testConstants() {
        assertEquals("Basic ", ApiUtils.BASIC_TOKEN_PREFIX);
        assertEquals("Bearer ", ApiUtils.BEARER_TOKEN_PREFIX);
    }
}
