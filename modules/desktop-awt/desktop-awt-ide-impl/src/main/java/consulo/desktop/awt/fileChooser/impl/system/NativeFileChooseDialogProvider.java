/*
 * Copyright 2013-2018 consulo.io
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
package consulo.desktop.awt.fileChooser.impl.system;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.provider.FileChooseDialogProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2018-06-28
 */
@ExtensionImpl
public class NativeFileChooseDialogProvider implements FileChooseDialogProvider {
    
    @Override
    public String getId() {
        return "native";
    }

    
    @Override
    public String getName() {
        return "system";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    
    @RequiredUIAccess
    @Override
    public CompletableFuture<List<Path>> chooseAsync(
        FileChooserDescriptor descriptor,
        @Nullable ComponentManager project,
        @Nullable Component parent,
        @Nullable Path toSelect
    ) {
        return new NativeFileChooseDialog(descriptor, parent, (Project) project).chooseAsync(toSelect);
    }
}
