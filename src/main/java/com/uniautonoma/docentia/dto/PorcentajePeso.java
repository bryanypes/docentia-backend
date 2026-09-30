package com.uniautonoma.docentia.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Peso de una fuente: obligatorio, entre 0 y 100 y con máximo dos decimales. */
@NotNull
@DecimalMin("0")
@DecimalMax("100")
@Digits(integer = 3, fraction = 2)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PorcentajePeso {

  String message() default "El peso debe ser un porcentaje entre 0 y 100, con máximo dos decimales";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
