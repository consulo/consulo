/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.fileChooser;

import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserBuilder;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.internal.PathFileChooser;
import consulo.fileChooser.provider.FileChooseDialogProvider;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class FileChooserBuilderImpl implements FileChooserBuilder {
    private static final Logger LOG = Logger.getInstance(FileChooserBuilderImpl.class);

    private static final Object NO_INPUT = new Object();

    private final Application myApplication;
    private final FileChooserDescriptor myDescriptor;
    private final Function<FileChooserDescriptor, FileChooseDialogProvider> myProviderFinder;

    private @Nullable ComponentManager myProject;
    private @Nullable Component myParent;
    private @Nullable Path myToSelect;
    private boolean myPreferLastOverExplicit;

    public FileChooserBuilderImpl(
        Application application,
        FileChooserDescriptor descriptor,
        Function<FileChooserDescriptor, FileChooseDialogProvider> providerFinder
    ) {
        myApplication = application;
        myDescriptor = descriptor;
        myProviderFinder = providerFinder;
    }

    @Override
    public FileChooserBuilder project(@Nullable ComponentManager project) {
        myProject = project;
        return this;
    }

    @Override
    public FileChooserBuilder parent(@Nullable Component parent) {
        myParent = parent;
        return this;
    }

    @Override
    public FileChooserBuilder toSelect(@Nullable Path toSelect) {
        myToSelect = toSelect;
        return this;
    }

    @Override
    public FileChooserBuilder preferLastOverExplicit() {
        myPreferLastOverExplicit = true;
        return this;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<Path> showSingle() {
        LOG.assertTrue(!myDescriptor.isChooseMultiple());
        return showMultiple().thenApply(paths -> paths.get(0));
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<List<Path>> showMultiple() {
        return choose().thenApply(paths -> {
            if (paths.isEmpty()) {
                throw new CancellationException();
            }
            return paths;
        });
    }

    @RequiredUIAccess
    private CompletableFuture<List<Path>> choose() {
        if (myDescriptor.isAnyPlatform() || !myDescriptor.getPlatform().equals(Platform.current())) {
            return myApplication.getInstance(PathFileChooser.class).choose(myDescriptor, myProject, myToSelect);
        }

        Component parent = myParent;
        if (parent == null) {
            parent = TargetAWT.to(WindowManager.getInstance().suggestParentWindow((Project) myProject));
        }

        Project project = myProject instanceof Project resolved ? resolved : null;
        Path toSelect = FileChooserUtil.getInitialPath(myDescriptor, project, myToSelect, myPreferLastOverExplicit);

        return myProviderFinder.apply(myDescriptor).chooseAsync(myDescriptor, myProject, parent, toSelect).thenApply(paths -> {
            if (!paths.isEmpty()) {
                FileChooserUtil.setLastOpenedFile(project, paths.get(0));
            }
            return paths;
        });
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<VirtualFile> showSingleAsVirtualFile() {
        LOG.assertTrue(!myDescriptor.isChooseMultiple());
        return showMultipleAsVirtualFiles().thenApply(files -> files.get(0));
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<List<VirtualFile>> showMultipleAsVirtualFiles() {
        CompletableFuture<List<Path>> paths = showMultiple();

        CoroutineScope scope = CoroutineScope.of(myApplication.coroutineContext());
        scope.putCopyableUserData(UIAccess.KEY, UIAccess.current());

        return Coroutine
            .first(CompletableFutureStep.<Object, List<Path>>await(ignored -> paths))
            .then(CodeExecution.<List<Path>, List<VirtualFile>>apply(chosen -> consulo.fileChooser.util.FileChooserUtil.toVirtualFiles(myDescriptor, chosen)))
            .then(UIAction.<List<VirtualFile>, List<VirtualFile>>apply(files -> {
                if (files.isEmpty()) {
                    throw new CancellationException();
                }
                return files;
            }))
            .runAsync(scope, NO_INPUT)
            .toFuture();
    }
}
