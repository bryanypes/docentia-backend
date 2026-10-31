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

/** Nota de un aspecto: obligatoria, entre 1.0 y 5.0 y con un solo decimal. */
@NotNull
@DecimalMin("1.0")
@DecimalMax("5.0")
@Digits(integer = 1, fraction = 1)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotaAspecto {

  String message() default "La nota debe estar entre 1.0 y 5.0, con máximo un decimal";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
