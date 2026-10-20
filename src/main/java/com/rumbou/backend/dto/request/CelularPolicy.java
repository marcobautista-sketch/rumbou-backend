package com.rumbou.backend.dto.request;

// Celular peruano: 9 digitos y empieza con 9.
public final class CelularPolicy {

    public static final String REGEX = "^9\\d{8}$";
    public static final String MENSAJE = "El celular debe tener 9 digitos y empezar con 9";

    private CelularPolicy() {
    }
}
