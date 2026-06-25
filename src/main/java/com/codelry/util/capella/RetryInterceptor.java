package com.codelry.util.capella;

import net.bytebuddy.implementation.bind.annotation.*;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class RetryInterceptor {

  @RuntimeType
  public Object intercept(@Origin Method method,
                          @SuperCall Callable<?> superCall) throws Throwable {
    return RetryExecutor.execute(method, () -> superCall.call());
  }
}
