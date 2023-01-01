// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.proxy;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.disposer.util.DisposableList;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.reflect.ReflectionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author max
 */
public class EventDispatcher<T extends EventListener> {
  private static final Logger LOG = Logger.getInstance(EventDispatcher.class);

  private T myMulticaster;

  private final DisposableList<T> myListeners = DisposableList.create();
  @Nonnull
  private final Class<T> myListenerClass;
  @Nullable
  private final Map<String, Object> myMethodReturnValues;

  @Nonnull
  public static <T extends EventListener> EventDispatcher<T> create(@Nonnull Class<T> listenerClass) {
    return new EventDispatcher<>(listenerClass, null);
  }

  @Nonnull
  public static <T extends EventListener> EventDispatcher<T> create(@Nonnull Class<T> listenerClass, @Nonnull Map<String, Object> methodReturnValues) {
    assertNonVoidMethodReturnValuesAreDeclared(methodReturnValues, listenerClass);
    return new EventDispatcher<>(listenerClass, methodReturnValues);
  }

  private static void assertNonVoidMethodReturnValuesAreDeclared(@Nonnull Map<String, Object> methodReturnValues, @Nonnull Class<?> listenerClass) {
    List<Method> declared = new ArrayList<>(ReflectionUtil.getClassPublicMethods(listenerClass));
    for (final Map.Entry<String, Object> entry : methodReturnValues.entrySet()) {
      final String methodName = entry.getKey();
      Method found = ContainerUtil.find(declared, m -> methodName.equals(m.getName()));
      assert found != null : "Method " + methodName + " must be declared in " + listenerClass;
      assert !found.getReturnType().equals(void.class) : "Method " + methodName + " must be non-void if you want to specify what its proxy should return";
      Object returnValue = entry.getValue();
      assert ReflectionUtil.boxType(found.getReturnType()).isAssignableFrom(returnValue.getClass()) : "You specified that method " +
                                                                                                      methodName +
                                                                                                      " proxy will return " +
                                                                                                      returnValue +
                                                                                                      " but its return type is " +
                                                                                                      found.getReturnType() +
                                                                                                      " which is incompatible with " +
                                                                                                      returnValue.getClass();
      declared.remove(found);
    }
    for (Method method : declared) {
      assert method.getReturnType().equals(void.class) : "Method " + method + " returns " + method.getReturnType() + " and yet you didn't specify what its proxy should return";
    }
  }

  private EventDispatcher(@Nonnull Class<T> listenerClass, @Nullable Map<String, Object> methodReturnValues) {
    myListenerClass = listenerClass;
    myMethodReturnValues = methodReturnValues;
  }

  @Nonnull
  public static <T> T createMulticaster(@Nonnull Class<T> listenerClass, @Nullable final Map<String, Object> methodReturnValues, final Supplier<? extends Iterable<T>> listeners) {
    LOG.assertTrue(listenerClass.isInterface(), "listenerClass must be an interface");
    InvocationHandler handler = (proxy, method, args) -> {
      String methodName = method.getName();
      if (method.getDeclaringClass().getName().equals("java.lang.Object")) {
        return handleObjectMethod(proxy, args, methodName);
      }
      else if (methodReturnValues != null && methodReturnValues.containsKey(methodName)) {
        return methodReturnValues.get(methodName);
      }
      else {
        dispatchVoidMethod(listeners.get(), method, args);
        return null;
      }
    };

    //noinspection unchecked
    return (T)Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, handler);
  }

  @Nullable
  public static Object handleObjectMethod(Object proxy, Object[] args, String methodName) {
    if (methodName.equals("toString")) {
      return "Multicaster";
    }
    else if (methodName.equals("hashCode")) {
      return System.identityHashCode(proxy);
    }
    else if (methodName.equals("equals")) {
      return proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
    }
    else {
      LOG.error("Incorrect Object's method invoked for proxy:" + methodName);
      return null;
    }
  }

  @Nonnull
  public T getMulticaster() {
    T multicaster = myMulticaster;
    if (multicaster == null) {
      // benign race
      myMulticaster = multicaster = createMulticaster(myListenerClass, myMethodReturnValues, () -> myListeners);
    }
    return multicaster;
  }

  private static <T> void dispatchVoidMethod(@Nonnull Iterable<? extends T> listeners, @Nonnull Method method, Object[] args) {
    method.setAccessible(true);

    for (T listener : listeners) {
      try {
        method.invoke(listener, args);
      }
      catch (AbstractMethodError ignored) {
        // Do nothing. This listener just does not implement something newly added yet.
        // AbstractMethodError is normally wrapped in InvocationTargetException,
        // but some Java versions didn't do it in some cases (see http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6531596)
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        final Throwable cause = e.getCause();
        ExceptionUtil.rethrowUnchecked(cause);
        if (!(cause instanceof AbstractMethodError)) { // AbstractMethodError means this listener doesn't implement some new method in interface
          LOG.error(cause);
        }
      }
    }
  }

  public void addListener(@Nonnull T listener) {
    myListeners.add(listener);
  }

  public void addListener(@Nonnull T listener, @Nonnull Disposable parentDisposable) {
    myListeners.add(listener, parentDisposable);
  }

  public void removeListener(@Nonnull T listener) {
    myListeners.remove(listener);
  }

  public boolean hasListeners() {
    return !myListeners.isEmpty();
  }

  @Nonnull
  public List<T> getListeners() {
    return myListeners;
  }

  //@TestOnly
  public void neuterMultiCasterWhilePerformanceTestIsRunningUntil(@Nonnull Disposable disposable) {
    T multicaster = myMulticaster;
    myMulticaster = createMulticaster(myListenerClass, myMethodReturnValues, List::of);
    Disposer.register(disposable, () -> myMulticaster = multicaster);
  }
}
