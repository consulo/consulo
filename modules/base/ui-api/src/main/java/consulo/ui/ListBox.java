/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui;

import consulo.ui.internal.UIInternal;
import consulo.ui.model.ListModel;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public interface ListBox<E> extends ValueComponent<E> {
  @SafeVarargs
  @Nonnull
  static <E> ListBox<E> create(@Nonnull E... elements) {
    return UIInternal.get()._Components_listBox(ListModel.create(Arrays.asList(elements)));
  }

  @Nonnull
  static <E> ListBox<E> create(@Nonnull Collection<E> elements) {
    return UIInternal.get()._Components_listBox(ListModel.create(elements));
  }

  @Nonnull
  static <E> ListBox<E> create(@Nonnull ListModel<E> model) {
    return UIInternal.get()._Components_listBox(model);
  }

  @Nonnull
  ListModel<E> getListModel();

  void setRender(@Nonnull TextItemRender<E> render);

  void setValueByIndex(int index);
}
