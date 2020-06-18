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
import net.sf.cglib.proxy.AdvancedProxy;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationHandler;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-06-18
 */
public class AdvancedProxyBuilder<T> {
  public static <K> AdvancedProxyBuilder<K> create(@Nonnull Class<K> superClass) {
    return new AdvancedProxyBuilder<>(superClass);
  }

  private Class<T> mySuperClass;
  private Object[] mySuperConstructorArguments = ArrayUtil.EMPTY_OBJECT_ARRAY;

  private Class[] myInterfaces = ArrayUtil.EMPTY_CLASS_ARRAY;
  private InvocationHandler myInvocationHandler;

  private boolean myInterceptObjectMethods = true;

  private AdvancedProxyBuilder(@Nonnull Class<T> superClass) {
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
  @SuppressWarnings("deprecation")
  public T build() {
    Objects.requireNonNull(myInvocationHandler, "invocation handler must be set");
    return AdvancedProxy.createProxy(mySuperClass, myInterfaces, (o, method, objects) -> myInvocationHandler.invoke(o, method, objects), myInterceptObjectMethods, mySuperConstructorArguments);
  }
}
