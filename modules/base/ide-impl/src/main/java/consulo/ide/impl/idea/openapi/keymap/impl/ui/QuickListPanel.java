/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.application.AllIcons;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ui.ex.awt.AnActionButton;
import consulo.ui.ex.awt.AnActionButtonRunnable;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.keymap.KeyMapBundle;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class QuickListPanel {
  private JPanel myPanel;
  private final JBList myActionsList;
  private JTextField myDisplayName;
  private JTextField myDescription;
  private JPanel myListPanel;
  private final QuickList[] myAllQuickLists;

  public QuickListPanel(@Nonnull QuickList origin, @Nonnull QuickList[] allQuickLists) {
    myAllQuickLists = allQuickLists;

    myActionsList = new JBList(new DefaultListModel());
    myActionsList.setCellRenderer(new MyListCellRenderer());
    myActionsList.getEmptyText().setText(KeyMapLocalize.noActions());
    myActionsList.setEnabled(true);

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        excludeSelectionAction();
        return true;
      }
    }.installOn(myActionsList);

    myListPanel.add(
            ToolbarDecorator.createDecorator(myActionsList)
                    .setAddAction(new AnActionButtonRunnable() {
                      @Override
                      public void run(AnActionButton button) {
                        includeSelectedAction();
                      }
                    }).addExtraAction(new AnActionButton("Add Separator", PlatformIconGroup.generalRemove()) {
              @Override
              public void actionPerformed(@Nullable AnActionEvent e) {
                addSeparator();
              }
            }).setButtonComparator("Add", "Add Separator", "Remove", "Up", "Down").createPanel(), BorderLayout.CENTER);

    myDisplayName.setText(origin.getName());
    myDescription.setText(origin.getDescription());

    String[] ids = origin.getActionIds();
    for (String id : ids) {
      includeActionId(id);
    }
  }

  public void addNameListener(DocumentAdapter adapter) {
    myDisplayName.getDocument().addDocumentListener(adapter);
  }

  public void addDescriptionListener(final DocumentAdapter adapter) {
    myDescription.getDocument().addDocumentListener(adapter);
  }

  private void excludeSelectionAction() {
    int[] ids = myActionsList.getSelectedIndices();
    for (int i = ids.length - 1; i >= 0; i--) {
      ((DefaultListModel)myActionsList.getModel()).remove(ids[i]);
    }
  }

  private void includeSelectedAction() {
    final ChooseActionsDialog dlg = new ChooseActionsDialog(myActionsList, KeymapManager.getInstance().getActiveKeymap(), myAllQuickLists);
    if (dlg.showAndGet()) {
      String[] ids = dlg.getTreeSelectedActionIds();
      for (String id : ids) {
        includeActionId(id);
      }
      DefaultListModel listModel = (DefaultListModel)myActionsList.getModel();
      int size = listModel.getSize();
      ListSelectionModel selectionModel = myActionsList.getSelectionModel();
      if (size > 0) {
        selectionModel.removeIndexInterval(0, size - 1);
      }
      for (String id1 : ids) {
        int idx = listModel.lastIndexOf(id1);
        if (idx >= 0) {
          selectionModel.addSelectionInterval(idx, idx);
        }
      }
    }
  }

  private void addSeparator() {
    ((DefaultListModel)myActionsList.getModel()).addElement(QuickList.SEPARATOR_ID);
  }

  public JList getActionsList() {
    return myActionsList;
  }

  public String getDescription() {
    return myDescription.getText();
  }

  public String getDisplayName() {
    return myDisplayName.getText();
  }

  private void includeActionId(String id) {
    DefaultListModel model = (DefaultListModel)myActionsList.getModel();
    if (!QuickList.SEPARATOR_ID.equals(id) && model.contains(id)) {
      return;
    }
    model.addElement(id);
  }

  public JPanel getPanel() {
    return myPanel;
  }

  private static class MyListCellRenderer extends DefaultListCellRenderer {
    @Nonnull
    @Override
    public Component getListCellRendererComponent(@Nonnull JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      Image icon = null;
      String actionId = (String)value;
      if (QuickList.SEPARATOR_ID.equals(actionId)) {
        // TODO[vova,anton]: beautify
        setText("-------------");
      }
      else {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        setText(action != null ? action.getTemplatePresentation().getText() : actionId);
        if (action != null) {
          Image actionIcon = action.getTemplatePresentation().getIcon();
          if (actionIcon != null) {
            icon = actionIcon;
          }
        }
        if (actionId.startsWith(QuickList.QUICK_LIST_PREFIX)) {
          icon = AllIcons.Actions.QuickList;
        }
        setIcon(TargetAWT.to(ActionsTree.getEvenIcon(icon)));
      }

      return this;
    }
  }
}
