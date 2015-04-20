package org.mustbe.consulo.extensions;

import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author VISTALL
 * @since 20.04.2015
 *
 * @see com.intellij.util.EventDispatcher
 */
@Logger
public abstract class CompositeExtensionPointName<T> {
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface BooleanBreakResult {
    boolean breakValue();
  }

  @NotNull
  public static <E> CompositeExtensionPointName<E> modulePoint(@NotNull String epName, @NotNull Class<E> clazz) {
    return new CompositeExtensionPointNameWithArea<E>(epName, clazz) {
      @Override
      protected boolean validateArea(@Nullable AreaInstance areaInstance) {
        return areaInstance instanceof Module;
      }
    };
  }

  @NotNull
  public static <E> CompositeExtensionPointName<E> projectPoint(@NotNull String epName, @NotNull Class<E> clazz) {
    return new CompositeExtensionPointNameWithArea<E>(epName, clazz) {
      @Override
      protected boolean validateArea(@Nullable AreaInstance areaInstance) {
        return areaInstance instanceof Project;
      }
    };
  }

  @NotNull
  public static <E> CompositeExtensionPointName<E> applicationPoint(@NotNull String epName, @NotNull Class<E> clazz) {
    return new CompositeExtensionPointNameNoArea<E>(epName, clazz);
  }

  private static class MyInvocationHandler<T> implements InvocationHandler {
    private final String myName;
    private final T[] myExtensions;

    public MyInvocationHandler(String name, T[] extensions) {
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
        if(returnType == Void.TYPE) {
          return invokeVoidMethod(method, args);
        }
        else if(returnType == Boolean.TYPE) {
          return invokeBooleanMethod(method, args);
        }
        return invokeMethod(method, args);
      }
    }

    private Object invokeMethod(@NotNull Method method, Object[] args) {
      method.setAccessible(true);

      for (T listener : myExtensions) {
        try {
          Object value = method.invoke(listener, args);
          if(value != null) {
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

    @NotNull
    private Boolean invokeBooleanMethod(@NotNull Method method, Object[] args) {
      method.setAccessible(true);

      Boolean breakValue = Boolean.TRUE;
      BooleanBreakResult annotation = method.getAnnotation(BooleanBreakResult.class);
      if(annotation != null) {
        breakValue = annotation.breakValue();
      }

      for (T listener : myExtensions) {
        try {
          Boolean value = (Boolean)method.invoke(listener, args);
          if(breakValue.equals(value)) {
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

    private Object invokeVoidMethod(@NotNull Method method, Object[] args) {
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

    protected CompositeExtensionPointNameNoArea(@NotNull String name, Class<E> clazz) {
      super(name, clazz);
    }

    @Nullable
    @Override
    protected E getCompositeValue(@Nullable AreaInstance areaInstance) {
      return myCompositeValue;
    }

    @Override
    protected void putCompositeValue(@Nullable AreaInstance areaInstance, E compositeValue) {
      myCompositeValue = compositeValue;
    }

    @Override
    protected boolean validateArea(@Nullable AreaInstance areaInstance) {
      return areaInstance == null;
    }
  }

  private static abstract class CompositeExtensionPointNameWithArea<E> extends CompositeExtensionPointName<E> {
    private Key<E> myCompositeValueKey;

    private CompositeExtensionPointNameWithArea(@NotNull String name, Class<E> clazz) {
      super(name, clazz);
      myCompositeValueKey = Key.create("CompositeExtensionPoint#" + name);
    }

    @Nullable
    @Override
    protected E getCompositeValue(@Nullable AreaInstance areaInstance) {
      assert areaInstance instanceof UserDataHolder;
      return ((UserDataHolder)areaInstance).getUserData(myCompositeValueKey);
    }

    @Override
    protected void putCompositeValue(@Nullable AreaInstance areaInstance, E compositeValue) {
      assert areaInstance instanceof UserDataHolder;
      ((UserDataHolder)areaInstance).putUserData(myCompositeValueKey, compositeValue);
    }
  }

  private final String myName;
  private final Class<T> myClazz;

  protected CompositeExtensionPointName(@NotNull String name, @NotNull Class<T> clazz) {
    myName = name;
    myClazz = clazz;
  }

  @NotNull
  private T buildCompositeValue(@Nullable AreaInstance areaInstance) {
    final T[] extensions = Extensions.getExtensions(myName, areaInstance);

    InvocationHandler handler = new MyInvocationHandler<T>(myName, extensions);

    //noinspection unchecked
    return (T)Proxy.newProxyInstance(myClazz.getClassLoader(), new Class[]{myClazz}, handler);
  }

  @Nullable
  protected abstract T getCompositeValue(@Nullable AreaInstance areaInstance);

  protected abstract void putCompositeValue(@Nullable AreaInstance areaInstance, T compositeValue);

  protected abstract boolean validateArea(@Nullable AreaInstance areaInstance);

  @NotNull
  public T composite() {
    return composite(null);
  }

  @NotNull
  public T composite(@Nullable AreaInstance areaInstance) {
    if(!validateArea(areaInstance)) {
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
