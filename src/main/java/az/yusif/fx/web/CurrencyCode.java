package az.yusif.fx.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Pattern(regexp = "^[A-Za-z]{3}$")
@Constraint(validatedBy = {})
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrencyCode {

    String message() default "must be a 3-letter currency code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
