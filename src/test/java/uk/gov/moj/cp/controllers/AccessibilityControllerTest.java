package uk.gov.moj.cp.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessibilityControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AccessibilityController accessibilityController = new AccessibilityController();
        mockMvc = MockMvcBuilders.standaloneSetup(accessibilityController).build();
    }

    @Test
    @DisplayName("GET /api/cases/active-user")
    void testGetActiveUser() throws Exception {
        mockMvc.perform(get("/api/cases/active-user"))
            .andExpect(status().isOk());
    }
}
