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
package consulo.ui.internal;

import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Iterator;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopComboBoxModelWrapper<E> extends AbstractListModel implements ListModel<E>, ComboBoxModel {
  private ListModel<E> myModel;
  private Object mySelectedItem;

  public DesktopComboBoxModelWrapper(ListModel<E> model) {
    myModel = model;
  }

  @NotNull
  @Override
  public E get(int index) {
    return myModel.get(index);
  }

  @Override
  public int indexOf(@NotNull E value) {
    return myModel.indexOf(value);
  }

  @Override
  public int getSize() {
    return myModel.getSize();
  }

  @Override
  public Object getElementAt(int index) {
    return myModel.get(index);
  }

  @Override
  public Iterator<E> iterator() {
    return myModel.iterator();
  }

  @Override
  public void setSelectedItem(Object anItem) {
    if (mySelectedItem != null && !mySelectedItem.equals(anItem) || mySelectedItem == null && anItem != null) {
      mySelectedItem = anItem;
      fireContentsChanged(this, -1, -1);
    }
  }

  @Override
  public Object getSelectedItem() {
    return mySelectedItem;
  }
}
