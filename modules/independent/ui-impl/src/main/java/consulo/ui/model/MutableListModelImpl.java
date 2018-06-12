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
package consulo.ui.model;

import com.intellij.openapi.Disposable;
import com.intellij.util.EventDispatcher;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class MutableListModelImpl<E> extends ImmutableListModelImpl<E> implements MutableListModel<E> {
  private EventDispatcher<MutableListModelListener> myDispatcher = EventDispatcher.create(MutableListModelListener.class);

  public MutableListModelImpl(Collection<? extends E> items) {
    super(items);
  }

  @Override
  public void add(@Nonnull E item) {
    myItems.add(item);

    myDispatcher.getMulticaster().itemAdded(item);
  }

  @Override
  public void remove(@Nonnull E item) {
    myItems.remove(item);

    myDispatcher.getMulticaster().itemRemove(item);
  }

  @Override
  public Disposable adddListener(@Nonnull MutableListModelListener<E> modelListener) {
    myDispatcher.addListener(modelListener);
    return () -> myDispatcher.removeListener(modelListener);
  }
}
