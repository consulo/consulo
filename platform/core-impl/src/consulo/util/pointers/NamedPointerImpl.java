/*
 * Copyright 2013 must-be.org
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
package consulo.util.pointers;

import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17:22/15.06.13
 */
@Logger
public class NamedPointerImpl<T extends Named> implements NamedPointer<T> {
  private T myValue;
  @NotNull
  private String myName;

  public NamedPointerImpl(T value) {
    myValue = value;
    myName = value.getName();
  }

  public NamedPointerImpl(@NotNull String name) {
    myValue = null;
    myName = name;
  }

  public void setValue(@NotNull T value) {
    LOGGER.assertTrue(myValue == null);
    LOGGER.assertTrue(myName.equals(value.getName()));
    myName = value.getName();
    myValue = value;
  }

  public void dropValue(@NotNull T value) {
    LOGGER.assertTrue(myValue == value);
    myName = myValue.getName();
    myValue = null;
  }

  @NotNull
  @Override
  public String getName() {
    if (myValue != null) {
      return myValue.getName();
    }
    else {
      return myName;
    }
  }

  @Override
  public T get() {
    return myValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NamedPointerImpl that = (NamedPointerImpl)o;

    if (!myName.equals(that.myName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
