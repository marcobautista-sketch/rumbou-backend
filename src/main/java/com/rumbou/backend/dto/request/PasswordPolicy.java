package com.rumbou.backend.dto.request;

// Regla unica de contrasena para registro y reseteo: 8 a 72 caracteres (72 es el
// limite de BCrypt) con al menos una minuscula, una mayuscula y un numero.
public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";
    public static final String MENSAJE =
            "La contrasena debe tener entre 8 y 72 caracteres, con al menos una mayuscula, una minuscula y un numero";

    private PasswordPolicy() {
    }
}
