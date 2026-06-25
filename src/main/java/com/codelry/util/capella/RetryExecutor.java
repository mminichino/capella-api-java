package com.codelry.util.capella;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public final class RetryExecutor {

  private RetryExecutor() {}

  public static <T> T execute(Method method, Callable<T> action) throws Exception {
    Retryable r = method.getAnnotation(Retryable.class);
    if (r == null) {
      return action.call();
    }

    Backoff backoff = r.backoff();
    long delay = backoff.delay();
    Exception lastError = null;

    for (int attempt = 1; attempt <= r.maxAttempts(); attempt++) {
      try {
        return action.call();
      } catch (Exception e) {
        if (!shouldRetry(r, e)) {
          throw e;
        }
        lastError = e;
        System.out.printf("Attempt %d/%d failed for %s: %s%n",
            attempt, r.maxAttempts(), method.getName(), e.getMessage());
        if (attempt < r.maxAttempts()) {
          Thread.sleep(delay);
          delay = (long) (delay * backoff.multiplier());
        }
      }
    }
    throw lastError;
  }

  public static void executeVoid(Method method, Runnable action) throws Exception {
    execute(method, () -> {
      action.run();
      return null;
    });
  }

  private static boolean shouldRetry(Retryable r, Throwable t) {
    Class<? extends Throwable>[] allowable = r.allowableExceptions();
    if (allowable.length == 0) {
      return true;
    }
    for (Class<? extends Throwable> type : allowable) {
      if (type.isInstance(t)) {
        return true;
      }
    }
    return false;
  }
}
