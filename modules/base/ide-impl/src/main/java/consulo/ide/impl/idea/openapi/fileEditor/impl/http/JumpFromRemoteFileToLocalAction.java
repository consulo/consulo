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
package consulo.ide.impl.idea.openapi.fileEditor.impl.http;

import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.ui.FileAppearanceService;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.virtualFileSystem.http.RemoteFileState;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

/**
 * @author nik
 */
public class JumpFromRemoteFileToLocalAction extends AnAction {
    private final HttpVirtualFile myFile;
    private final Project myProject;

    public JumpFromRemoteFileToLocalAction(HttpVirtualFile file, Project project) {
        super(LocalizeValue.localizeTODO("Find Local File"), LocalizeValue.empty(), PlatformIconGroup.generalAutoscrolltosource());
        myFile = file;
        myProject = project;
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        RemoteFileState state = myFile.getFileInfo().getState();
        e.getPresentation().setEnabled(state == RemoteFileState.DOWNLOADED);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        String url = myFile.getUrl();
        String fileName = myFile.getName();
        Collection<VirtualFile> files = findLocalFiles(myProject, url, fileName);

        if (files.isEmpty()) {
            Messages.showErrorDialog(myProject, "Cannot find local file for '" + url + "'", CommonLocalize.titleError().get());
            return;
        }

        if (files.size() == 1) {
            navigateToFile(myProject, ContainerUtil.getFirstItem(files, null));
        }
        else {
            JList<VirtualFile> list = new JBList<>(VfsUtilCore.toVirtualFileArray(files));
            list.setCellRenderer(new ColoredListCellRenderer<>() {
                @Override
                protected void customizeCellRenderer(
                    @Nonnull JList<? extends VirtualFile> list,
                    VirtualFile value,
                    int index,
                    boolean selected,
                    boolean hasFocus
                ) {
                    FileAppearanceService.getInstance().getRenderForVirtualFile(value).accept(this);
                }
            });
            new PopupChooserBuilder(list)
                .setTitle("Select Target File")
                .setMovable(true)
                .setItemChoosenCallback(() -> {
                    for (Object value : list.getSelectedValues()) {
                        navigateToFile(myProject, (VirtualFile)value);
                    }
                })
                .createPopup()
                .showUnderneathOf(e.getInputEvent().getComponent());
        }
    }

    public static Collection<VirtualFile> findLocalFiles(Project project, String url, String fileName) {
        for (LocalFileFinder finder : LocalFileFinder.EP_NAME.getExtensions()) {
            VirtualFile file = finder.findLocalFile(url, project);
            if (file != null) {
                return Collections.singletonList(file);
            }
        }

        return FilenameIndex.getVirtualFilesByName(project, fileName, GlobalSearchScope.allScope(project));
    }

    private static void navigateToFile(Project project, @Nonnull VirtualFile file) {
        new OpenFileDescriptorImpl(project, file).navigate(true);
    }
}
