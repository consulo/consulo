/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.util.lang.ref;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author ven
 */
public class SimpleReference<T> implements Supplier<T> {
  private T myValue;

  public SimpleReference() {
  }

  public SimpleReference(@Nullable T value) {
    myValue = value;
  }

  public boolean isNull() {
    return myValue == null;
  }

  @Override
  public T get() {
    return myValue;
  }

  public void set(@Nullable T value) {
    myValue = value;
  }

  public boolean setIfNull(@Nullable T value) {
    if (myValue == null) {
      myValue = value;
      return true;
    }
    return false;
  }

  public static <T> SimpleReference<T> create() {
    return new SimpleReference<>();
  }

  public static <T> SimpleReference<T> create(@Nullable T value) {
    return new SimpleReference<>(value);
  }

  @Override
  public String toString() {
    return String.valueOf(myValue);
  }
}
