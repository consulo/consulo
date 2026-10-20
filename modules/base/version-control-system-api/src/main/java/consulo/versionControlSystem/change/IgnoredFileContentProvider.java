// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.versionControlSystem.VcsKey;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Builds the content of the native VCS ignore files (e.g. .gitignore, .hgignore).
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface IgnoredFileContentProvider {
    VcsKey getSupportedVcs();

    String getFileName();

    String buildIgnoreFileContent(VirtualFile ignoreFileRoot, IgnoredFileProvider[] ignoredFileProviders);

    String buildUnignoreContent(String ignorePattern);

    String buildIgnoreEntryContent(VirtualFile ignoreEntryRoot, IgnoredFileDescriptor ignoredFileDescriptor);

    String buildIgnoreGroupDescription(IgnoredFileProvider ignoredFileProvider);

    default boolean supportIgnoreFileNotInVcsRoot() {
        return true;
    }

    default boolean canCreateIgnoreFileInStateStoreDir() {
        return true;
    }
}
