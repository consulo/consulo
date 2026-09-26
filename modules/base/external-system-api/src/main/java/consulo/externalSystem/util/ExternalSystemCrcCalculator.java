// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Calculates CRC for build scripts of applicable build systems.
 * Used to provide custom CRC calculation rules for scripts.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemCrcCalculator {
    /**
     * Checks that CRC calculation is applicable to defined build system {@code systemId} and build script {@code file}.
     */
    boolean isApplicable(ProjectSystemId systemId, VirtualFile file);

    /**
     * Calculates CRC for {@code fileText}. {@code file} content may be not equal to {@code fileText},
     * for example {@code fileText} can be taken from corresponding {@link consulo.document.Document}.
     */
    @Nullable Long calculateCrc(Project project, VirtualFile file, CharSequence fileText);

    static @Nullable ExternalSystemCrcCalculator getInstance(ProjectSystemId systemId, VirtualFile file) {
        return Application.get()
            .getExtensionPoint(ExternalSystemCrcCalculator.class)
            .findFirstSafe(it -> it.isApplicable(systemId, file));
    }
}
