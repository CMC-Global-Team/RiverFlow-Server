package com.riverflow.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * Validator to check if password and confirmPassword fields match
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private String passwordFieldName;
    private String confirmPasswordFieldName;

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        this.passwordFieldName = constraintAnnotation.password();
        this.confirmPasswordFieldName = constraintAnnotation.confirmPassword();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        try {
            Object password = getFieldValue(obj, passwordFieldName);
            Object confirmPassword = getFieldValue(obj, confirmPasswordFieldName);

            if (password == null && confirmPassword == null) {
                return true;
            }

            return password != null && password.equals(confirmPassword);

        } catch (Exception e) {
            return false;
        }
    }

    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}

