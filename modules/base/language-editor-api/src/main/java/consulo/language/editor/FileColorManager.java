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
import consulo.language.psi.PsiFile;
import consulo.project.Project;
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
  public static FileColorManager getInstance(@Nonnull final Project project) {
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
  public abstract Color getFileColor(@Nonnull final PsiFile file);

  public abstract Color getFileColor(@Nonnull final VirtualFile file);

  @Nullable
  public abstract Color getScopeColor(@Nonnull String scopeName);

  public abstract boolean isShared(@Nonnull final String scopeName);

  public abstract boolean isColored(@Nonnull String scopeName, final boolean shared);

  @Nullable
  public abstract Color getRendererBackground(VirtualFile file);

  @Nullable
  public abstract Color getRendererBackground(PsiFile file);
}
