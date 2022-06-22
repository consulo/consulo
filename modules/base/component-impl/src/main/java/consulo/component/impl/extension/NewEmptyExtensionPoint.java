/*
 * Copyright 2013-2022 consulo.io
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
package consulo.component.impl.extension;

import consulo.component.extension.ExtensionPoint;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-Jun-22
 */
@SuppressWarnings("unchecked")
public class NewEmptyExtensionPoint<T> implements ExtensionPoint<T> {
  private final Class<T> myClass;

  public NewEmptyExtensionPoint(Class<T> aClass) {
    myClass = aClass;
  }

  @Override
  public boolean hasAnyExtensions() {
    return false;
  }

  @Nonnull
  @Override
  public T[] getExtensions() {
    return (T[])Array.newInstance(myClass, 0);
  }

  @Nonnull
  @Override
  public String getName() {
    return "";
  }

  @Nonnull
  @Override
  public List<T> getExtensionList() {
    return List.of();
  }

  @Nonnull
  @Override
  public Class<T> getExtensionClass() {
    return myClass;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myClass.isInterface() ? Kind.INTERFACE : Kind.BEAN_CLASS;
  }
}