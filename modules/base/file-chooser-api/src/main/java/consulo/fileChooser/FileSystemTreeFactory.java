/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.fileChooser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.ui.ex.action.ActionGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.util.function.Function;

@ServiceAPI(ComponentScope.APPLICATION)
public interface FileSystemTreeFactory {
    @Nonnull
    static FileSystemTreeFactory getInstance() {
        return Application.get().getInstance(FileSystemTreeFactory.class);
    }

    @Nonnull
    FileSystemTree createFileSystemTree(@Nullable ComponentManager project, FileChooserDescriptor fileChooserDescriptor);

    @Nonnull
    FileSystemTree createFileSystemTree(@Nullable ComponentManager project,
                                        FileChooserDescriptor descriptor,
                                        Object tree,
                                        @Nullable TreeCellRenderer renderer,
                                        @Nullable Function<? super TreePath, String> speedSearchConverter);
    @Nonnull
    ActionGroup createDefaultFileSystemActions(FileSystemTree fileSystemTree);
}
