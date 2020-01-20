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
package consulo.ui.model;

import com.intellij.openapi.Disposable;
import consulo.ui.UIInternal;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-15
 */
public interface MutableListModel<E> extends ListModel<E> {
  @Nonnull
  static <T> MutableListModel<T> create(@Nonnull Collection<? extends T> items) {
    return UIInternal.get()._MutableListModel_create(items);
  }

  @RequiredUIAccess
  default void add(@Nonnull E e) {
    add(e, getSize());
  }

  @RequiredUIAccess
  void add(@Nonnull E e, int index);

  @RequiredUIAccess
  void remove(@Nonnull E e);

  /**
   * @param newItems
   * @return oldItems
   */
  @RequiredUIAccess
  List<E> replaceAll(@Nonnull Iterable<E> newItems);

  Disposable adddListener(@Nonnull MutableListModelListener<E> modelListener);
}
