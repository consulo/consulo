/*
 * Copyright 2013-2020 consulo.io
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

import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
public interface TableModel<E> {
  @Nonnull
  static <T> TableModel<T> of(@Nonnull Collection<? extends T> items) {
    return UIInternal.get()._TableModel_create(items);
  }

  int getSize();

  @Nonnull
  E get(int index);
}
