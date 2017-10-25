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

import consulo.ui.model.ImmutableListModel;
import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface ComboBox<E> extends ValueComponent<E> {
  @NotNull
  @SafeVarargs
  static <E> ComboBox<E> create(@NotNull E... elements) {
    return UIInternal.get()._Components_comboBox(new ImmutableListModel<E>(elements));
  }

  @NotNull
  static <E> ComboBox<E> create(@NotNull ListModel<E> model) {
    return UIInternal.get()._Components_comboBox(model);
  }

  static class SimpleBuilder<K> {
    private Map<K, String> myValues = new LinkedHashMap<>();

    public SimpleBuilder<K> add(K key, String value) {
      myValues.put(key, value);
      return this;
    }

    public SimpleBuilder<K> fillByEnum(Class<? extends K> clazz, Function<K, String> presentation) {
      if (!clazz.isEnum()) {
        throw new IllegalArgumentException("Accepted only enum");
      }
      K[] enumConstants = clazz.getEnumConstants();
      for (K enumConstant : enumConstants) {
        add(enumConstant, presentation.apply(enumConstant));
      }
      return this;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public ComboBox<K> build() {
      Object[] objects = myValues.keySet().toArray();
      ComboBox comboBox = ComboBox.create(objects);
      comboBox.setRender((render, index, item) -> render.append(myValues.get(item)));
      return comboBox;
    }
  }

  @NotNull
  static <K> SimpleBuilder<K> builder() {
    return new SimpleBuilder<>();
  }

  @NotNull
  ListModel<E> getListModel();

  void setRender(@NotNull ListItemRender<E> render);

  void setValueByIndex(int index);
}
