// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

public final class RejectAllIndexingHint implements FileBasedIndex.InputFilter {
    public static final RejectAllIndexingHint INSTANCE = new RejectAllIndexingHint();

    private RejectAllIndexingHint() {
    }

    @Override
    public boolean acceptInput(@Nullable Project project, VirtualFile file) {
        return false;
    }
}
