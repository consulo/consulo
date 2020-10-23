/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.util.xmlb;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class PropertyAccessor implements MutableAccessor {
  private final String myName;
  private final Class<?> myType;
  private final Method myReadMethod;
  private final Method myWriteMethod;
  private final Type myGenericType;

  public PropertyAccessor(String name, Class<?> type, @Nonnull Method readMethod, @Nonnull Method writeMethod) {
    myName = name;
    myType = type;
    myReadMethod = readMethod;
    myWriteMethod = writeMethod;
    myGenericType = myReadMethod.getGenericReturnType();

    try {
      myReadMethod.setAccessible(true);
      myWriteMethod.setAccessible(true);
    }
    catch (SecurityException ignored) { }
  }

  @Override
  public Object read(@Nonnull Object o) {
    try {
      return myReadMethod.invoke(o);
    }
    catch (IllegalAccessException e) {
      throw new XmlSerializationException(e);
    }
    catch (InvocationTargetException e) {
      Throwable exception = e.getTargetException();
      if (exception instanceof Error) throw (Error)exception;
      if (exception instanceof RuntimeException) throw (RuntimeException)exception;
      throw new XmlSerializationException(e);
    }
  }

  @Override
  public void set(@Nonnull Object host, @Nullable Object value) {
    try {
      myWriteMethod.invoke(host, value);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new XmlSerializationException(e);
    }
  }

  @Override
  public void setBoolean(@Nonnull Object host, boolean value) {
    set(host, value);
  }

  @Override
  public void setInt(@Nonnull Object host, int value) {
    set(host, value);
  }

  @Override
  public void setShort(@Nonnull Object host, short value) {
    set(host, value);
  }

  @Override
  public void setLong(@Nonnull Object host, long value) {
    set(host, value);
  }

  @Override
  public void setDouble(@Nonnull Object host, double value) {
    set(host, value);
  }

  @Override
  public void setFloat(@Nonnull Object host, float value) {
    set(host, value);
  }

  @Override
  public <T extends Annotation> T getAnnotation(@Nonnull Class<T> annotationClass) {
    T annotation = myReadMethod.getAnnotation(annotationClass);
    if (annotation == null) annotation = myWriteMethod.getAnnotation(annotationClass);
    return annotation;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Class<?> getValueClass() {
    return myType;
  }

  @Override
  public Type getGenericType() {
    return myGenericType;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public String toString() {
    return "PropertyAccessor[" + myReadMethod.getDeclaringClass().getName() + "." + getName() +"]";
  }
}
