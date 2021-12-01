/*
 * Copyright 2013-2020 consulo.io
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
package consulo.util.advandedProxy.internal.impl.bytebuddy;

import consulo.container.impl.classloader.proxy.ProxyDescription;
import consulo.container.impl.classloader.proxy.ProxyHolderClassLoader;
import consulo.util.advandedProxy.ProxyHelper;
import consulo.util.advandedProxy.internal.AdvancedProxyFacade;
import consulo.util.advandedProxy.internal.impl.AdvancedProxyTesting;
import consulo.util.collection.ArrayUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;

/**
 * @author VISTALL
 * @since 2020-09-10
 */
public class ByteBuddyAdvancedProxyFacade implements AdvancedProxyFacade {
  private static final String HANDLER_FIELD_NAME = "$$$invocationHandler";

  private final ByteBuddy myBuddy;

  public ByteBuddyAdvancedProxyFacade() {
    myBuddy = new ByteBuddy().with(ClassFileVersion.JAVA_V11).with(TypeValidation.DISABLED);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T create(Class<T> superClass, Class[] interfaces, InvocationHandler invocationHandler, boolean interceptObjectMethods, Object[] superConstructorArguments) {
    try {
      ClassLoader classLoader = ProxyHelper.preferClassLoader(superClass, interfaces);

      if (AdvancedProxyTesting.isProxyClassLoaderRequired()) {
        if (!(classLoader instanceof ProxyHolderClassLoader)) {
          throw new IllegalArgumentException("Must be ProxyHolderClassLoader: " + classLoader.getClass().getName());
        }
      }

      Class[] params = ArrayUtil.EMPTY_CLASS_ARRAY;
      if (superConstructorArguments.length > 0) {
        params = ProxyHelper.getConstructorParameterTypes(superClass, superConstructorArguments);
      }

      ProxyDescription key = new ProxyDescription(superClass, interfaces, interceptObjectMethods);

      ByteBuddyProxyFactory proxyFactory;
      if (AdvancedProxyTesting.isProxyClassLoaderRequired()) {
        proxyFactory =
                (ByteBuddyProxyFactory)((ProxyHolderClassLoader)classLoader).registerOrGetProxy(key, it -> buildProxyClass(it.getSuperClass(), it.getInterfaces(), it.isInterceptObjectMethods()));
      }
      else {
        proxyFactory = buildProxyClass(key.getSuperClass(), key.getInterfaces(), key.isInterceptObjectMethods());
      }

      Class<?> proxyClass = proxyFactory.getProxyClass();
      Field handlerField = proxyFactory.getInvocationHandlerField();

      T proxyInstance = (T)proxyClass.getDeclaredConstructor(params).newInstance(superConstructorArguments);
      handlerField.set(proxyInstance, invocationHandler);
      return proxyInstance;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> ByteBuddyProxyFactory buildProxyClass(Class<T> superClass, Class[] interfaces, boolean interceptObjectMethods) {
    DynamicType.Builder<? extends T> builder;

    if (superClass != null) {
      builder = myBuddy.subclass(superClass);
    }
    else {
      builder = (DynamicType.Builder<T>)myBuddy.subclass(Object.class);
    }

    builder = builder.implement(interfaces);

    ElementMatcher.Junction<MethodDescription> junction = ElementMatchers.not(ElementMatchers.isDefaultMethod());
    if (superClass != null) {
      junction = junction.and(ElementMatchers.not(ElementMatchers.isDeclaredBy(superClass)));
    }

    if (interceptObjectMethods) {
      junction = junction.and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)));
    }

    DynamicType.Builder.MethodDefinition.ImplementationDefinition<? extends T> definition = builder.method(junction);

    DynamicType.Builder<? extends T> intercept = definition.intercept(InvocationHandlerAdapter.toField(HANDLER_FIELD_NAME));

    DynamicType.Unloaded<? extends T> make = intercept.defineField(HANDLER_FIELD_NAME, InvocationHandler.class, Modifier.PUBLIC).make();

    ClassLoader classLoader = ProxyHelper.preferClassLoader(superClass, interfaces);

    DynamicType.Loaded<? extends T> type;

    if (AdvancedProxyTesting.isProxyClassLoaderRequired()) {
      if (!(classLoader instanceof ProxyHolderClassLoader)) {
        throw new IllegalArgumentException("Must be ProxyHolderClassLoader: " + classLoader.getClass().getName());
      }

      type = make.load(classLoader, new UrlClassLoaderStrategy(classLoader));
    }
    else {
      type = make.load(classLoader, new ClassLoadingStrategy.ForUnsafeInjection());
    }

    Class<? extends T> loaded = type.getLoaded();
    Field handlerField = null;
    try {
      handlerField = loaded.getDeclaredField(HANDLER_FIELD_NAME);
    }
    catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }

    return new ByteBuddyProxyFactory(loaded, handlerField);
  }
}
