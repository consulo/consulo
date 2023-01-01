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
package consulo.disposer.util;

import consulo.disposer.Disposable;
import consulo.disposer.internal.DisposerInternal;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Similar to a list returned by {@link ContainerUtil#createLockFreeCopyOnWriteList} with additional methods that allow
 * some elements of the list to be associated with {@link Disposable} objects. When these disposable objects are disposed,
 * the associated elements are removed from the list.
 *
 * @param <E> the type of elements held in this list
 */
public interface DisposableList<E> extends List<E> {
  @Nonnull
  static <T> DisposableList<T> create() {
    return DisposerInternal.ourInstance.createList();
  }

  @Nonnull
  Disposable add(E element, @Nonnull Disposable parentDisposable);

  @Nonnull
  Disposable add(int index, E element, @Nonnull Disposable parentDisposable);
}
