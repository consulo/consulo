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
package consulo.util.advandedProxy.internal.impl;

import consulo.container.classloader.PluginClassLoader;
import consulo.util.advandedProxy.ProxyHelper;
import consulo.util.advandedProxy.internal.AdvancedProxyFacade;
import consulo.util.collection.ArrayUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationHandler;

/**
 * @author VISTALL
 * @since 2020-09-10
 */
public class ByteBuddyAdvancedProxyFacade implements AdvancedProxyFacade {
  private final ByteBuddy myBuddy;

  public ByteBuddyAdvancedProxyFacade() {
    myBuddy = new ByteBuddy().with(ClassFileVersion.JAVA_V8).with(TypeValidation.ENABLED);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T create(Class<T> superClass, Class[] interfaces, InvocationHandler invocationHandler, boolean interceptObjectMethods, Object[] superConstructorArguments) {

    try {
      DynamicType.Builder<T> builder;

      if (superClass != null) {
        builder = myBuddy.subclass(superClass);
      }
      else {
        builder = (DynamicType.Builder<T>)myBuddy.subclass(Object.class);
      }

      Class[] params = ArrayUtil.EMPTY_CLASS_ARRAY;
      if (superConstructorArguments.length > 0) {
        params = ProxyHelper.getConstructorParameterTypes(superClass, superConstructorArguments);
      }

      builder = builder.implement(interfaces);

      ElementMatcher.Junction<MethodDescription> junction = ElementMatchers.not(ElementMatchers.isDefaultMethod());
      //if(mySuperClass != null) {
      //  junction.and(ElementMatchers.not(ElementMatchers.isOverriddenFrom(mySuperClass)));
      //}

      if (!interceptObjectMethods) {
        junction = junction.and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)));
      }

      DynamicType.Builder.MethodDefinition.ImplementationDefinition<T> definition = builder.method(junction);

      DynamicType.Builder<T> intercept = definition.intercept(InvocationHandlerAdapter.of(invocationHandler));

      DynamicType.Unloaded<T> make = intercept.make();

      ClassLoader classLoader = ProxyHelper.preferClassLoader(superClass, interfaces);

      if(!(classLoader instanceof PluginClassLoader)) {
        throw new IllegalArgumentException("Must be PluginClassLoader: " + classLoader.getClass().getName());
      }

      DynamicType.Loaded<T> type = make.load(classLoader, new UrlClassLoaderStrategy(classLoader));

      Class<? extends T> loaded = type.getLoaded();

      return loaded.getDeclaredConstructor(params).newInstance(superConstructorArguments);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
