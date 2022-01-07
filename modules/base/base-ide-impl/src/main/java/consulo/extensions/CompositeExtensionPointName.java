/*
 * Copyright 2013-2016 consulo.io
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
package consulo.extensions;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @author VISTALL
 * @see com.intellij.util.EventDispatcher
 * @since 20.04.2015
 */
public abstract class CompositeExtensionPointName<T> {
  public static final Logger LOGGER = Logger.getInstance(CompositeExtensionPointName.class);

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface BooleanBreakResult {
    boolean breakValue();
  }

  @Nonnull
  public static <E> CompositeExtensionPointName<E> modulePoint(@Nonnull String epName, @Nonnull Class<E> clazz) {
    return new CompositeExtensionPointNameWithArea<E>(epName, clazz) {
      @Override
      protected boolean validateArea(@Nullable ComponentManager componentManager) {
        return componentManager instanceof Module;
      }
    };
  }

  @Nonnull
  public static <E> CompositeExtensionPointName<E> projectPoint(@Nonnull String epName, @Nonnull Class<E> clazz) {
    return new CompositeExtensionPointNameWithArea<E>(epName, clazz) {
      @Override
      protected boolean validateArea(@Nullable ComponentManager componentManager) {
        return componentManager instanceof Project;
      }
    };
  }

  @Nonnull
  public static <E> CompositeExtensionPointName<E> applicationPoint(@Nonnull String epName, @Nonnull Class<E> clazz) {
    return new CompositeExtensionPointNameNoArea<E>(epName, clazz);
  }

  private static class MyInvocationHandler<T> implements InvocationHandler {
    private final ExtensionPointName<T> myName;
    private final List<T> myExtensions;

    public MyInvocationHandler(ExtensionPointName<T> name, List<T> extensions) {
      myName = name;
      myExtensions = extensions;
    }

    @Override
    @NonNls
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
      if (method.getDeclaringClass().getName().equals("java.lang.Object")) {
        @NonNls String methodName = method.getName();
        if (methodName.equals("toString")) {
          return "CompositeValue: " + myName;
        }
        else if (methodName.equals("hashCode")) {
          return Integer.valueOf(System.identityHashCode(proxy));
        }
        else if (methodName.equals("equals")) {
          return proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
        }
        else {
          CompositeExtensionPointName.LOGGER.error("Incorrect Object's method invoked for proxy:" + methodName);
          return null;
        }
      }
      else {
        Class<?> returnType = method.getReturnType();
        if (returnType == Void.TYPE) {
          return invokeVoidMethod(method, args);
        }
        else if (returnType == Boolean.TYPE) {
          return invokeBooleanMethod(method, args);
        }
        return invokeMethod(method, args);
      }
    }

    private Object invokeMethod(@Nonnull Method method, Object[] args) {
      method.setAccessible(true);

      for (T listener : myExtensions) {
        try {
          Object value = method.invoke(listener, args);
          if (value != null) {
            return value;
          }
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
          if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
          }
          else if (!(cause instanceof AbstractMethodError)) { // AbstractMethodError means this listener doesn't implement some new method in interface
            CompositeExtensionPointName.LOGGER.error(cause);
          }
        }
      }
      return null;
    }

    @Nonnull
    private Boolean invokeBooleanMethod(@Nonnull Method method, Object[] args) {
      method.setAccessible(true);

      Boolean breakValue = Boolean.TRUE;
      BooleanBreakResult annotation = method.getAnnotation(BooleanBreakResult.class);
      if (annotation != null) {
        breakValue = annotation.breakValue();
      }

      for (T listener : myExtensions) {
        try {
          Boolean value = (Boolean)method.invoke(listener, args);
          if (breakValue.equals(value)) {
            return value;
          }
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
          if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
          }
          else if (!(cause instanceof AbstractMethodError)) { // AbstractMethodError means this listener doesn't implement some new method in interface
            CompositeExtensionPointName.LOGGER.error(cause);
          }
        }
      }
      return !breakValue;
    }

    private Object invokeVoidMethod(@Nonnull Method method, Object[] args) {
      method.setAccessible(true);

      for (T listener : myExtensions) {
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
          if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
          }
          else if (!(cause instanceof AbstractMethodError)) { // AbstractMethodError means this listener doesn't implement some new method in interface
            CompositeExtensionPointName.LOGGER.error(cause);
          }
        }
      }
      return null;
    }
  }

  private static class CompositeExtensionPointNameNoArea<E> extends CompositeExtensionPointName<E> {
    private volatile E myCompositeValue;

    protected CompositeExtensionPointNameNoArea(@Nonnull String name, Class<E> clazz) {
      super(name, clazz);
    }

    @Nullable
    @Override
    protected E getCompositeValue(@Nullable ComponentManager componentManager) {
      return myCompositeValue;
    }

    @Override
    protected void putCompositeValue(@Nullable ComponentManager componentManager, E compositeValue) {
      myCompositeValue = compositeValue;
    }

    @Override
    protected boolean validateArea(@Nullable ComponentManager componentManager) {
      return componentManager == null;
    }
  }

  private static abstract class CompositeExtensionPointNameWithArea<E> extends CompositeExtensionPointName<E> {
    private Key<E> myCompositeValueKey;

    private CompositeExtensionPointNameWithArea(@Nonnull String name, Class<E> clazz) {
      super(name, clazz);
      myCompositeValueKey = Key.create("CompositeExtensionPoint#" + name);
    }

    @Nullable
    @Override
    protected E getCompositeValue(@Nullable ComponentManager componentManager) {
      return componentManager.getUserData(myCompositeValueKey);
    }

    @Override
    protected void putCompositeValue(@Nullable ComponentManager componentManager, E compositeValue) {
      componentManager.putUserData(myCompositeValueKey, compositeValue);
    }
  }

  private final ExtensionPointName<T> myName;
  private final Class<T> myClazz;

  protected CompositeExtensionPointName(@Nonnull String name, @Nonnull Class<T> clazz) {
    myName = ExtensionPointName.create(name);
    myClazz = clazz;
  }

  @Nonnull
  private T buildCompositeValue(@Nullable ComponentManager componentManager) {
    final List<T> extensions = myName.getExtensionList(componentManager == null ? Application.get() : componentManager);

    InvocationHandler handler = new MyInvocationHandler<>(myName, extensions);

    //noinspection unchecked
    return (T)Proxy.newProxyInstance(myClazz.getClassLoader(), new Class[]{myClazz}, handler);
  }

  @Nullable
  protected abstract T getCompositeValue(@Nullable ComponentManager componentManager);

  protected abstract void putCompositeValue(@Nullable ComponentManager componentManager, T compositeValue);

  protected abstract boolean validateArea(@Nullable ComponentManager componentManager);

  @Nonnull
  public T composite() {
    return composite(null);
  }

  @Nonnull
  public T composite(@Nullable ComponentManager areaInstance) {
    if (!validateArea(areaInstance)) {
      throw new IllegalArgumentException("Wrong area instance for '" + myName + "' extension point");
    }
    T compositeValue = getCompositeValue(areaInstance);
    if (compositeValue != null) {
      return compositeValue;
    }

    compositeValue = buildCompositeValue(areaInstance);
    putCompositeValue(areaInstance, compositeValue);
    return compositeValue;
  }
}
