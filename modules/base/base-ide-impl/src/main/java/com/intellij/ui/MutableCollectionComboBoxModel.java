/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author traff
 */
public class MutableCollectionComboBoxModel<T> extends AbstractCollectionComboBoxModel<T> {
  private List<T> myItems;

  public MutableCollectionComboBoxModel(@Nonnull List<T> items) {
    this(items, ContainerUtil.getFirstItem(items));
  }

  public MutableCollectionComboBoxModel(@Nonnull List<T> items, @Nullable T selection) {
    super(selection);

    myItems = items;
  }

  @Nonnull
  @Override
  final protected List<T> getItems() {
    return myItems;
  }

  public void update(@Nonnull List<T> items) {
    myItems = items;
    super.update();
  }

  public void addItem(T item) {
    myItems.add(item);

    fireIntervalAdded(this, myItems.size() - 1, myItems.size() - 1);
    if (myItems.size() == 1 && getSelectedItem() == null && item != null) {
      setSelectedItem(item);
    }
  }
}
