/*
 * Copyright 2013-2016 consulo.io
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
package consulo.component.util.pointer;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2013-06-15
 */
public class NamedPointerImpl<T extends Named> implements NamedPointer<T> {
  @Nullable
  private T myValue;

  private final String myName;

  public NamedPointerImpl(T value) {
    myValue = value;
    myName = value.getName();
  }

  public NamedPointerImpl(String name) {
    myValue = null;
    myName = name;
  }

  public void setValue(T value) {
    if (myValue != null) {
      throw new IllegalStateException("Value is already set");
    }
    if (!myName.equals(value.getName())) {
      throw new IllegalStateException("Value name \"" + value.getName() + "\" is not same as our \"" + myName + '"');
    }
    myValue = value;
  }

  public void dropValue(T value) {
    if (myValue != value) {
      throw new IllegalStateException("Value is different");
    }
    myValue = null;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public T get() {
    return myValue;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NamedPointerImpl that = (NamedPointerImpl)o;

    return myName.equals(that.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
