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
package consulo.project.internal;

import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author max
 */
public abstract class ProjectOpenProcessor {
    @Nullable
    public abstract Image getIcon(@Nonnull VirtualFile file);

    public abstract boolean canOpenProject(@Nonnull File file);

    @Nonnull
    public abstract AsyncResult<Project> doOpenProjectAsync(@Nonnull VirtualFile virtualFile,
                                                            @Nonnull UIAccess uiAccess,
                                                            @Nonnull ProjectOpenContext context);
}
