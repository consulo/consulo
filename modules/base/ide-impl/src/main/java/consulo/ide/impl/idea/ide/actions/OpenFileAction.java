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
import consulo.fileChooser.FileChooserFactory;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.node.FileElement;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.fileEditor.impl.internal.NonProjectFileWritingAccessProvider;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.impl.internal.ProjectImplUtil;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ActionImpl(id = "OpenFile")
public class OpenFileAction extends AnAction implements DumbAware {
    private final FileChooserFactory myFileChooserFactory;
    private final VirtualFileManager myVirtualFileManager;

    @Inject
    public OpenFileAction(FileChooserFactory fileChooserFactory, VirtualFileManager virtualFileManager) {
        this(
            fileChooserFactory,
            virtualFileManager,
            ActionLocalize.actionOpenfileText(),
            ActionLocalize.actionOpenfileDescription(),
            PlatformIconGroup.nodesFolderopened()
        );
    }

    protected OpenFileAction(
        FileChooserFactory fileChooserFactory,
        VirtualFileManager virtualFileManager,
        LocalizeValue text,
        LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
        myFileChooserFactory = fileChooserFactory;
        myVirtualFileManager = virtualFileManager;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        boolean showFiles = project != null;

        FileChooserDescriptor descriptor = new OpenProjectFileChooserDescriptor(true) {
            @Override
            public boolean isPathSelectable(Path path) {
                return super.isPathSelectable(path) || showFiles && !Files.isDirectory(path) && !isArchive(path);
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
        descriptor.withTitle(
            showFiles ? LocalizeValue.localizeTODO("Open File or Project") : LocalizeValue.localizeTODO("Open Project")
        );

        UIAccess uiAccess = UIAccess.current();
        myFileChooserFactory.newBuilder(descriptor)
            .project(project)
            .toSelect(Platform.current().user().homePath())
            .preferLastOverExplicit()
            .showMultiple()
            .whenCompleteAsync((paths, error) -> {
                if (error != null) {
                    return;
                }

                for (Path path : paths) {
                    if (!descriptor.isPathSelectable(path)) { // on Mac, it could be selected anyway
                        Messages.showInfoMessage(
                            project,
                            path + " contains no " + Application.get().getName() + " project",
                            "Cannot Open Project"
                        );
                        return;
                    }
                }
                doOpenFile(project, paths);
            }, uiAccess);
    }

    @RequiredUIAccess
    private void doOpenFile(@Nullable Project project, List<Path> paths) {
        List<Path> toOpen = new ArrayList<>();
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                openProject(project, path);
                return;
            }

            if (OpenProjectFileChooserDescriptor.canOpen(path)) {
                int answer = Messages.showYesNoDialog(
                    project,
                    IdeLocalize.messageOpenFileIsProject(String.valueOf(path.getFileName())).get(),
                    IdeLocalize.titleOpenProject().get(),
                    UIUtil.getQuestionIcon()
                );
                if (answer == 0) {
                    openProject(project, path);
                    return;
                }
            }

            toOpen.add(path);
        }

        if (project != null && !toOpen.isEmpty()) {
            openFiles(project, toOpen);
        }
    }

    @RequiredUIAccess
    private static void openProject(@Nullable Project project, Path path) {
        ProjectImplUtil.openAsync(path, project, false, UIAccess.current())
            .doWhenDone(openedProject -> FileChooserUtil.setLastOpenedFile(openedProject, path));
    }

    private void openFiles(Project project, List<Path> paths) {
        CoroutineScope.launchAsync(
            project.coroutineContext(),
            () -> Coroutine
                .first(CodeExecution.<Object, List<VirtualFile>>apply(ignored -> {
                    List<VirtualFile> files = new ArrayList<>(paths.size());
                    for (Path path : paths) {
                        VirtualFile file = myVirtualFileManager.refreshAndFindFileByNioPath(path);
                        if (file != null && file.isValid()) {
                            files.add(file);
                        }
                    }
                    return files;
                }))
                .then(UIAction.<List<VirtualFile>, List<VirtualFile>>apply(files -> {
                    for (VirtualFile file : files) {
                        FileType type = FileTypeChooser.getKnownFileTypeOrAssociate(file, project);
                        if (type == null) {
                            break;
                        }
                        openFile(file, project);
                    }
                    return files;
                }))
        );
    }

    private static boolean isArchive(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && FileTypeRegistry.getInstance().getFileTypeByFileName(fileName.toString()) instanceof ArchiveFileType;
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
