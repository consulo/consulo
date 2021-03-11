package net.sf.cglib.proxy;

import consulo.container.impl.classloader.proxy.ProxyDescription;
import consulo.container.impl.classloader.proxy.ProxyHolderClassLoader;
import consulo.util.advandedProxy.ObjectMethods;
import consulo.util.advandedProxy.ProxyHelper;
import consulo.util.advandedProxy.internal.AdvancedProxyFacade;
import consulo.util.advandedProxy.internal.impl.cglib.CglibProxyFactory;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Maps;
import consulo.util.lang.ControlFlowException;
import net.sf.cglib.core.CodeGenerationException;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * @author peter
 */
public class CglibAdvancedProxyFacade implements AdvancedProxyFacade {
  public static final Method FINALIZE_METHOD = ObjectMethods.FINALIZE_METHOD;
  public static final Method EQUALS_METHOD = ObjectMethods.EQUALS_METHOD;
  public static final Method HASHCODE_METHOD = ObjectMethods.HASHCODE_METHOD;
  public static final Method TOSTRING_METHOD = ObjectMethods.TOSTRING_METHOD;

  private static final CallbackFilter NO_OBJECT_METHODS_FILTER = new CallbackFilter() {
    public int accept(Method method) {
      if (CglibAdvancedProxyFacade.FINALIZE_METHOD.equals(method)) {
        return 1;
      }

      if ((method.getModifiers() & Modifier.ABSTRACT) != 0) {
        return 0;
      }

      return 1;
    }
  };
  private static final CallbackFilter WITH_OBJECT_METHODS_FILTER = new CallbackFilter() {
    public int accept(Method method) {
      if (CglibAdvancedProxyFacade.FINALIZE_METHOD.equals(method)) {
        return 1;
      }

      if (HASHCODE_METHOD.equals(method) || TOSTRING_METHOD.equals(method) || EQUALS_METHOD.equals(method)) {
        return 0;
      }

      if ((method.getModifiers() & Modifier.ABSTRACT) != 0) {
        return 0;
      }

      return 1;
    }
  };

  public static <T> T createProxy(final InvocationHandler handler, final Class<T> superClass, final Class... otherInterfaces) {
    return createProxy(superClass, otherInterfaces, handler, ArrayUtil.EMPTY_OBJECT_ARRAY);
  }

  public static <T> T createProxy(final Class<T> superClass, final Class... otherInterfaces) {
    return createProxy(superClass, otherInterfaces, new InvocationHandler() {
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        throw new AbstractMethodError(method.toString());
      }
    }, false, ArrayUtil.EMPTY_OBJECT_ARRAY);
  }

  public static <T> T createProxy(final Class<T> superClass, final Class[] interfaces, final InvocationHandler handler, final Object... constructorArgs) {
    return createProxy(superClass, interfaces, handler, true, constructorArgs);
  }

  @Nonnull
  @Override
  public <T> T create(Class<T> superClass, Class[] interfaces, java.lang.reflect.InvocationHandler invocationHandler, boolean interceptObjectMethods, Object[] superConstructorArguments) {
    return createProxy(superClass, interfaces, invocationHandler::invoke, true, superConstructorArguments);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T> T createProxy(final Class<T> superClass, final Class[] interfaces, final InvocationHandler handler, final boolean interceptObjectMethods, final Object... constructorArgs) {
    try {
      ClassLoader classLoader = ProxyHelper.preferClassLoader(superClass, interfaces);

      if (!(classLoader instanceof ProxyHolderClassLoader)) {
        throw new IllegalArgumentException("Must be ProxyHolderClassLoader: " + classLoader.getClass().getName());
      }

      final ProxyDescription key = new ProxyDescription(superClass, interfaces, interceptObjectMethods);

      CglibProxyFactory proxyFactory = (CglibProxyFactory)((ProxyHolderClassLoader)classLoader)
              .registerOrGetProxy(key, description -> createProxyImpl(superClass, interfaces, handler, interceptObjectMethods, constructorArgs));

      final Callback[] callbacks = new Callback[]{handler, NoOp.INSTANCE};

      Factory factory = proxyFactory.getFactory();

      return (T)factory.newInstance(ProxyHelper.getConstructorParameterTypes(factory.getClass(), constructorArgs), constructorArgs, callbacks);
    }
    catch (CodeGenerationException e) {
      final Throwable throwable = e.getCause();
      if (throwable instanceof InvocationTargetException) {
        final InvocationTargetException targetException = (InvocationTargetException)throwable;
        final Throwable cause = targetException.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException)cause;
        }
        if (cause instanceof Error) {
          throw (Error)cause;
        }
      }
      if (throwable instanceof RuntimeException) {
        throw (RuntimeException)throwable;
      }
      throw e;
    }
    catch (Exception e) {
      if (e instanceof ControlFlowException) {
        throw e;
      }
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  private static CglibProxyFactory createProxyImpl(final Class<?> superClass,
                                                   final Class[] interfaces,
                                                   final InvocationHandler handler,
                                                   final boolean interceptObjectMethods,
                                                   final Object... constructorArgs) {
    try {
      ClassLoader classLoader = ProxyHelper.preferClassLoader(superClass, interfaces);

      if (!(classLoader instanceof ProxyHolderClassLoader)) {
        throw new IllegalArgumentException("Must be ProxyHolderClassLoader: " + classLoader.getClass().getName());
      }

      final Callback[] callbacks = new Callback[]{handler, NoOp.INSTANCE};

      Factory factory;
      AdvancedEnhancer e = new AdvancedEnhancer();
      e.setInterfaces(interfaces);
      e.setCallbacks(callbacks);
      e.setCallbackFilter(interceptObjectMethods ? WITH_OBJECT_METHODS_FILTER : NO_OBJECT_METHODS_FILTER);
      if (superClass != null) {
        e.setSuperclass(superClass);
        factory = (Factory)e.create(ProxyHelper.getConstructorParameterTypes(superClass, constructorArgs), constructorArgs);
      }
      else {
        assert constructorArgs.length == 0;
        factory = (Factory)e.create();
      }
      return new CglibProxyFactory(factory);
    }
    catch (CodeGenerationException e) {
      final Throwable throwable = e.getCause();
      if (throwable instanceof InvocationTargetException) {
        final InvocationTargetException targetException = (InvocationTargetException)throwable;
        final Throwable cause = targetException.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException)cause;
        }
        if (cause instanceof Error) {
          throw (Error)cause;
        }
      }
      if (throwable instanceof RuntimeException) {
        throw (RuntimeException)throwable;
      }
      throw e;
    }
    catch (Exception e) {
      if (e instanceof ControlFlowException) {
        throw e;
      }
      throw new RuntimeException(e);
    }
  }
}
