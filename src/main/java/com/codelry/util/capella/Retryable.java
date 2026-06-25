package com.codelry.util.capella;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Retryable {
  int maxAttempts() default 3;
  Backoff backoff() default @Backoff();
  Class<? extends Throwable>[] allowableExceptions() default {};
}
