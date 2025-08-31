/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.ui.ex.awt.event.DoubleClickListener;

import jakarta.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author peter
 * @author Konstantin Bulenkov
 */
public abstract class AddEditDeleteListPanel<T> extends AddDeleteListPanel<T> {
  public AddEditDeleteListPanel(String title, List<T> initialList) {
    super(title, initialList);
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent event) {
        editSelectedItem();
        return true;
      }
    }.installOn(myList);
  }

  @Override
  protected void customizeDecorator(ToolbarDecorator decorator) {
    decorator.setEditAction(b -> editSelectedItem());
  }

  @Nullable
  protected abstract T editSelectedItem(T item);

  private void editSelectedItem() {
    int index = myList.getSelectedIndex();
    if (index >= 0) {
      T newValue = editSelectedItem((T) myListModel.get(index));
      if (newValue != null) {
        myListModel.set(index, newValue);
      }
    }
  }
}
