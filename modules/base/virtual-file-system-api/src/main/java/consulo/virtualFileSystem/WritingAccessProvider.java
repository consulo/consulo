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
package consulo.virtualFileSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionPointName;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.PROJECT)
public abstract class WritingAccessProvider {
    private static final ExtensionPointName<WritingAccessProvider> EP_NAME = ExtensionPointName.create(WritingAccessProvider.class);

    /**
     * @param files files to be checked
     * @return set of files that cannot be accessed
     */
    @Nonnull
    public abstract Collection<VirtualFile> requestWriting(VirtualFile... files);

    public abstract boolean isPotentiallyWritable(@Nonnull VirtualFile file);

    @Nonnull
    public static List<WritingAccessProvider> getProvidersForProject(@Nullable ComponentManager project) {
        return project == null ? List.of() : EP_NAME.getExtensionList(project);
    }

    public static boolean isPotentiallyWritable(@Nonnull VirtualFile file, @Nullable ComponentManager project) {
        List<WritingAccessProvider> providers = getProvidersForProject(project);
        for (WritingAccessProvider provider : providers) {
            if (!provider.isPotentiallyWritable(file)) {
                return false;
            }
        }
        return true;
    }
}
