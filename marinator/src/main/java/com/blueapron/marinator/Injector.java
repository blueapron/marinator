package com.blueapron.marinator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation specifying that this method injects a particular class.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Injector {

    /**
     * Whether or not to use strict injection for this class. Strict injection require an
     * injector registered for every class. This is the safest mode because it guarantees
     * injection is only performed by the correct class. Occasionally, though, it is useful to allow
     * "loose" injection where the injector can be inferred from the superclass. This is only safe
     * if there's no potential ambiguity here, which is why this is opt-in.
     */
    boolean strict() default true;
}

