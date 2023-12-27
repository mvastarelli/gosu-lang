/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.testharness;

import java.lang.annotation.*;

/**
 * Annotations which have this annotation will be added to the JUnit test results XML file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Inherited
public @interface IncludeInTestResults {
}