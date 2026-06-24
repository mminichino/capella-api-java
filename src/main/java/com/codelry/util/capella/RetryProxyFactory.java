package com.codelry.util.capella;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.implementation.MethodDelegation;

public final class RetryProxyFactory {

  @SuppressWarnings("unchecked")
  public static <T> T wrap(Class<T> targetClass) throws Exception {
    Class<? extends T> proxyClass = new ByteBuddy()
        .subclass(targetClass)
        .method(ElementMatchers.isAnnotatedWith(Retryable.class))
        .intercept(MethodDelegation.to(new RetryInterceptor()))
        .make()
        .load(targetClass.getClassLoader())
        .getLoaded();

    return proxyClass.getDeclaredConstructor().newInstance();
  }
}
