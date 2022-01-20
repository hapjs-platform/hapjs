/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hapjs.bridge.Extension;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ActionAnnotation {
    String name();

    boolean instanceMethod() default false;

    Extension.Mode mode();

    Extension.Type type() default Extension.Type.FUNCTION;

    Extension.Access access() default Extension.Access.NONE;

    Extension.Normalize normalize() default Extension.Normalize.JSON;

    Extension.Multiple multiple() default Extension.Multiple.SINGLE;

    String alias() default "";

    String[] permissions() default {};

    String[] subAttrs() default {};

    ActionAnnotation.ResidentType residentType() default ActionAnnotation.ResidentType.NONE;

    enum ResidentType {
        NONE,
        USEABLE
    }
}
