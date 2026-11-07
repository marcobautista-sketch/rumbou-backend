package com.rumbou.backend.entity;

// Las funcionalidades que se cobran o se limitan segun el plan.
// Cada una consume de uno de los 2 contadores de UsoDiario.
public enum Funcionalidad {
    SIMULACRO_TEMA,      // gratuitos: 3 a la semana; PRO: ilimitados
    SIMULACRO_COMPLETO   // gratuitos: 1 al mes; PRO: ilimitados
}