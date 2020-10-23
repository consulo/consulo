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

/*
 * @author max
 */
package com.intellij.projectImport;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Consumer;

public abstract class ProjectOpenProcessor {
  @Nonnull
  @Language("HTML")
  public String getFileSample() {
    return "";
  }

  public void collectFileSamples(@Nonnull Consumer<String> fileSamples) {
    fileSamples.accept(getFileSample());
  }

  @Nullable
  public abstract Image getIcon(@Nonnull VirtualFile file);

  public abstract boolean canOpenProject(@Nonnull File file);

  @Nonnull
  public abstract AsyncResult<Project> doOpenProjectAsync(@Nonnull VirtualFile virtualFile, @Nonnull UIAccess uiAccess);
}
