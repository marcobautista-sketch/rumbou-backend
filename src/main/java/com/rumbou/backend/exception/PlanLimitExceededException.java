package com.rumbou.backend.exception;

// Un 403 especifico: la accion existe pero el plan del usuario no la permite o ya agoto su cupo.
public class PlanLimitExceededException extends ForbiddenException {

    public PlanLimitExceededException(String message) {
        super(message);
    }
}
