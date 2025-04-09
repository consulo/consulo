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
package consulo.fileChooser.provider;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionPointName;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.PathChooserDialog;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-06-28
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FileChooseDialogProvider extends FileOperateDialogProvider {
    ExtensionPointName<FileChooseDialogProvider> EP_NAME = ExtensionPointName.create(FileChooseDialogProvider.class);

    @Nonnull
    FileChooserDialog createFileChooser(
        @Nonnull FileChooserDescriptor descriptor,
        @Nullable ComponentManager project,
        @Nullable Component parent
    );

    @Nonnull
    PathChooserDialog createPathChooser(
        @Nonnull FileChooserDescriptor descriptor,
        @Nullable ComponentManager project,
        @Nullable Component parent
    );
}
