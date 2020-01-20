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
package consulo.ui.desktop.internal;

import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.model.MutableListModelListener;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopComboBoxModelWrapper<E> extends AbstractListModel<E> implements ComboBoxModel<E> {
  private ListModel<E> myModel;
  private Object mySelectedItem;

  public DesktopComboBoxModelWrapper(ListModel<E> model) {
    myModel = model;

    if (model instanceof MutableListModel) {
      ((MutableListModel)model).adddListener(new MutableListModelListener() {
        @Override
        public void itemAdded(@Nonnull Object item) {
          fireContentsChanged(this, -1, -1);
        }

        @Override
        public void itemRemoved(@Nonnull Object item) {
          fireContentsChanged(this, -1, -1);
        }
      });
    }
  }

  @Override
  public int getSize() {
    return myModel.getSize();
  }

  @Override
  public E getElementAt(int index) {
    return myModel.get(index);
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
