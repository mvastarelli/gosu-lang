/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.testharness;

import java.lang.annotation.*;

/**
 * Tells the com.guidewire.testharness.multiapp.VerifyAllResourcesAndPCFFilesTest (in pl-test module) to ignore this type. Used for classes
 * that are intentionally invalid (for tests).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface DoNotVerifyResource {
}
