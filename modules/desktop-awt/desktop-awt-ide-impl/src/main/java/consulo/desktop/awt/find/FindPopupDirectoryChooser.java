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
import consulo.application.Application;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.find.FindBundle;
import consulo.find.FindInProjectSettings;
import consulo.find.FindModel;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.find.impl.FindUIHelper;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awt.internal.ComboBoxStyle;
import consulo.ui.ex.awt.internal.HasSuffixComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class FindPopupDirectoryChooser {
    @Nonnull
    private final FindUIHelper myHelper;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final FindPopupPanel myFindPopupPanel;
    @Nonnull
    private final ComboBox<String> myDirectoryComboBox;

    @RequiredUIAccess
    public FindPopupDirectoryChooser(@Nonnull FindPopupPanel panel) {
        myHelper = panel.getHelper();
        myProject = panel.getProject();
        myFindPopupPanel = panel;
        myDirectoryComboBox = new ComboBox<>(200);
        ComboBoxStyle.makeBorderInline(myDirectoryComboBox);

        Component editorComponent = myDirectoryComboBox.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField) {
            JTextField field = (JTextField) editorComponent;
            field.setColumns(40);
        }
        myDirectoryComboBox.setEditable(true);
        myDirectoryComboBox.setMaximumRowCount(8);

        ActionListener restartSearchListener = e -> myFindPopupPanel.scheduleResultsUpdate();
        myDirectoryComboBox.addActionListener(restartSearchListener);

        DumbAwareAction selectPathAction = DumbAwareAction.create("Select Path", PlatformIconGroup.nodesFolder(), e -> {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            descriptor.setUseApplicationDialog();
            myFindPopupPanel.getCanClose().set(false);
            IdeaFileChooser.chooseFiles(
                descriptor,
                myProject,
                null,
                VfsUtil.findFileByIoFile(new File(getDirectory()), true),
                new IdeaFileChooser.FileChooserConsumer() {
                    @Override
                    public void accept(List<VirtualFile> files) {
                        Application.get().invokeLater(() -> {
                            myFindPopupPanel.getCanClose().set(true);
                            ProjectIdeFocusManager.getInstance(myProject)
                                .requestFocus(myDirectoryComboBox.getEditor().getEditorComponent(), true);
                            myHelper.getModel().setDirectoryName(files.get(0).getPresentableUrl());
                            myDirectoryComboBox.getEditor().setItem(files.get(0).getPresentableUrl());
                        });
                    }

                    @Override
                    public void cancelled() {
                        Application.get().invokeLater(() -> {
                            myFindPopupPanel.getCanClose().set(true);
                            ProjectIdeFocusManager.getInstance(myProject)
                                .requestFocus(myDirectoryComboBox.getEditor().getEditorComponent(), true);
                        });
                    }
                }
            );
        });

        selectPathAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)),
            myFindPopupPanel);

        MyRecursiveDirectoryAction recursiveDirectoryAction = new MyRecursiveDirectoryAction();
        int mnemonicModifiers =
            Platform.current().os().isMac() ? InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK : InputEvent.ALT_DOWN_MASK;

        recursiveDirectoryAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mnemonicModifiers)),
            myFindPopupPanel
        );

        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder().add(selectPathAction).add(recursiveDirectoryAction);

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar("FindPopupDirectoryBox", builder.build(), ActionToolbar.Style.INPLACE);

        toolbar.setTargetComponent(myDirectoryComboBox);

        toolbar.updateActionsAsync();

        toolbar.getComponent().setBorder(JBCurrentTheme.comboBoxSubBorder(true));

        HasSuffixComponent.setSuffixComponent(myDirectoryComboBox, toolbar.getComponent());
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
        return (String) myDirectoryComboBox.getSelectedItem();
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
            super(FindLocalize.findScopeDirectoryRecursiveCheckbox(), LocalizeValue.localizeTODO("Recursively"), AllIcons.Actions.ShowAsTree);
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myHelper.getModel().isWithSubdirectories();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myHelper.getModel().setWithSubdirectories(state);
            myFindPopupPanel.scheduleResultsUpdate();
        }
    }
}
