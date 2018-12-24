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
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

public abstract class ProjectOpenProcessor {
  @Nonnull
  @Language("HTML")
  public String getFileSample() {
    return "";
  }

  @Nonnull
  public abstract Image getIcon();

  @Nullable
  public Image getIcon(final VirtualFile file) {
    return getIcon();
  }

  public abstract boolean canOpenProject(@Nonnull File file);

  public void doOpenProjectAsync(@Nonnull AsyncResult<Project> asyncResult,
                                 @Nonnull VirtualFile virtualFile,
                                 @Nullable Project projectToClose,
                                 boolean forceOpenInNewFrame,
                                 @Nonnull UIAccess uiAccess) {
    throw new AbstractMethodError();
  }

  public void refreshProjectFiles(File basedir) {
  }
}
