/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.idea.ui;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.fileEditor.FileEditor;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 12.11.2015
 */
@ExtensionAPI(ComponentScope.PROJECT)
@Deprecated(forRemoval = true)
@DeprecationInfo("Use consulo.fileEditor.EditorNotificationProvider")
public interface EditorNotificationProvider<T extends JComponent> {
  ExtensionPointName<EditorNotificationProvider> EP_NAME = ExtensionPointName.create(EditorNotificationProvider.class);

  @Nonnull
  @Deprecated
  @DeprecationInfo("Don't implement this method")
  default Key<T> getKey() {
    return EditorNotificationProviderKeyCache.getOrCreate(getClass());
  }

  @Nullable
  @RequiredReadAction
  T createNotificationPanel(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor);
}