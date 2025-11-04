package com.riverflow.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom annotation to validate that password and confirmPassword match
 * Applied at class level
 */
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {
    String message() default "Mật khẩu và xác nhận mật khẩu không khớp";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    String password();
    String confirmPassword();
}

