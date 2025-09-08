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

import consulo.ui.ex.awt.dnd.RowsDnDSupport;
import consulo.ui.ex.awt.util.ListUtil;

import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
class ListToolbarDecorator extends ToolbarDecorator {
  private final JList myList;
  private EditableModel myEditableModel;

  ListToolbarDecorator(JList list, @Nullable EditableModel editableModel) {
    myList = list;
    myEditableModel = editableModel;
    myAddActionEnabled = myRemoveActionEnabled = myUpActionEnabled = myDownActionEnabled = true;
    createActions();
    myList.addListSelectionListener(e -> updateButtons());
    myList.addPropertyChangeListener("enabled", evt -> updateButtons());
  }

  private void createActions() {
    myRemoveAction = (button, e) -> {
      ListUtil.removeSelectedItems(myList);
      updateButtons();
    };
    myUpAction = (button, e) -> {
      ListUtil.moveSelectedItemsUp(myList);
      updateButtons();
    };
    myDownAction = (button, e) -> {
      ListUtil.moveSelectedItemsDown(myList);
      updateButtons();
    };
  }

  @Override
  protected JComponent getComponent() {
    return myList;
  }

  @Override
  protected void updateButtons() {
    CommonActionsPanel p = getActionsPanel();
    if (p != null) {
      if (myList.isEnabled()) {
        int index = myList.getSelectedIndex();
        if (0 <= index && index < myList.getModel().getSize()) {
          boolean downEnable = myList.getMaxSelectionIndex() < myList.getModel().getSize() - 1;
          boolean upEnable = myList.getMinSelectionIndex() > 0;
          boolean editEnabled = myList.getSelectedIndices().length == 1;
          p.setEnabled(CommonActionsPanel.Buttons.EDIT, editEnabled);
          p.setEnabled(CommonActionsPanel.Buttons.REMOVE, true);
          p.setEnabled(CommonActionsPanel.Buttons.UP, upEnable);
          p.setEnabled(CommonActionsPanel.Buttons.DOWN, downEnable);
        }
        else {
          p.setEnabled(CommonActionsPanel.Buttons.EDIT, false);
          p.setEnabled(CommonActionsPanel.Buttons.REMOVE, false);
          p.setEnabled(CommonActionsPanel.Buttons.UP, false);
          p.setEnabled(CommonActionsPanel.Buttons.DOWN, false);
        }
        p.setEnabled(CommonActionsPanel.Buttons.ADD, true);
      }
      else {
        p.setEnabled(CommonActionsPanel.Buttons.ADD, false);
        p.setEnabled(CommonActionsPanel.Buttons.REMOVE, false);
        p.setEnabled(CommonActionsPanel.Buttons.UP, false);
        p.setEnabled(CommonActionsPanel.Buttons.DOWN, false);
      }
    }
  }

  @Override
  public ToolbarDecorator setVisibleRowCount(int rowCount) {
    myList.setVisibleRowCount(rowCount);
    return this;
  }

  @Override
  protected boolean isModelEditable() {
    return myEditableModel != null || myList.getModel() instanceof EditableModel;
  }

  @Override
  protected void installDnDSupport() {
    RowsDnDSupport.install(myList, myEditableModel != null ? myEditableModel : (EditableModel)myList.getModel());
  }
}
