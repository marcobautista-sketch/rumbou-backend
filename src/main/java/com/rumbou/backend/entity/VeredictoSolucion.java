package com.rumbou.backend.entity;

// Resultado de revisar la foto del procedimiento escrito a mano.
public enum VeredictoSolucion {
    CORRECTA,    // razonamiento valido que llega a la clave
    PARCIAL,     // buen planteamiento con un error de calculo, o sin terminar
    INCORRECTA,  // planteamiento o resultado equivocados
    ILEGIBLE     // la foto no se lee o no es una solucion de esta pregunta
}
