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
package consulo.fileChooser.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.provider.FileChooseDialogProvider;
import consulo.fileChooser.provider.FileOperateDialogProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl(id = FileOperateDialogProvider.APPLICATION_ID, order = "last")
public class UniversalFileChooseDialogProvider implements FileChooseDialogProvider {
    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;

    @Inject
    public UniversalFileChooseDialogProvider(ApplicationTreeExecutorFactory treeExecutorFactory) {
        myTreeExecutorFactory = treeExecutorFactory;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<List<Path>> chooseAsync(
        FileChooserDescriptor descriptor,
        @Nullable ComponentManager project,
        @Nullable Component parent,
        @Nullable Path toSelect
    ) {
        return new UniversalFileChooser(descriptor, (Project) project, myTreeExecutorFactory).chooseAsync(project, toSelect);
    }

    @Override
    public String getId() {
        return APPLICATION_ID;
    }

    @Override
    public String getName() {
        return "application";
    }
}
