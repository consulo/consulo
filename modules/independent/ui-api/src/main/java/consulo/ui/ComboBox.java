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
package consulo.ui;

import consulo.ui.model.ListModel;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface ComboBox<E> extends ValueComponent<E> {
  @Nonnull
  @SafeVarargs
  static <E> ComboBox<E> create(@Nonnull E... elements) {
    return UIInternal.get()._Components_comboBox(ListModel.immutable(Arrays.asList(elements)));
  }

  @Nonnull
  static <E> ComboBox<E> create(@Nonnull ListModel<E> model) {
    return UIInternal.get()._Components_comboBox(model);
  }

  static class Builder<K> {
    private Map<K, String> myValues = new LinkedHashMap<>();

    public Builder<K> add(K key, String value) {
      myValues.put(key, value);
      return this;
    }

    public Builder<K> fillByEnum(Class<? extends K> clazz, Function<K, String> presentation) {
      if (!clazz.isEnum()) {
        throw new IllegalArgumentException("Accepted only enum");
      }
      K[] enumConstants = clazz.getEnumConstants();
      for (K enumConstant : enumConstants) {
        add(enumConstant, presentation.apply(enumConstant));
      }
      return this;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public ComboBox<K> build() {
      K[] objects = (K[])myValues.keySet().toArray();
      ComboBox<K> comboBox = ComboBox.create(objects);
      comboBox.setRender((render, index, item) -> render.append(item == null ? "" : myValues.get(item)));
      return comboBox;
    }
  }

  @Nonnull
  static <K> Builder<K> builder() {
    return new Builder<>();
  }

  @Nonnull
  ListModel<E> getListModel();

  void setRender(@Nonnull ListItemRender<E> render);

  void setValueByIndex(int index);
}
