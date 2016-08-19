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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 17:33/15.06.13
 *
 * Main idea was get from ModulePointerManagerImpl by <b>nik</b>
 */
public abstract class NamedPointerManagerImpl<T extends Named> implements NamedPointerManager<T> {
  private final Map<String, NamedPointerImpl<T>> myUnresolved = new HashMap<String, NamedPointerImpl<T>>();
  private final Map<T, NamedPointerImpl<T>> myPointers = new HashMap<T, NamedPointerImpl<T>>();

  @Nullable
  public abstract T findByName(@NotNull String name);

  protected void updatePointers(T value) {
    updatePointers(value, value.getName());
  }

  protected void updatePointers(T value, @NotNull String name) {
    NamedPointerImpl<T> pointer = myUnresolved.remove(name);
    if (pointer != null && pointer.get() == null) {
      pointer.setValue(value);
      registerPointer(value, pointer);
    }
  }

  protected void registerPointer(final T value, final NamedPointerImpl<T> pointer) {
    myPointers.put(value, pointer);
  }

  protected void unregisterPointers(List<? extends T> value) {
    for (T t : value) {
      unregisterPointer(t);
    }
  }

  protected void unregisterPointer(T value) {
    final NamedPointerImpl<T> pointer = myPointers.remove(value);
    if (pointer != null) {
      pointer.dropValue(value);
      myUnresolved.put(pointer.getName(), pointer);
    }
  }

  @NotNull
  @Override
  public NamedPointer<T> create(@NotNull T value) {
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

  @NotNull
  @Override
  public NamedPointer<T> create(@NotNull String name) {
    final T value = findByName(name);
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
    return new NamedPointerImpl<T>(name);
  }

  public NamedPointerImpl<T> createImpl(T t) {
    return new NamedPointerImpl<T>(t);
  }
}
