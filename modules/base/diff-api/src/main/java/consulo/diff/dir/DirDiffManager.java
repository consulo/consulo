/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.diff.dir;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class DirDiffManager {
  public static DirDiffManager getInstance(@Nonnull Project project) {
    return project.getInstance(DirDiffManager.class);
  }
  
  public abstract void showDiff(@Nonnull DiffElement dir1, @Nonnull DiffElement dir2, DirDiffSettings settings, @Nullable Runnable onWindowClose);

  public abstract void showDiff(@Nonnull DiffElement dir1, @Nonnull DiffElement dir2, DirDiffSettings settings);

  public abstract void showDiff(@Nonnull DiffElement dir1, @Nonnull DiffElement dir2);

  public abstract boolean canShow(@Nonnull DiffElement dir1, @Nonnull DiffElement dir2);

  @Nullable
  public abstract DiffElement createDiffElement(Object obj);

  public abstract DirDiffModel createDiffModel(DiffElement e1, DiffElement e2, DirDiffSettings settings);
}
