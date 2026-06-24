package com.codelry.util.capella;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Backoff {
  long delay() default 1000;
  double multiplier() default 1.0;
}
