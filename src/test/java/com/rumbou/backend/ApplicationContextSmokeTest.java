package com.rumbou.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

// Prueba de humo: levanta la aplicacion COMPLETA, igual que al ejecutarla de
// verdad. Los demas tests usan rebanadas (@WebMvcTest, @DataJpaTest) o mocks,
// asi que ninguno detecta errores de configuracion de beans: el CI puede estar
// en verde con una aplicacion que no arranca. Ya nos paso una vez con un
// listener mal anotado, y por eso existe este test.
//
// No hereda de AbstractContainerBaseTest porque esa clase es @DataJpaTest,
// incompatible con @SpringBootTest: necesita su propio contenedor.
@SpringBootTest
@Testcontainers
class ApplicationContextSmokeTest {

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
}
