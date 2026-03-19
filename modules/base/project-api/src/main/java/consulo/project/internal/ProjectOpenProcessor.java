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
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author max
 */
public abstract class ProjectOpenProcessor {
    public abstract @Nullable Image getIcon(VirtualFile file);

    public abstract boolean canOpenProject(File file);

    
    public abstract AsyncResult<Project> doOpenProjectAsync(VirtualFile virtualFile,
                                                            UIAccess uiAccess,
                                                            ProjectOpenContext context);
}
