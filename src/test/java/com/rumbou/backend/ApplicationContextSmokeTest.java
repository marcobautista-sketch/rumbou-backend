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

// Prueba de humo: levanta la aplicacion COMPLETA, igual que al ejecutarla de
// verdad. Los demas tests usan rebanadas (@WebMvcTest, @DataJpaTest) o mocks,
// asi que ninguno detecta errores de configuracion de beans: el CI puede estar
// en verde con una aplicacion que no arranca. Ya nos paso una vez con un
// listener mal anotado, y por eso existe este test.
//
// No hereda de AbstractContainerBaseTest porque esa clase es @DataJpaTest,
// incompatible con @SpringBootTest: necesita su propio contenedor.
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
        // Si el contexto de Spring no puede construirse, el test falla solo.
    }

    // Estos dos casos usan la cadena de seguridad REAL (SecurityConfig, no un
    // slice con los filtros apagados): son la unica verificacion automatica de
    // que /health es publico y de que el resto de la API sigue cerrada.
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
