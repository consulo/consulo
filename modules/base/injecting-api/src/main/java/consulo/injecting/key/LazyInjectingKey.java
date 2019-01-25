/*
 * Copyright 2013-2018 consulo.io
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
package consulo.injecting.key;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public class LazyInjectingKey<T> implements InjectingKey<T> {
  private String myClassName;
  private ClassLoader myClassLoader;

  private Class<T> myResolvedClass;

  LazyInjectingKey(String className, ClassLoader classLoader) {
    myClassName = className;
    myClassLoader = classLoader;
  }

  @Nonnull
  @Override
  public String getTargetClassName() {
    return myClassName;
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Class<T> getTargetClass() {
    if(myResolvedClass != null) {
      return myResolvedClass;
    }
    try {
      return myResolvedClass = (Class<T>)Class.forName(myClassName, false, myClassLoader);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof InjectingKey && obj.hashCode() == hashCode();
  }

  @Override
  public int hashCode() {
    return myClassName.hashCode();
  }

  @Override
  public String toString() {
    return myClassName;
  }
}
