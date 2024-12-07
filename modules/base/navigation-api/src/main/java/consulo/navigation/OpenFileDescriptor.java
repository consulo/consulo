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
package consulo.navigation;

import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @see OpenFileDescriptorFactory
 * @since 19-Feb-22
 */
public interface OpenFileDescriptor extends Disposable, UserDataHolder, Navigatable, Comparable<OpenFileDescriptor> {
    @Nonnull
    VirtualFile getFile();

    @Nonnull
    ComponentManager getProject();

    int getOffset();

    int getLine();

    int getColumn();

    default boolean isValid() {
        return true;
    }

    default boolean isUseCurrentWindow() {
        return false;
    }

    boolean navigateInEditor(boolean requestFocus);
}
