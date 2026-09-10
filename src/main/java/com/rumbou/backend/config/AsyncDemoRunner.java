package com.rumbou.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Clase temporal, solo para comprobar que @Async funciona al arrancar la app.
// Se elimina cuando ya tengamos un caso real de uso (ej. RespuestaIncorrectaEvent).
@Component
public class AsyncDemoRunner implements CommandLineRunner {

    private final AsyncDemoService asyncDemoService;

    public AsyncDemoRunner(AsyncDemoService asyncDemoService) {
        this.asyncDemoService = asyncDemoService;
    }

    @Override
    public void run(String... args) {
        System.out.println("Hilo principal (main): " + Thread.currentThread().getName());
        asyncDemoService.printFromAnotherThread();
    }
}
