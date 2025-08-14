// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.content.scope;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import jakarta.annotation.Nonnull;

public class FilteredNamedScope extends NamedScope {
    public FilteredNamedScope(@Nonnull String name, LocalizeValue presentableName, @Nonnull Image icon, int priority, @Nonnull VirtualFileFilter filter) {
        super(name, presentableName, icon, new FilteredPackageSet(name, priority) {
            @Override
            public boolean contains(@Nonnull VirtualFile file, @Nonnull Project project) {
                return filter.accept(file);
            }
        });
    }
}
