package com.rumbou.backend.config;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// Servicio temporal, solo para comprobar que @Async funciona.
// Se elimina cuando ya tengamos un caso real de uso (ej. RespuestaIncorrectaEvent).
@Service
public class AsyncDemoService {

    @Async
    public void printFromAnotherThread() {
        System.out.println("Hilo asincrono (@Async): " + Thread.currentThread().getName());
    }
}
