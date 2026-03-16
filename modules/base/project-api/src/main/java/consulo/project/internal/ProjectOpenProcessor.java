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

import consulo.project.ProjectOpenContext;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author max
 */
public abstract class ProjectOpenProcessor {
    @Nullable
    public abstract Image getIcon(VirtualFile file);

    public abstract boolean canOpenProject(File file);

    /**
     * Extend the coroutine chain with preparation steps for opening a project.
     * The chain receives a {@link VirtualFile} (the project directory) as input
     * and must return a {@link VirtualFile} as output (typically the same one).
     * <p>
     * Processors should add steps to create directory structure (.consulo/, modules, etc.)
     * or show import wizards. The actual project opening is handled by the service.
     * <p>
     * Default implementation: no-op (returns chain unchanged).
     *
     * @param uiAccess UI access for EDT operations
     * @param context  project open context with configuration keys
     * @param in       the incoming coroutine chain ending with VirtualFile
     * @return the extended coroutine chain
     */
    public <I> Coroutine<I, VirtualFile> prepareSteps(UIAccess uiAccess,
                                                       ProjectOpenContext context,
                                                       Coroutine<I, VirtualFile> in) {
        return in;
    }
}
