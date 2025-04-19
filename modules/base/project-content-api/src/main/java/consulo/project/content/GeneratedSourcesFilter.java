/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.project.content;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface GeneratedSourcesFilter {
    @RequiredReadAction
    boolean isGeneratedSource(@Nonnull VirtualFile file);

    @RequiredReadAction
    public static boolean isGeneratedSourceByAnyFilter(@Nonnull VirtualFile file, @Nonnull Project project) {
        if (project.isDisposed() || !file.isValid()) {
            return false;
        }

        ExtensionPoint<GeneratedSourcesFilter> point = project.getExtensionPoint(GeneratedSourcesFilter.class);
        return point.findFirstSafe(it -> it.isGeneratedSource(file)) != null;
    }

    @RequiredReadAction
    @Deprecated
    public static boolean isGenerated(@Nonnull Project project, @Nonnull VirtualFile file) {
        return isGeneratedSourceByAnyFilter(file, project);
    }
}
