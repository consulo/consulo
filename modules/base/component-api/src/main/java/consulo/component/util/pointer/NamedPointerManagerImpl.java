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

import consulo.annotation.ReviewAfterIssueFix;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2013-06-15
 *
 * Main idea was get from ModulePointerManagerImpl by <b>nik</b>
 */
public abstract class NamedPointerManagerImpl<T extends Named> implements NamedPointerManager<T> {
  private final Map<String, NamedPointerImpl<T>> myUnresolved = new HashMap<>();
  private final Map<T, NamedPointerImpl<T>> myPointers = new HashMap<>();

  protected abstract @Nullable T findByName(String name);

  protected void updatePointers(T value) {
    updatePointers(value, value.getName());
  }

  protected void updatePointers(T value, String name) {
    NamedPointerImpl<T> pointer = myUnresolved.remove(name);
    if (pointer != null && pointer.get() == null) {
      pointer.setValue(value);
      registerPointer(value, pointer);
    }
  }

  protected void registerPointer(T value, NamedPointerImpl<T> pointer) {
    myPointers.put(value, pointer);
  }

  protected void unregisterPointers(List<? extends T> value) {
    for (T t : value) {
      unregisterPointer(t);
    }
  }

  protected void unregisterPointer(T value) {
    NamedPointerImpl<T> pointer = myPointers.remove(value);
    if (pointer != null) {
      pointer.dropValue(value);
      myUnresolved.put(pointer.getName(), pointer);
    }
  }

  @Override
  public NamedPointer<T> create(T value) {
    NamedPointerImpl<T> pointer = myPointers.get(value);
    if (pointer == null) {
      pointer = myUnresolved.get(value.getName());
      if (pointer == null) {
        pointer = createImpl(value);
      }
      else {
        pointer.setValue(value);
      }
      registerPointer(value, pointer);
    }
    return pointer;
  }

  @Override
  @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1500", todo = "Remove explicit casts")
  public NamedPointer<T> create(String name) {
    return create(name, (Function<String, @Nullable T>) this::findByName);
  }

  public NamedPointer<T> create(String name, Function<String, @Nullable T> findByNameFunc) {
    T value = findByNameFunc.apply(name);
    if (value != null) {
      return create(value);
    }

    NamedPointerImpl<T> pointer = myUnresolved.get(name);
    if (pointer == null) {
      pointer = createImpl(name);
      myUnresolved.put(name, pointer);
    }
    return pointer;
  }

  public NamedPointerImpl<T> createImpl(String name) {
    return new NamedPointerImpl<>(name);
  }

  public NamedPointerImpl<T> createImpl(T t) {
    return new NamedPointerImpl<>(t);
  }
}
