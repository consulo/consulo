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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiFile;
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
public abstract class FileColorManager {
    public static FileColorManager getInstance(@Nonnull Project project) {
        return project.getInstance(FileColorManager.class);
    }

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean enabled);

    public abstract boolean isEnabledForTabs();

    public abstract boolean isEnabledForProjectView();

    public abstract Project getProject();

    @Nullable
    public abstract Color getColor(@Nonnull String name);

    public abstract Collection<String> getColorNames();

    @Nullable
    public abstract Color getFileColor(@Nonnull PsiFile file);

    @Deprecated
    public abstract Color getFileColor(@Nonnull VirtualFile file);

    @Nullable
    public ColorValue getFileColorValue(@Nonnull VirtualFile file) {
        return TargetAWT.from(getFileColor(file));
    }

    @Nullable
    public abstract Color getScopeColor(@Nonnull String scopeName);

    public abstract boolean isShared(@Nonnull String scopeName);

    public abstract boolean isColored(@Nonnull String scopeName, boolean shared);

    @Nullable
    @RequiredReadAction
    public abstract Color getRendererBackground(VirtualFile file);

    @Nullable
    public abstract Color getRendererBackground(PsiFile file);
}
