/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.find;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.find.FindBundle;
import consulo.find.FindInProjectSettings;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.find.impl.FindUIHelper;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class FindPopupDirectoryChooser extends JPanel {
  @Nonnull
  private final FindUIHelper myHelper;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final FindPopupPanel myFindPopupPanel;
  @Nonnull
  private final ComboBox<String> myDirectoryComboBox;

  public FindPopupDirectoryChooser(@Nonnull FindPopupPanel panel) {
    super(new BorderLayout());

    myHelper = panel.getHelper();
    myProject = panel.getProject();
    myFindPopupPanel = panel;
    myDirectoryComboBox = new ComboBox<>(200);

    Component editorComponent = myDirectoryComboBox.getEditor().getEditorComponent();
    if (editorComponent instanceof JTextField) {
      JTextField field = (JTextField)editorComponent;
      field.setColumns(40);
    }
    myDirectoryComboBox.setEditable(true);
    myDirectoryComboBox.setMaximumRowCount(8);

    ActionListener restartSearchListener = e -> myFindPopupPanel.scheduleResultsUpdate();
    myDirectoryComboBox.addActionListener(restartSearchListener);

    FixedSizeButton mySelectDirectoryButton = new FixedSizeButton(myDirectoryComboBox);
    TextFieldWithBrowseButton.MyDoClickAction.addTo(mySelectDirectoryButton, myDirectoryComboBox);
    mySelectDirectoryButton.setMargin(JBUI.emptyInsets());

    mySelectDirectoryButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setUseApplicationDialog();
        myFindPopupPanel.getCanClose().set(false);
        IdeaFileChooser.chooseFiles(descriptor, myProject, null, VfsUtil.findFileByIoFile(new File(getDirectory()), true), new IdeaFileChooser.FileChooserConsumer() {
          @Override
          public void accept(List<VirtualFile> files) {
            ApplicationManager.getApplication().invokeLater(() -> {
              myFindPopupPanel.getCanClose().set(true);
              ProjectIdeFocusManager.getInstance(myProject).requestFocus(myDirectoryComboBox.getEditor().getEditorComponent(), true);
              myHelper.getModel().setDirectoryName(files.get(0).getPresentableUrl());
              myDirectoryComboBox.getEditor().setItem(files.get(0).getPresentableUrl());
            });
          }

          @Override
          public void cancelled() {
            ApplicationManager.getApplication().invokeLater(() -> {
              myFindPopupPanel.getCanClose().set(true);
              ProjectIdeFocusManager.getInstance(myProject).requestFocus(myDirectoryComboBox.getEditor().getEditorComponent(), true);
            });
          }
        });
      }
    });

    MyRecursiveDirectoryAction recursiveDirectoryAction = new MyRecursiveDirectoryAction();
    int mnemonicModifiers = Platform.current().os().isMac() ? InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK : InputEvent.ALT_DOWN_MASK;
    recursiveDirectoryAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mnemonicModifiers)), myFindPopupPanel);

    add(myDirectoryComboBox, BorderLayout.CENTER);
    JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));
    buttonsPanel.add(mySelectDirectoryButton);
    ActionToolbar toolbar = FindPopupPanel.createToolbar(recursiveDirectoryAction);
    toolbar.setTargetComponent(this);
    buttonsPanel.add(toolbar.getComponent()); //check if toolbar updates the button with no delays
    add(buttonsPanel, BorderLayout.EAST);
  }

  public void initByModel(@Nonnull FindModel findModel) {
    final String directoryName = findModel.getDirectoryName();
    java.util.List<String> strings = FindInProjectSettings.getInstance(myProject).getRecentDirectories();

    if (myDirectoryComboBox.getItemCount() > 0) {
      myDirectoryComboBox.removeAllItems();
    }
    if (directoryName != null && !directoryName.isEmpty()) {
      if (strings.contains(directoryName)) {
        strings.remove(directoryName);
      }
      myDirectoryComboBox.addItem(directoryName);
    }
    for (int i = strings.size() - 1; i >= 0; i--) {
      myDirectoryComboBox.addItem(strings.get(i));
    }
    if (myDirectoryComboBox.getItemCount() == 0) {
      myDirectoryComboBox.addItem("");
    }
  }

  @Nonnull
  public ComboBox getComboBox() {
    return myDirectoryComboBox;
  }

  @Nonnull
  public String getDirectory() {
    return (String)myDirectoryComboBox.getSelectedItem();
  }

  @Nullable
  public ValidationInfo validate(@Nonnull FindModel model) {
    VirtualFile directory = FindInProjectUtil.getDirectory(model);
    if (directory == null) {
      return new ValidationInfo(FindBundle.message("find.directory.not.found.error", getDirectory()), myDirectoryComboBox);
    }
    return null;
  }

  private class MyRecursiveDirectoryAction extends ToggleAction {
    MyRecursiveDirectoryAction() {
      super(FindBundle.message("find.scope.directory.recursive.checkbox"), "Recursively", AllIcons.Actions.ShowAsTree);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myHelper.getModel().isWithSubdirectories();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myHelper.getModel().setWithSubdirectories(state);
      myFindPopupPanel.scheduleResultsUpdate();
    }
  }
}
