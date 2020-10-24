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
package consulo.ui;

import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
public interface TableColumn<Value, Item> {
  class Builder<Value, Item> {
    @Nonnull
    private final String myName;
    private final Function<Item, Value> myConverter;

    public Builder(@Nonnull String name, @Nonnull Function<Item, Value> converter) {
      myName = name;
      myConverter = converter;
    }

    public TableColumn<Value, Item> build() {
      return UIInternal.get()._Components_tableColumBuild(myName, myConverter);
    }
  }

  @Nonnull
  static <Value1, Item1> Builder<Value1, Item1> create(@Nonnull String name, @Nonnull Function<Item1, Value1> converter) {
    return new Builder<>(name, converter);
  }
}
