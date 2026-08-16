// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.PathChooserDialog;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Capable of choosing files in a local file system and in Docker/WSL containers.
 */
public class UniversalFileChooser implements FileChooserDialog, PathChooserDialog {
    private final FileChooserDescriptor myDescriptor;
    private final @Nullable Project myProject;

    public UniversalFileChooser(FileChooserDescriptor descriptor, @Nullable Project project) {
        myDescriptor = descriptor;
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<VirtualFile[]> chooseAsync(@Nullable VirtualFile toSelect) {
        return chooseAsync(myProject, toSelect == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{toSelect});
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<VirtualFile[]> chooseAsync(@Nullable ComponentManager project, VirtualFile[] toSelect) {
        Project passedProject = project instanceof Project resolved ? resolved : myProject;
        Project targetProject = passedProject != null ? passedProject : ProjectManager.getInstance().getDefaultProject();

        Path preselectPath = toSelect.length == 0
            ? NioFileChooserUtil.getLastOpenedPath(targetProject)
            : NioFileChooserUtil.toNioPathSafe(toSelect[0]);

        List<UniversalFileChooserContributor> contributors =
            Application.get().getExtensionPoint(UniversalFileChooserContributor.class).getExtensionList();

        UniversalFileChooserDescriptor dialogDescriptor =
            new UniversalFileChooserDescriptor(myDescriptor, targetProject, contributors, preselectPath);

        CompletableFuture<VirtualFile[]> result = new CompletableFuture<>();

        Dialog dialog = Application.get().getInstance(DialogService.class).build(dialogDescriptor);
        dialogDescriptor.setOkAction(() -> dialog.doOkAction(dialogDescriptor.getOkValue()));
        dialog.showAsync().whenComplete((value, throwable) -> {
            if (throwable != null || value == null) {
                result.completeExceptionally(new CancellationException());
                return;
            }
            List<Path> selectedPaths = dialogDescriptor.getSelectedFiles();
            if (!selectedPaths.isEmpty()) {
                NioFileChooserUtil.setLastOpenedFile(targetProject, selectedPaths.get(0));
            }
            result.complete(toVirtualFiles(selectedPaths).toArray(VirtualFile.EMPTY_ARRAY));
        });

        return result;
    }

    private List<VirtualFile> toVirtualFiles(List<Path> paths) {
        // Mirror FileChooserDialogImpl.doOKAction: after resolving NIO paths to VirtualFiles, run each
        // result through `descriptor.getFileToSelect(...)` so that, e.g., an archive file is returned as
        // its `jar://...!/` archive file system VirtualFile when the descriptor has `isChooseJarContents = true`.
        List<VirtualFile> resolved = new ArrayList<>();
        for (Path path : paths) {
            VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
            if (file != null && file.isValid()) {
                VirtualFile toSelect = myDescriptor.getFileToSelect(file);
                if (toSelect != null) {
                    resolved.add(toSelect);
                }
            }
        }
        return resolved;
    }
}
