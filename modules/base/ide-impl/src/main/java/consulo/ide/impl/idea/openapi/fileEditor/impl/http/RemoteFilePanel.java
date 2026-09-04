/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.http;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.application.Application;
import consulo.disposer.Disposer;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.http.HttpProxySettingService;
import consulo.http.localize.HttpLocalize;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.virtualFileSystem.http.RemoteFileInfo;
import consulo.virtualFileSystem.http.RemoteFileState;
import consulo.virtualFileSystem.http.event.FileDownloadingListener;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public class RemoteFilePanel implements PropertyChangeListener {
    private static final Logger LOG = Logger.getInstance(RemoteFilePanel.class);
    private static final String ERROR_CARD = "error";
    private static final String DOWNLOADING_CARD = "downloading";
    private static final String EDITOR_CARD = "editor";
    private JPanel myMainPanel;
    private Label myProgressLabel;
    private JProgressBar myProgressBar;
    private Button myCancelButton;
    private JPanel myContentPanel;
    private Label myErrorLabel;
    private Button myTryAgainButton;
    private Button myChangeProxySettingsButton;
    private JPanel myEditorPanel;
    private JTextField myUrlTextField;
    private JPanel myToolbarPanel;
    private final Project myProject;
    private final HttpVirtualFile myVirtualFile;
    private final MergingUpdateQueue myProgressUpdatesQueue;
    private final MyDownloadingListener myDownloadingListener;
    private final EventDispatcher<PropertyChangeListener> myDispatcher = EventDispatcher.create(PropertyChangeListener.class);
    private @Nullable TextEditor myFileEditor;

    public RemoteFilePanel(Project project, HttpVirtualFile virtualFile) {
        $$$setupUI$$$();

        myProject = project;
        myVirtualFile = virtualFile;
        myErrorLabel.setImage(PlatformIconGroup.generalError());
        myUrlTextField.setText(virtualFile.getUrl());
        myProgressUpdatesQueue = new MergingUpdateQueue("downloading progress updates", 300, false, myMainPanel);
        initToolbar(project);

        RemoteFileInfo remoteFileInfo = virtualFile.getFileInfo();
        myDownloadingListener = new MyDownloadingListener();
        remoteFileInfo.addDownloadingListener(myDownloadingListener);
        myCancelButton.addClickListener(e -> remoteFileInfo.cancelDownloading());

        myTryAgainButton.addClickListener(e -> {
            showCard(DOWNLOADING_CARD);
            remoteFileInfo.restartDownloading();
        });
        myChangeProxySettingsButton.addClickListener(
            e -> Application.get().getInstance(HttpProxySettingService.class).showSettings(project)
        );
        showCard(DOWNLOADING_CARD);
        remoteFileInfo.startDownloading();
        if (remoteFileInfo.getState() == RemoteFileState.DOWNLOADED) {
            switchEditor();
        }
        else {
            LocalizeValue errorMessage = remoteFileInfo.getErrorMessage();
            if (errorMessage != LocalizeValue.empty()) {
                myDownloadingListener.errorOccurred(errorMessage);
            }
        }
    }

    private void initToolbar(Project project) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new RefreshRemoteFileAction(myVirtualFile));

        for (RemoteFileEditorActionProvider actionProvider : RemoteFileEditorActionProvider.EP_NAME.getExtensionList()) {
            group.addAll(actionProvider.createToolbarActions(project, myVirtualFile));
        }
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        myToolbarPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }

    private void showCard(String name) {
        ((CardLayout) myContentPanel.getLayout()).show(myContentPanel, name);
    }

    private void switchEditor() {
        LOG.debug("Switching editor...");
        Application.get().invokeLater(() -> {
            TextEditor textEditor = (TextEditor) TextEditorProvider.getInstance().createEditor(myProject, myVirtualFile);
            textEditor.addPropertyChangeListener(RemoteFilePanel.this);
            myEditorPanel.removeAll();
            myEditorPanel.add(textEditor.getComponent(), BorderLayout.CENTER);
            myFileEditor = textEditor;
            showCard(EDITOR_CARD);
            LOG.debug("Editor for downloaded file opened.");
        });
    }

    public @Nullable TextEditor getFileEditor() {
        return myFileEditor;
    }

    public JPanel getMainPanel() {
        return myMainPanel;
    }

    public void selectNotify() {
        UIUtil.invokeLaterIfNeeded(() -> {
            myProgressUpdatesQueue.showNotify();
            if (myFileEditor != null) {
                myFileEditor.selectNotify();
            }
        });
    }

    public void deselectNotify() {
        UIUtil.invokeLaterIfNeeded(() -> {
            myProgressUpdatesQueue.hideNotify();
            if (myFileEditor != null) {
                myFileEditor.deselectNotify();
            }
        });
    }

    public void dispose() {
        myVirtualFile.getFileInfo().removeDownloadingListener(myDownloadingListener);
        myProgressUpdatesQueue.dispose();
        if (myFileEditor != null) {
            Disposer.dispose(myFileEditor);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        myDispatcher.addListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        myDispatcher.removeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        myDispatcher.getMulticaster().propertyChange(evt);
    }

    /**
     * Method generated by Consulo GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        myMainPanel = new JPanel();
        myMainPanel.setLayout(new GridLayoutManager(3, 1, JBUI.emptyInsets(), -1, 0));
        myContentPanel = new JPanel();
        myContentPanel.setLayout(new CardLayout(0, 0));
        myMainPanel.add(
            myContentPanel,
            new GridConstraints(
                1,
                0,
                2,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, JBUI.insets(5), -1, -1));
        myContentPanel.add(panel1, "downloading");
        myProgressLabel = Label.create(HttpLocalize.downloadingFileStarted());
        panel1.add(
            TargetAWT.to(myProgressLabel),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
        panel1.add(
            panel2,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                new Dimension(241, 28),
                null,
                1,
                false
            )
        );
        myProgressBar = new JProgressBar();
        panel2.add(
            myProgressBar,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myCancelButton = Button.create(CommonLocalize.buttonCancel());
        panel2.add(
            TargetAWT.to(myCancelButton),
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        Spacer spacer1 = new Spacer();
        panel1.add(
            spacer1,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, JBUI.insets(5), -1, -1));
        myContentPanel.add(panel3, "error");
        myErrorLabel = Label.create();
        panel3.add(
            TargetAWT.to(myErrorLabel),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, JBUI.emptyInsets(), -1, -1));
        panel3.add(
            panel4,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myChangeProxySettingsButton = Button.create(HttpLocalize.downloadingFileChangeHttpProxySettings());
        panel4.add(
            TargetAWT.to(myChangeProxySettingsButton),
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        Spacer spacer2 = new Spacer();
        panel4.add(
            spacer2,
            new GridConstraints(
                0,
                2,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        myTryAgainButton = Button.create(HttpLocalize.downloadingFileTryAgainButton());
        panel4.add(
            TargetAWT.to(myTryAgainButton),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        Spacer spacer3 = new Spacer();
        panel3.add(
            spacer3,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myEditorPanel = new JPanel();
        myEditorPanel.setLayout(new BorderLayout(0, 0));
        myContentPanel.add(myEditorPanel, "editor");
        JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, JBUI.insets(3, 3, 0, 3), 0, -1));
        myMainPanel.add(
            panel5,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        myUrlTextField = new JTextField();
        myUrlTextField.setEditable(false);
        panel5.add(
            myUrlTextField,
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(150, -1),
                null,
                0,
                false
            )
        );
        myToolbarPanel = new JPanel();
        myToolbarPanel.setLayout(new BorderLayout(0, 0));
        panel5.add(
            myToolbarPanel,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
    }

    public JComponent $$$getRootComponent$$$() {
        return myMainPanel;
    }

    private class MyDownloadingListener implements FileDownloadingListener {
        @Override
        public void fileDownloaded(VirtualFile localFile) {
            switchEditor();
        }

        @Override
        public void downloadingCancelled() {
            Application.get().invokeLater(() -> {
                if (myFileEditor != null) {
                    showCard(EDITOR_CARD);
                }
                else {
                    myErrorLabel.setText(HttpLocalize.downloadingFileCancelled());
                    showCard(ERROR_CARD);
                }
            });
        }

        @Override
        public void downloadingStarted() {
            Application.get().invokeLater(() -> showCard(DOWNLOADING_CARD));
        }

        @Override
        public void errorOccurred(LocalizeValue errorMessage) {
            Application.get().invokeLater(() -> {
                myErrorLabel.setText(errorMessage);
                showCard(ERROR_CARD);
            });
        }

        @Override
        public void progressMessageChanged(boolean indeterminate, LocalizeValue message) {
            myProgressUpdatesQueue.queue(new Update("progress text") {
                @Override
                @RequiredUIAccess
                public void run() {
                    myProgressLabel.setText(message);
                }
            });
        }

        @Override
        public void progressFractionChanged(double fraction) {
            myProgressUpdatesQueue.queue(new Update("fraction") {
                @Override
                public void run() {
                    myProgressBar.setValue((int) Math.round(100 * fraction));
                }
            });
        }
    }
}
