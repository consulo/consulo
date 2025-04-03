/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.PathChooserDialog;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.project.impl.internal.ProjectImplUtil;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.ide.impl.idea.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessors;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

public class OpenFileAction extends AnAction implements DumbAware {
    @NonNls
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        @Nullable final Project project = e.getData(Project.KEY);
        final boolean showFiles = project != null;

        final FileChooserDescriptor descriptor = new OpenProjectFileChooserDescriptor(true) {
            @RequiredUIAccess
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return super.isFileSelectable(file) || (!file.isDirectory() && showFiles && !FileElement.isArchive(file));
            }

            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                if (!file.isDirectory() && isFileSelectable(file)) {
                    return showHiddenFiles || !FileElement.isFileHidden(file);
                }
                return super.isFileVisible(file, showHiddenFiles);
            }

            @Override
            public boolean isChooseMultiple() {
                return showFiles;
            }
        };
        descriptor.setTitle(showFiles ? "Open File or Project" : "Open Project");
        // FIXME [VISTALL] we need this? descriptor.setDescription(getFileChooserDescription());

        descriptor.putUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT, Boolean.TRUE);

        FileChooser.chooseFiles(descriptor, project, VfsUtil.getUserHomeDir()).doWhenDone(files -> {
            for (VirtualFile file : files) {
                if (!descriptor.isFileSelectable(file)) { // on Mac, it could be selected anyway
                    Messages.showInfoMessage(
                        project,
                        file.getPresentableUrl() + " contains no " + Application.get().getName() + " project",
                        "Cannot Open Project"
                    );
                    return;
                }
            }
            doOpenFile(project, files);
        });
    }

    @Nonnull
    private static String getFileChooserDescription() {
        List<ProjectOpenProcessor> providers = ProjectOpenProcessors.getInstance().getProcessors();
        List<String> fileSamples = new ArrayList<>();
        for (ProjectOpenProcessor processor : providers) {
            processor.collectFileSamples(fileSamples::add);
        }
        return IdeLocalize.importProjectChooserHeader(StringUtil.join(fileSamples, ", <br>")).get();
    }

    @RequiredUIAccess
    private static void doOpenFile(@Nullable final Project project, @Nonnull final VirtualFile[] result) {
        for (final VirtualFile file : result) {
            if (file.isDirectory()) {
                ProjectImplUtil.openAsync(file.getPath(), project, false, UIAccess.current())
                    .doWhenDone(openedProject -> FileChooserUtil.setLastOpenedFile(openedProject, file));
                return;
            }

            if (OpenProjectFileChooserDescriptor.canOpen(file)) {
                int answer = Messages.showYesNoDialog(
                    project,
                    IdeLocalize.messageOpenFileIsProject(file.getName()).get(),
                    IdeLocalize.titleOpenProject().get(),
                    Messages.getQuestionIcon()
                );
                if (answer == 0) {
                    ProjectImplUtil.openAsync(
                        file.getPath(),
                        project,
                        false,
                        UIAccess.current()
                    ).doWhenDone(openedProject -> FileChooserUtil.setLastOpenedFile(openedProject, file));
                    return;
                }
            }

            FileType type = FileTypeChooser.getKnownFileTypeOrAssociate(file, project);
            if (type == null) {
                return;
            }

            if (project != null) {
                openFile(file, project);
            }
        }
    }

    public static void openFile(final String filePath, final Project project) {
        final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file != null && file.isValid()) {
            openFile(file, project);
        }
    }

    public static void openFile(final VirtualFile virtualFile, final Project project) {
        FileEditorProviderManager editorProviderManager = FileEditorProviderManager.getInstance();
        if (editorProviderManager.getProviders(project, virtualFile).length == 0) {
            Messages.showMessageDialog(
                project,
                IdeLocalize.errorFilesOfThisTypeCannotBeOpened(Application.get().getName()).get(),
                IdeLocalize.titleCannotOpenFile().get(),
                Messages.getErrorIcon()
            );
            return;
        }

        NonProjectFileWritingAccessProvider.allowWriting(virtualFile);
        OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(project, virtualFile);
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    }
}
