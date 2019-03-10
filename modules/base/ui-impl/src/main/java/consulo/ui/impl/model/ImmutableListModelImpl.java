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
package consulo.ui.impl.model;

import consulo.ui.model.ListModel;

import javax.annotation.Nonnull;

import java.util.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class ImmutableListModelImpl<E> implements ListModel<E> {
  protected final List<E> myItems = new ArrayList<>();

  public ImmutableListModelImpl(Collection<? extends E> items) {
    myItems.addAll(items);
  }

  @Nonnull
  @Override
  public E get(int index) {
    return myItems.get(index);
  }

  @Override
  public int indexOf(@Nonnull E value) {
    return myItems.indexOf(value);
  }

  @Override
  public int getSize() {
    return myItems.size();
  }

  @Override
  public Iterator<E> iterator() {
    return myItems.iterator();
  }
}
