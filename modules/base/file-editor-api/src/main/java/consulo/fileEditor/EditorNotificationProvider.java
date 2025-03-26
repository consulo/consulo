/*
 * Copyright 2013-2022 consulo.io
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
package consulo.fileEditor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface EditorNotificationProvider {
    @Nonnull
    String getId();

    @Nullable
    @RequiredReadAction
    EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file,
                                                @Nonnull FileEditor fileEditor,
                                                @Nonnull Supplier<EditorNotificationBuilder> builderFactory);
}