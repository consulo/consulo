/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Collection;

/**
 * @author spleaner
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface FileColorManager {
    static FileColorManager getInstance(@Nonnull Project project) {
        return project.getInstance(FileColorManager.class);
    }

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isEnabledForTabs();

    boolean isEnabledForProjectView();

    @Nullable
    Color getColor(@Nonnull String name);

    Collection<String> getColorNames();

    @Deprecated
    Color getFileColor(@Nonnull VirtualFile file);

    @Nullable
    default ColorValue getFileColorValue(@Nonnull VirtualFile file) {
        return TargetAWT.from(getFileColor(file));
    }

    @Nullable
    Color getScopeColor(@Nonnull String scopeName);

    boolean isShared(@Nonnull String scopeName);

    boolean isColored(@Nonnull String scopeName, boolean shared);

    @Nullable
    Color getRendererBackground(VirtualFile file);
}
