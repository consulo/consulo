/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.util.rmi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.rmi.Remote;
import java.rmi.ServerError;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Gregory.Shrago
 */
public class RemoteUtil {
  private RemoteUtil() {
  }

  private static final ConcurrentMap<Map.Entry<Class<?>, Class<?>>, Map<Method, Method>> ourRemoteToLocalMap = new ConcurrentHashMap<Map.Entry<Class<?>, Class<?>>, Map<Method, Method>>();

  @Nonnull
  private static Map<Method, Method> getOrCreateRemoteToLocal(Map.Entry<Class<?>, Class<?>> key) {
    Map<Method, Method> methods = ourRemoteToLocalMap.get(key);
    if (methods != null) {
      return methods;
    }

    final Map<Method, Method> map = new HashMap<Method, Method>();
    for (Method method : key.getValue().getMethods()) {
      Method m = null;
      main:
      for (Method candidate : key.getKey().getMethods()) {
        if (!candidate.getName().equals(method.getName())) continue;
        Class<?>[] cpts = candidate.getParameterTypes();
        Class<?>[] mpts = method.getParameterTypes();
        if (cpts.length != mpts.length) continue;
        for (int i = 0; i < mpts.length; i++) {
          Class<?> mpt = mpts[i];
          Class<?> cpt = cpts[i];
          if (!cpt.isAssignableFrom(mpt)) continue main;
        }
        m = candidate;
        break;
      }
      if (m != null) map.put(method, m);
    }

    ourRemoteToLocalMap.putIfAbsent(key, map);
    return map;
  }

  @Nullable
  public static <T> T castToRemote(final Object object, final Class<T> clazz) {
    if (!Proxy.isProxyClass(object.getClass())) return null;
    final InvocationHandler handler = Proxy.getInvocationHandler(object);
    if (handler instanceof RemoteInvocationHandler) {
      final RemoteInvocationHandler rih = (RemoteInvocationHandler)handler;
      if (clazz.isInstance(rih.myRemote)) {
        return (T)rih.myRemote;
      }
    }
    return null;
  }

  public static <T> T castToLocal(final Object remote, final Class<T> clazz) {
    final ClassLoader loader = clazz.getClassLoader();
    //noinspection unchecked
    return (T)Proxy.newProxyInstance(loader, new Class[]{clazz}, new RemoteInvocationHandler(remote, clazz, loader));
  }

  private static Class<?> tryFixReturnType(Object result, Class<?> returnType, ClassLoader loader) throws Exception {
    if (returnType.isInterface()) return returnType;
    if (result instanceof RemoteCastable) {
      final String className = ((RemoteCastable)result).getCastToClassName();
      return Class.forName(className, true, loader);
    }
    return returnType;
  }

  public static <T> T substituteClassLoader(final T remote, final ClassLoader classLoader) throws Exception {
    return executeWithClassLoader(new Callable<T>() {
      @Override
      public T call() {
        Object proxy = Proxy.newProxyInstance(classLoader, remote.getClass().getInterfaces(), new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
            return executeWithClassLoader(new Callable<Object>() {
              @Override
              public Object call() throws Exception {
                try {
                  return handleRemoteResult(method.invoke(remote, args), method.getReturnType(), classLoader, true);
                }
                catch (InvocationTargetException e) {
                  Throwable cause = e.getCause();
                  if (cause instanceof RuntimeException) throw (RuntimeException)cause;
                  if (cause instanceof Error) throw (Error)cause;
                  if (canThrow(cause, method)) throw (Exception)cause;
                  throw new RuntimeException(cause);
                }
              }
            }, classLoader);
          }
        });
        return (T)proxy;
      }
    }, classLoader);
  }

  public static <T> T handleRemoteResult(Object value, Class<? super T> clazz, Object requestor) throws Exception {
    return handleRemoteResult(value, clazz, requestor.getClass().getClassLoader(), false);
  }

  private static <T> T handleRemoteResult(Object value, Class<?> methodReturnType, ClassLoader classLoader, boolean substituteClassLoader) throws Exception {
    Object result;
    if (value instanceof Remote) {
      if (value instanceof RemoteCastable) {
        result = castToLocal(value, tryFixReturnType(value, methodReturnType, classLoader));
      }
      else {
        result = substituteClassLoader ? substituteClassLoader(value, classLoader) : value;
      }
    }
    else if (value instanceof List && methodReturnType.isInterface()) {
      result = Arrays.asList((Object[])handleRemoteResult(((List)value).toArray(), Object.class, classLoader, substituteClassLoader));
    }
    else if (value instanceof Object[]) {
      Object[] array = (Object[])value;
      for (int i = 0; i < array.length; i++) {
        array[i] = handleRemoteResult(array[i], Object.class, classLoader, substituteClassLoader);
      }
      result = array;
    }
    else {
      result = value;
    }
    return (T)result;
  }

  private static boolean canThrow(Throwable cause, Method method) {
    for (Class<?> each : method.getExceptionTypes()) {
      if (each.isInstance(cause)) return true;
    }
    return false;
  }

  public static <T> T executeWithClassLoader(final Callable<T> action, final ClassLoader classLoader) throws Exception {
    final Thread thread = Thread.currentThread();
    final ClassLoader prev = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(classLoader);
      return action.call();
    }
    finally {
      thread.setContextClassLoader(prev);
    }
  }

  /**
   * There is a possible case that remotely called code throws exception during processing. That exception is wrapped at different
   * levels then - {@link InvocationTargetException}, {@link UndeclaredThrowableException} etc.
   * <p/>
   * This method tries to extract the 'real exception' from the given potentially wrapped one.
   *
   * @param e exception to process
   * @return extracted 'real exception' if any; given exception otherwise
   */
  @Nonnull
  public static Throwable unwrap(@Nonnull Throwable e) {
    for (Throwable candidate = e; candidate != null; candidate = candidate.getCause()) {
      Class<? extends Throwable> clazz = candidate.getClass();
      if (clazz != InvocationTargetException.class && clazz != UndeclaredThrowableException.class) {
        return candidate;
      }
    }
    return e;
  }

  private static class RemoteInvocationHandler implements InvocationHandler {
    private final Object myRemote;
    private final Class<?> myClazz;
    private final ClassLoader myLoader;

    public RemoteInvocationHandler(Object remote, Class<?> clazz, ClassLoader loader) {
      myRemote = remote;
      myClazz = clazz;
      myLoader = loader;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(myRemote, args);
      }
      else {
        Method m = getOrCreateRemoteToLocal(new AbstractMap.SimpleEntry<Class<?>, Class<?>>(myRemote.getClass(), myClazz)).get(method);
        if (m == null) throw new NoSuchMethodError(method.getName() + " in " + myRemote.getClass());
        try {
          return handleRemoteResult(m.invoke(myRemote, args), method.getReturnType(), myLoader, false);
        }
        catch (InvocationTargetException e) {
          Throwable cause = e.getCause(); // root cause may go deeper than we need, so leave it like this
          if (cause instanceof ServerError) cause = cause.getCause() == null ? cause : cause.getCause();
          if (cause instanceof RuntimeException) throw cause;
          if (cause instanceof Error) throw cause;
          if (canThrow(cause, method)) throw cause;
          throw new RuntimeException(cause);
        }
      }
    }
  }
}
