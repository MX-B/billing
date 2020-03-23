package io.gr1d.billing.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates if a Provider exists
 *
 * @author Sérgio Marcelino
 *
 */
@Documented
@Constraint(validatedBy = ProviderExistsValidator.class)
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface ProviderExists {

	String message() default "{io.gr1d.validation.ProviderExists.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
