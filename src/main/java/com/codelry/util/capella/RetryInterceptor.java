package com.codelry.util.capella;

import net.bytebuddy.implementation.bind.annotation.*;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class RetryInterceptor {

  @RuntimeType
  public Object intercept(@Origin Method method,
                          @SuperCall Callable<?> superCall) throws Throwable {

    Retryable r = method.getAnnotation(Retryable.class);
    if (r == null) {
      return superCall.call();
    }

    Backoff backoff = r.backoff();
    long delay = backoff.delay();
    Throwable lastError = null;

    for (int attempt = 1; attempt <= r.maxAttempts(); attempt++) {
      try {
        return superCall.call();
      } catch (Throwable t) {
        if (!r.retryOn().isInstance(t)) {
          throw t;
        }
        lastError = t;
        System.out.printf("Attempt %d/%d failed for %s: %s%n",
            attempt, r.maxAttempts(), method.getName(), t.getMessage());
        if (attempt < r.maxAttempts()) {
          Thread.sleep(delay);
          delay = (long) (delay * backoff.multiplier());
        }
      }
    }
    throw lastError;
  }
}
