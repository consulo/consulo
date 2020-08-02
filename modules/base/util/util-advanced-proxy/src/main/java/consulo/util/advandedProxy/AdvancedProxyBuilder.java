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
package consulo.util.advandedProxy;

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
import net.sf.cglib.proxy.AdvancedProxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-06-18
 */
public class AdvancedProxyBuilder<T> {
  private static final boolean enableByteBuddyProxy = Boolean.getBoolean("consulo.enable.byte.buddy.proxy");

  @Nonnull
  public static <K> AdvancedProxyBuilder<K> create(@Nullable Class<K> superClass) {
    return new AdvancedProxyBuilder<>(superClass);
  }

  private Class<T> mySuperClass;
  private Object[] mySuperConstructorArguments = ArrayUtil.EMPTY_OBJECT_ARRAY;

  private Class[] myInterfaces = ArrayUtil.EMPTY_CLASS_ARRAY;
  private InvocationHandler myInvocationHandler;

  private boolean myInterceptObjectMethods = true;

  private AdvancedProxyBuilder(@Nullable Class<T> superClass) {
    mySuperClass = superClass;
  }

  @Nonnull
  public AdvancedProxyBuilder<T> withSuperConstructorArguments(@Nonnull Object... args) {
    mySuperConstructorArguments = args;
    return this;
  }

  @Nonnull
  public AdvancedProxyBuilder<T> withInterfaces(@Nonnull Class... interfaces) {
    myInterfaces = interfaces;
    return this;
  }

  @Nonnull
  public AdvancedProxyBuilder<T> withInterfaces(@Nonnull Collection<Class> interfaces) {
    return withInterfaces(interfaces.toArray(ArrayUtil.EMPTY_CLASS_ARRAY));
  }

  @Nonnull
  public AdvancedProxyBuilder<T> withInvocationHandler(@Nonnull InvocationHandler invocationHandler) {
    myInvocationHandler = invocationHandler;
    return this;
  }

  @Nonnull
  public AdvancedProxyBuilder<T> withInterceptObjectMethods(boolean interceptObjectMethods) {
    myInterceptObjectMethods = interceptObjectMethods;
    return this;
  }

  @Nonnull
  @SuppressWarnings({"deprecation", "unchecked"})
  public T build() {
    Objects.requireNonNull(myInvocationHandler, "invocation handler must be set");
    if (enableByteBuddyProxy) {
      ByteBuddy buddy = new ByteBuddy();
      buddy = buddy.with(ClassFileVersion.JAVA_V8);
      buddy = buddy.with(TypeValidation.ENABLED);

      try {
        DynamicType.Builder<T> builder;

        if (mySuperClass != null) {
          builder = buddy.subclass(mySuperClass);
        }
        else {
          builder = (DynamicType.Builder<T>)buddy.subclass(Object.class);
        }

        Class[] params = ArrayUtil.EMPTY_CLASS_ARRAY;
        if (mySuperConstructorArguments.length > 0) {
          params = ProxyHelper.getConstructorParameterTypes(mySuperClass, mySuperConstructorArguments);
        }

        builder = builder.implement(myInterfaces);

        ElementMatcher.Junction<MethodDescription> junction = ElementMatchers.not(ElementMatchers.isDefaultMethod());
        //if(mySuperClass != null) {
        //  junction.and(ElementMatchers.not(ElementMatchers.isOverriddenFrom(mySuperClass)));
        //}

        if (!myInterceptObjectMethods) {
          junction = junction.and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)));
        }

        DynamicType.Builder.MethodDefinition.ImplementationDefinition<T> definition = builder.method(junction);

        DynamicType.Builder<T> intercept = definition.intercept(InvocationHandlerAdapter.of(myInvocationHandler));

        DynamicType.Unloaded<T> make = intercept.make();

        ClassLoader classLoader = ProxyHelper.preferClassLoader(mySuperClass, myInterfaces);

        DynamicType.Loaded<T> type = make.load(classLoader, ClassLoadingStrategy.Default.INJECTION);

        Class<? extends T> loaded = type.getLoaded();

        return loaded.getDeclaredConstructor(params).newInstance(mySuperConstructorArguments);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    else {
      return AdvancedProxy.createProxy(mySuperClass, myInterfaces, (o, method, objects) -> myInvocationHandler.invoke(o, method, objects), myInterceptObjectMethods, mySuperConstructorArguments);
    }
  }
}
