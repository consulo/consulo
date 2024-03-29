/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class CollectionComboBoxModel<T> extends AbstractCollectionComboBoxModel<T> {
  private final List<T> myItems;

  public CollectionComboBoxModel(final List<? extends T> items, @Nullable final T selection) {
    super(selection);
    myItems = Collections.unmodifiableList(items);
  }

  public CollectionComboBoxModel(List<T> items) {
    super(items.isEmpty() ? null : items.get(0));
    myItems = items;
  }

  @Override
  @Nonnull
  final protected List<T> getItems() {
    return myItems;
  }
}
