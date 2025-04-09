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

import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ide.impl.idea.util.net.HttpProxyConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.virtualFileSystem.http.RemoteFileInfo;
import consulo.virtualFileSystem.http.RemoteFileState;
import consulo.virtualFileSystem.http.event.FileDownloadingListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author nik
 */
public class RemoteFilePanel implements PropertyChangeListener {
    private static final Logger LOG = Logger.getInstance(RemoteFilePanel.class);
    @NonNls
    private static final String ERROR_CARD = "error";
    @NonNls
    private static final String DOWNLOADING_CARD = "downloading";
    @NonNls
    private static final String EDITOR_CARD = "editor";
    private JPanel myMainPanel;
    private JLabel myProgressLabel;
    private JProgressBar myProgressBar;
    private JButton myCancelButton;
    private JPanel myContentPanel;
    private JLabel myErrorLabel;
    private JButton myTryAgainButton;
    private JButton myChangeProxySettingsButton;
    private JPanel myEditorPanel;
    private JTextField myUrlTextField;
    private JPanel myToolbarPanel;
    private final Project myProject;
    private final HttpVirtualFile myVirtualFile;
    private final MergingUpdateQueue myProgressUpdatesQueue;
    private final MyDownloadingListener myDownloadingListener;
    private final EventDispatcher<PropertyChangeListener> myDispatcher = EventDispatcher.create(PropertyChangeListener.class);
    private @Nullable
    TextEditor myFileEditor;

    public RemoteFilePanel(final Project project, final HttpVirtualFile virtualFile) {
        myProject = project;
        myVirtualFile = virtualFile;
        myErrorLabel.setIcon(TargetAWT.to(PlatformIconGroup.generalError()));
        myUrlTextField.setText(virtualFile.getUrl());
        myProgressUpdatesQueue = new MergingUpdateQueue("downloading progress updates", 300, false, myMainPanel);
        initToolbar(project);

        final RemoteFileInfo remoteFileInfo = virtualFile.getFileInfo();
        myDownloadingListener = new MyDownloadingListener();
        remoteFileInfo.addDownloadingListener(myDownloadingListener);
        myCancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                remoteFileInfo.cancelDownloading();
            }
        });

        myTryAgainButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                showCard(DOWNLOADING_CARD);
                remoteFileInfo.restartDownloading();
            }
        });
        myChangeProxySettingsButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                ShowSettingsUtil.getInstance().editConfigurable(myMainPanel, new HttpProxyConfigurable());
            }
        });
        showCard(DOWNLOADING_CARD);
        remoteFileInfo.startDownloading();
        if (remoteFileInfo.getState() == RemoteFileState.DOWNLOADED) {
            switchEditor();
        }
        else {
            String errorMessage = remoteFileInfo.getErrorMessage();
            if (errorMessage != null) {
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
        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        myToolbarPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }

    private void showCard(final String name) {
        ((CardLayout)myContentPanel.getLayout()).show(myContentPanel, name);
    }

    private void switchEditor() {
        LOG.debug("Switching editor...");
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                final TextEditor textEditor = (TextEditor)TextEditorProvider.getInstance().createEditor(myProject, myVirtualFile);
                textEditor.addPropertyChangeListener(RemoteFilePanel.this);
                myEditorPanel.removeAll();
                myEditorPanel.add(textEditor.getComponent(), BorderLayout.CENTER);
                myFileEditor = textEditor;
                showCard(EDITOR_CARD);
                LOG.debug("Editor for downloaded file opened.");
            }
        });
    }

    @Nullable
    public TextEditor getFileEditor() {
        return myFileEditor;
    }

    public JPanel getMainPanel() {
        return myMainPanel;
    }

    public void selectNotify() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                myProgressUpdatesQueue.showNotify();
                if (myFileEditor != null) {
                    myFileEditor.selectNotify();
                }
            }
        });
    }

    public void deselectNotify() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                myProgressUpdatesQueue.hideNotify();
                if (myFileEditor != null) {
                    myFileEditor.deselectNotify();
                }
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

    public void propertyChange(PropertyChangeEvent evt) {
        myDispatcher.getMulticaster().propertyChange(evt);
    }

    private class MyDownloadingListener implements FileDownloadingListener {
        public void fileDownloaded(final VirtualFile localFile) {
            switchEditor();
        }

        public void downloadingCancelled() {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    if (myFileEditor != null) {
                        showCard(EDITOR_CARD);
                    }
                    else {
                        myErrorLabel.setText("Downloading cancelled");
                        showCard(ERROR_CARD);
                    }
                }
            });
        }

        public void downloadingStarted() {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    showCard(DOWNLOADING_CARD);
                }
            });
        }

        public void errorOccurred(@Nonnull final String errorMessage) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    myErrorLabel.setText(errorMessage);
                    showCard(ERROR_CARD);
                }
            });
        }

        public void progressMessageChanged(final boolean indeterminate, @Nonnull final String message) {
            myProgressUpdatesQueue.queue(new Update("progress text") {
                public void run() {
                    myProgressLabel.setText(message);
                }
            });
        }

        public void progressFractionChanged(final double fraction) {
            myProgressUpdatesQueue.queue(new Update("fraction") {
                public void run() {
                    myProgressBar.setValue((int)Math.round(100 * fraction));
                }
            });
        }
    }
}
