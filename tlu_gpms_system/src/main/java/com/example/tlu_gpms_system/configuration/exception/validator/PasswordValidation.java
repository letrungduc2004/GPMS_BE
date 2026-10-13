package com.example.tlu_gpms_system.configuration.exception.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Target({ FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = {PasswordConstrain.class})
public @interface PasswordValidation {
    String message() default "Password must be 8 character include characters and numbers";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
