// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.scratch;

import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.ide.localize.IdeLocalize;
import consulo.language.scratch.RootType;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ScratchesNamedScope extends NamedScope {
    public static final String ID = "Scratches and Consoles";

    public ScratchesNamedScope() {
        super(ID, IdeLocalize.scratchesAndConsoles(), PlatformIconGroup.scopeScratches(), new AbstractPackageSet(IdeLocalize.scratchesAndConsoles().get()) {
            @Override
            public boolean contains(@Nonnull VirtualFile file, @Nonnull Project project, @Nullable NamedScopesHolder holder) {
                return ScratchesNamedScope.contains(project, file);
            }
        });
    }

    public static boolean contains(@Nonnull Project project, @Nonnull VirtualFile file) {
        RootType rootType = RootType.forFile(file);
        return rootType != null && !(rootType.isHidden() || rootType.isIgnored(project, file));
    }
}
