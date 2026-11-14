// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.psi.PsiDirectory;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Implement this contributor to add useful and often used directories in the 'Create Directory' popup.<br>
 * E.g. Gradle or Maven could add conventional source directories ('src/main/java', 'src/main/test')
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CreateDirectoryCompletionContributor {
    /**
     * @return A short description for the suggested variants, to be shown as a group's title in the completion list,
     * e.g. 'Gradle Source Sets', 'Maven Source Directories'.
     */
    LocalizeValue getDescription();

    /**
     * @return completion subdirectory variant for the selected directory
     */
    @RequiredReadAction
    Collection<Variant> getVariants(PsiDirectory directory);

    final class Variant {
        private final String myPath;
        private final @Nullable ContentFolderTypeProvider myRootType;
        private final @Nullable Image myIcon;

        public Variant(String path, @Nullable ContentFolderTypeProvider rootType) {
            this(path, rootType, null);
        }

        /**
         * @param path     absolute or relative path to a directory
         * @param rootType root type with which the created directory will be marked automatically marked
         */
        public Variant(String path, @Nullable ContentFolderTypeProvider rootType, @Nullable Image icon) {
            myPath = path;
            myRootType = rootType;
            myIcon = icon;
        }

        public String getPath() {
            return myPath;
        }

        public @Nullable ContentFolderTypeProvider getRootType() {
            return myRootType;
        }

        public @Nullable Image getIcon() {
            return myIcon;
        }
    }
}
