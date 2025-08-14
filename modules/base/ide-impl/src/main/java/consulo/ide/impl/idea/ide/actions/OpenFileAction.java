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

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.PathChooserDialog;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.ide.impl.idea.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.impl.internal.ProjectImplUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

@ActionImpl(id = "OpenFile")
public class OpenFileAction extends AnAction implements DumbAware {
    @Inject
    public OpenFileAction() {
        this(ActionLocalize.actionOpenfileText(), ActionLocalize.actionOpenfileDescription(), PlatformIconGroup.nodesFolderopened());
    }

    public OpenFileAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        @Nullable Project project = e.getData(Project.KEY);
        boolean showFiles = project != null;

        FileChooserDescriptor descriptor = new OpenProjectFileChooserDescriptor(true) {
            @Override
            @RequiredUIAccess
            public boolean isFileSelectable(VirtualFile file) {
                return super.isFileSelectable(file) || (!file.isDirectory() && showFiles && !FileElement.isArchive(file));
            }

            @Override
            @RequiredUIAccess
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
        descriptor.withTitleValue(
            showFiles ? LocalizeValue.localizeTODO("Open File or Project") : LocalizeValue.localizeTODO("Open Project")
        );
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

    @RequiredUIAccess
    private static void doOpenFile(@Nullable Project project, @Nonnull VirtualFile[] result) {
        for (VirtualFile file : result) {
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
                    UIUtil.getQuestionIcon()
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

    @RequiredUIAccess
    public static void openFile(String filePath, Project project) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file != null && file.isValid()) {
            openFile(file, project);
        }
    }

    @RequiredUIAccess
    public static void openFile(VirtualFile virtualFile, Project project) {
        FileEditorProviderManager editorProviderManager = FileEditorProviderManager.getInstance();
        if (editorProviderManager.getProviders(project, virtualFile).length == 0) {
            Messages.showMessageDialog(
                project,
                IdeLocalize.errorFilesOfThisTypeCannotBeOpened(Application.get().getName()).get(),
                IdeLocalize.titleCannotOpenFile().get(),
                UIUtil.getErrorIcon()
            );
            return;
        }

        NonProjectFileWritingAccessProvider.allowWriting(virtualFile);
        OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(project, virtualFile);
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    }
}
