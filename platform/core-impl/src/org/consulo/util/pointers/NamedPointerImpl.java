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
package org.consulo.util.pointers;

import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17:22/15.06.13
 */
@Logger
public class NamedPointerImpl<T extends Named> implements NamedPointer<T> {
  private T myValue;
  @NotNull
  private String myModuleName;

  public NamedPointerImpl(T value) {
    myValue = value;
    myModuleName = value.getName();
  }

  public NamedPointerImpl(@NotNull String name) {
    myValue = null;
    myModuleName = name;
  }

  public void setValue(@NotNull T module) {
    LOGGER.assertTrue(myValue == null);
    LOGGER.assertTrue(myModuleName.equals(module.getName()));
    myModuleName = module.getName();
    myValue = module;
  }

  public void dropValue(@NotNull T module) {
    LOGGER.assertTrue(myValue == module);
    myModuleName = myValue.getName();
    myValue = null;
  }

  @NotNull
  @Override
  public String getName() {
    if (myValue != null) {
      return myValue.getName();
    }
    else {
      return myModuleName;
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

    if (!myModuleName.equals(that.myModuleName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myModuleName.hashCode();
  }
}
