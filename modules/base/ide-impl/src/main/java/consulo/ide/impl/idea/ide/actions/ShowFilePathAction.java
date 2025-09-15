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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.dataContext.DataManager;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.PlatformFeature;
import consulo.platform.PlatformFileSystem;
import consulo.platform.base.localize.ActionLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ActionImpl(id = "ShowFilePath")
public class ShowFilePathAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ShowFilePathAction.class);

    @UsedInPlugin
    public static final NotificationListener FILE_SELECTING_LISTENER = new NotificationListener.Adapter() {
        @Override
        @RequiredUIAccess
        protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e) {
            URL url = e.getURL();
            if (url != null) {
                try {
                    Platform.current().openFileInFileManager(new File(url.toURI()), UIAccess.current());
                }
                catch (URISyntaxException ex) {
                    LOG.warn("invalid URL: " + url, ex);
                }
            }
            notification.expire();
        }
    };

    public ShowFilePathAction() {
        super(ActionLocalize.actionShowfilepathText(), ActionLocalize.actionShowfilepathDescription());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean visible = Platform.current().supportsFeature(PlatformFeature.OPEN_FILE_IN_FILE_MANANGER);
        e.getPresentation().setVisible(visible);
        if (visible) {
            VirtualFile file = e.getData(VirtualFile.KEY);
            e.getPresentation().setEnabled(file != null);
            e.getPresentation().setTextValue(ActionLocalize.actionShowfilepathTuned(file != null && file.isDirectory() ? 1 : 0));
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        show(
            UIAccess.current(),
            e.getData(Project.KEY),
            e.getRequiredData(VirtualFile.KEY),
            popup -> DataManager.getInstance()
                .getDataContextFromFocus()
                .doWhenDone(popup::showInBestPositionFor)
        );
    }

    public static void show(UIAccess uiAccess, Project project, VirtualFile file, MouseEvent e) {
        show(uiAccess, project, file, popup -> {
            if (e.getComponent().isShowing()) {
                popup.show(new RelativePoint(e));
            }
        });
    }

    public static void show(@Nonnull UIAccess uiAccess, @Nullable Project project, VirtualFile file, Consumer<ListPopup> action) {
        if (!Platform.current().supportsFeature(PlatformFeature.OPEN_FILE_IN_FILE_MANANGER)) {
            return;
        }

        List<VirtualFile> files = new ArrayList<>();
        List<String> fileUrls = new ArrayList<>();
        VirtualFile eachParent = file;
        while (eachParent != null) {
            int index = files.size();
            files.add(index, eachParent);
            fileUrls.add(index, getPresentableUrl(eachParent));
            if (eachParent.getParent() == null && eachParent.getFileSystem() instanceof ArchiveFileSystem) {
                eachParent = ArchiveVfsUtil.getVirtualFileForArchive(eachParent);
                if (eachParent == null) {
                    break;
                }
            }
            eachParent = eachParent.getParent();
        }

        PlatformFileSystem fs = Platform.current().fs();
        Application.get().executeOnPooledThread(() -> {
            List<Image> icons = new ArrayList<>();
            for (String url : fileUrls) {
                File ioFile = new File(url);
                icons.add(ioFile.exists() ? fs.getImage(ioFile) : Image.empty(16));
            }

            uiAccess.give(() -> action.accept(createPopup(project, files, icons)));
        });
    }

    private static String getPresentableUrl(VirtualFile eachParent) {
        String url = eachParent.getPresentableUrl();
        if (eachParent.getParent() == null && Platform.current().os().isWindows()) {
            url += "\\";
        }
        return url;
    }

    private static ListPopup createPopup(@Nullable Project project, List<VirtualFile> files, List<Image> icons) {
        BaseListPopupStep<VirtualFile> step = new BaseListPopupStep<VirtualFile>("File Path", files, icons) {
            @Nonnull
            @Override
            public String getTextFor(VirtualFile value) {
                return value.getPresentableName();
            }

            @Override
            @RequiredUIAccess
            public PopupStep onChosen(VirtualFile selectedValue, boolean finalChoice) {
                File selectedFile = new File(getPresentableUrl(selectedValue));
                if (selectedFile.exists()) {
                    Platform.current().openFileInFileManager(selectedFile, UIAccess.current());
                }
                return FINAL_CHOICE;
            }
        };

        return JBPopupFactory.getInstance().createListPopup(project, step);
    }

    /**
     * Shows system file manager with given file's parent directory open and the file highlighted in it<br/>
     * (note that not all platforms support highlighting).
     *
     * @param file a file or directory to show and highlight in a file manager.
     */
    @RequiredUIAccess
    @Deprecated
    public static void openFile(@Nonnull File file) {
        Platform.current().openDirectoryInFileManager(file, UIAccess.current());
    }

    @RequiredUIAccess
    public static Boolean showDialog(Project project, String message, String title, File file) {
        Boolean[] ref = new Boolean[1];
        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return true;
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                if (!value) {
                    ref[0] = exitCode == 0;
                }
            }

            @Override
            public boolean canBeHidden() {
                return true;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return true;
            }

            @Nonnull
            @Override
            public LocalizeValue getDoNotShowMessage() {
                return CommonLocalize.dialogOptionsDoNotAsk();
            }
        };
        showDialog(project, message, title, file, option);
        return ref[0];
    }

    @RequiredUIAccess
    public static void showDialog(Project project, String message, String title, File file, DialogWrapper.DoNotAskOption option) {
        if (Messages.showOkCancelDialog(
            project,
            message,
            title,
            RevealFileAction.getActionName().get(),
            IdeLocalize.actionClose().get(),
            UIUtil.getInformationIcon(),
            option
        ) == Messages.OK) {
            openFile(file);
        }
    }

    @Nullable
    public static VirtualFile findLocalFile(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }

        if (file.isInLocalFileSystem()) {
            return file;
        }

        return file.getFileSystem() instanceof ArchiveFileSystem archiveFileSystem && file.getParent() == null
            ? archiveFileSystem.getLocalVirtualFileFor(file)
            : null;
    }
}
