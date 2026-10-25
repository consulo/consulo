// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.internal.UniversalFileChooserContributor;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Capable of choosing files in a local file system and in Docker/WSL containers.
 */
public class UniversalFileChooser {
    private final FileChooserDescriptor myDescriptor;
    private final @Nullable Project myProject;
    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;

    public UniversalFileChooser(
        FileChooserDescriptor descriptor,
        @Nullable Project project,
        ApplicationTreeExecutorFactory treeExecutorFactory
    ) {
        myDescriptor = descriptor;
        myProject = project;
        myTreeExecutorFactory = treeExecutorFactory;
    }

    @RequiredUIAccess
    public CompletableFuture<List<Path>> chooseAsync(@Nullable ComponentManager project, @Nullable Path toSelect) {
        Project passedProject = project instanceof Project resolved ? resolved : myProject;
        Project targetProject = passedProject != null ? passedProject : ProjectManager.getInstance().getDefaultProject();

        List<UniversalFileChooserContributor> contributors =
            Application.get().getExtensionPoint(UniversalFileChooserContributor.class).getExtensionList();

        return choosePaths(targetProject, toSelect, contributors);
    }

    /**
     * Chooses in the file systems the given contributors list, answering the paths as they are - they may belong to
     * another machine, where no virtual file can stand for them.
     */
    @RequiredUIAccess
    public CompletableFuture<List<Path>> choosePaths(Project project,
                                                     @Nullable Path preselectPath,
                                                     List<UniversalFileChooserContributor> contributors) {
        UniversalFileChooserDescriptor dialogDescriptor =
            new UniversalFileChooserDescriptor(myDescriptor, project, contributors, preselectPath, myTreeExecutorFactory);

        CompletableFuture<List<Path>> result = new CompletableFuture<>();

        Dialog dialog = Application.get().getInstance(DialogService.class).build(dialogDescriptor);
        dialogDescriptor.setDialog(dialog);
        dialog.showAsync().whenComplete((value, throwable) -> {
            if (throwable != null || value == null) {
                result.completeExceptionally(new CancellationException());
                return;
            }
            result.complete(dialogDescriptor.getSelectedFiles());
        });

        return result;
    }
}
