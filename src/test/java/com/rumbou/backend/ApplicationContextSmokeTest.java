package com.rumbou.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba de humo: levanta la aplicacion completa. Los tests por rebanada no
// detectan errores de configuracion de beans. No hereda de AbstractContainerBaseTest
// (@DataJpaTest) porque es incompatible con @SpringBootTest.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApplicationContextSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    private static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("rumbou_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void laAplicacionLevantaConTodosSusBeans() {
    }

    // Cadena de seguridad real: unica verificacion automatica de que /health es publico y el resto exige token.
    @Test
    void elHealthCheckEsPublicoYRespondeOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.servicio").value("backend"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void elRestoDeLaApiSigueExigiendoToken() throws Exception {
        mockMvc.perform(get("/api/v1/preguntas"))
                .andExpect(status().isUnauthorized());
    }
}
