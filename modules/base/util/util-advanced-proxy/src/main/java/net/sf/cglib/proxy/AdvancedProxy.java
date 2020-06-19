package net.sf.cglib.proxy;

import consulo.annotation.DeprecationInfo;
import consulo.util.advandedProxy.ObjectMethods;
import consulo.util.advandedProxy.ProxyHelper;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Maps;
import consulo.util.lang.ControlFlowException;
import net.sf.cglib.core.CodeGenerationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

/**
 * @author peter
 */
@Deprecated
@DeprecationInfo("Use AdvancedProxyBuilder")
@SuppressWarnings("deprecation")
public class AdvancedProxy {
  public static final Method FINALIZE_METHOD = ObjectMethods.FINALIZE_METHOD;
  public static final Method EQUALS_METHOD = ObjectMethods.EQUALS_METHOD;
  public static final Method HASHCODE_METHOD = ObjectMethods.HASHCODE_METHOD;
  public static final Method TOSTRING_METHOD = ObjectMethods.TOSTRING_METHOD;

  private static final Map<ProxyDescription, Factory> ourFactories = Maps.newConcurrentWeakValueMap();
  private static final CallbackFilter NO_OBJECT_METHODS_FILTER = new CallbackFilter() {
    public int accept(Method method) {
      if (AdvancedProxy.FINALIZE_METHOD.equals(method)) {
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
      if (AdvancedProxy.FINALIZE_METHOD.equals(method)) {
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

  public static InvocationHandler getInvocationHandler(Object proxy) {
    return (InvocationHandler)((Factory) proxy).getCallback(0);
  }

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

  public static <T> T createProxy(final Class<T> superClass,
                                  final Class[] interfaces,
                                  final InvocationHandler handler, final Object... constructorArgs) {
    return createProxy(superClass, interfaces, handler, true, constructorArgs);
  }

  public static <T> T createProxy(final Class<T> superClass,
                                  final Class[] interfaces,
                                  final InvocationHandler handler,
                                  final boolean interceptObjectMethods, final Object... constructorArgs) {
    try {
      final Callback[] callbacks = new Callback[]{handler, NoOp.INSTANCE};

      final ProxyDescription key = new ProxyDescription(superClass, interfaces);
      Factory factory = ourFactories.get(key);
      if (factory != null) {
        return (T)factory.newInstance(ProxyHelper.getConstructorParameterTypes(factory.getClass(), constructorArgs), constructorArgs, callbacks);
      }

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

      ourFactories.put(key, factory);
      return (T)factory;
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
      if(e instanceof ControlFlowException) {
        throw e;
      }
      throw new RuntimeException(e);
    }
  }

  private static class ProxyDescription {
    private final Class mySuperClass;
    private final Class[] myInterfaces;

    public ProxyDescription(final Class superClass, final Class[] interfaces) {
      mySuperClass = superClass;
      myInterfaces = interfaces;
    }

    public String toString() {
      return mySuperClass + " " + (myInterfaces != null ? Arrays.asList(myInterfaces) : "");
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ProxyDescription that = (ProxyDescription)o;

      if (!Arrays.equals(myInterfaces, that.myInterfaces)) return false;
      if (mySuperClass != null ? !mySuperClass.equals(that.mySuperClass) : that.mySuperClass != null) return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (mySuperClass != null ? mySuperClass.hashCode() : 0);
      return result;
    }
  }

}
