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
package consulo.fileEditor.impl.internal.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.fileEditor.impl.internal.NonProjectFileWritingAccessProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.util.FileListRenderer;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;

public class NonProjectFileWritingAccessDialog extends DialogWrapper {
    private JPanel myPanel;
    private JLabel myListTitle;
    private JList myFileList;
    private JRadioButton myUnlockOneButton;
    private JRadioButton myUnlockDirButton;
    private JRadioButton myUnlockAllButton;

    public NonProjectFileWritingAccessDialog(Project project, List<VirtualFile> nonProjectFiles) {
        this(project, nonProjectFiles, "Non-Project Files");
    }

    @RequiredUIAccess
    public NonProjectFileWritingAccessDialog(
        Project project,
        List<VirtualFile> nonProjectFiles,
        String filesType
    ) {
        super(project);
        setTitle(filesType + " Protection");

        initUI();

        myFileList.setPreferredSize(new Dimension(500, 400));

        myFileList.setCellRenderer(new FileListRenderer());
        myFileList.setModel(new CollectionListModel<>(nonProjectFiles));

        String theseFilesMessage = getTheseFilesMessage(nonProjectFiles);
        myListTitle.setText(
            StringUtil.capitalize(theseFilesMessage)
                + " " + (nonProjectFiles.size() > 1 ? "do" : "does")
                + " not belong to the project:"
        );

        myUnlockOneButton.setSelected(true);
        setTextAndMnemonicAndListeners(myUnlockOneButton, "I want to edit " + theseFilesMessage + " anyway", "edit");

        int dirs = ContainerUtil.map2Set(nonProjectFiles, VirtualFile::getParent).size();
        setTextAndMnemonicAndListeners(
            myUnlockDirButton,
            "I want to edit all files in " + StringUtil.pluralize("this", dirs) + " " + StringUtil.pluralize("directory", dirs),
            "dir"
        );

        setTextAndMnemonicAndListeners(myUnlockAllButton, "I want to edit any non-project file in the current session", "any");

        // disable default button to avoid accidental pressing, if user typed something, missed the dialog and pressed 'enter'.
        getOKAction().putValue(DEFAULT_ACTION, null);
        getCancelAction().putValue(DEFAULT_ACTION, null);

        getRootPane().registerKeyboardAction(
            e -> doOKAction(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
        getRootPane().registerKeyboardAction(
            e -> doOKAction(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        init();
    }

    
    public static String getTheseFilesMessage(Collection<VirtualFile> files) {
        boolean dirsOnly = true;
        for (VirtualFile each : files) {
            if (!each.isDirectory()) {
                dirsOnly = false;
                break;

            }
        }

        int size = files.size();
        return StringUtil.pluralize("this", size) + " " + StringUtil.pluralize((dirsOnly ? "directory" : "file"), size);
    }

    private void setTextAndMnemonicAndListeners(JRadioButton button, String text, String mnemonic) {
        button.setText(text);
        button.setMnemonic(mnemonic.charAt(0));
        button.setDisplayedMnemonicIndex(button.getText().indexOf(mnemonic));

        // enabled OK button when user selects an option
        button.addActionListener(e -> button.getRootPane().setDefaultButton(getButton(getOKAction())));
        button.addItemListener(e -> button.getRootPane().setDefaultButton(getButton(getOKAction())));
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myUnlockOneButton;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    
    public NonProjectFileWritingAccessProvider.UnlockOption getUnlockOption() {
        if (myUnlockAllButton.isSelected()) {
            return NonProjectFileWritingAccessProvider.UnlockOption.UNLOCK_ALL;
        }
        if (myUnlockDirButton.isSelected()) {
            return NonProjectFileWritingAccessProvider.UnlockOption.UNLOCK_DIR;
        }
        return NonProjectFileWritingAccessProvider.UnlockOption.UNLOCK;
    }

    protected String getHelpId() {
        return "Non-Project_Files_Access_Dialog";
    }

    private void initUI() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        myPanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        myUnlockOneButton = new JRadioButton();
        myUnlockOneButton.setSelected(false);
        myUnlockOneButton.setText("unlock one");
        panel1.add(myUnlockOneButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myUnlockAllButton = new JRadioButton();
        myUnlockAllButton.setText("unlock all");
        panel1.add(myUnlockAllButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myUnlockDirButton = new JRadioButton();
        myUnlockDirButton.setSelected(false);
        myUnlockDirButton.setText("unlock dir");
        panel1.add(myUnlockDirButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myListTitle = new JLabel();
        myListTitle.setText("title");
        myPanel.add(myListTitle, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBScrollPane jBScrollPane1 = new JBScrollPane();
        myPanel.add(jBScrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myFileList = new JBList();
        jBScrollPane1.setViewportView(myFileList);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(myUnlockAllButton);
        buttonGroup.add(myUnlockOneButton);
        buttonGroup.add(myUnlockDirButton);
    }
}