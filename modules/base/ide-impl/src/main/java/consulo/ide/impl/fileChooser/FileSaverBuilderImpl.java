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

import consulo.component.ComponentManager;
import consulo.fileChooser.FileSaverBuilder;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.provider.FileSaveDialogProvider;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class FileSaverBuilderImpl implements FileSaverBuilder {
    private final FileSaverDescriptor myDescriptor;
    private final Function<FileSaverDescriptor, FileSaveDialogProvider> myProviderFinder;

    private @Nullable ComponentManager myProject;
    private @Nullable Component myParent;
    private @Nullable Path myBaseDir;
    private @Nullable String myFileName;

    public FileSaverBuilderImpl(FileSaverDescriptor descriptor, Function<FileSaverDescriptor, FileSaveDialogProvider> providerFinder) {
        myDescriptor = descriptor;
        myProviderFinder = providerFinder;
    }

    @Override
    public FileSaverBuilder project(@Nullable ComponentManager project) {
        myProject = project;
        return this;
    }

    @Override
    public FileSaverBuilder parent(@Nullable Component parent) {
        myParent = parent;
        return this;
    }

    @Override
    public FileSaverBuilder baseDir(@Nullable Path baseDir) {
        myBaseDir = baseDir;
        return this;
    }

    @Override
    public FileSaverBuilder fileName(@Nullable String fileName) {
        myFileName = fileName;
        return this;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<Path> showAsync() {
        Component parent = myParent;
        if (parent == null) {
            parent = TargetAWT.to(WindowManager.getInstance().suggestParentWindow((Project) myProject));
        }

        Project project = myProject instanceof Project resolved ? resolved : null;
        Path baseDir = FileChooserUtil.getInitialPath(myDescriptor, project, myBaseDir, false);

        return myProviderFinder.apply(myDescriptor).saveAsync(myDescriptor, myProject, parent, baseDir, myFileName).thenApply(path -> {
            FileChooserUtil.setLastOpenedFile(project, path);
            return path;
        });
    }
}
