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
package consulo.language.editor.scope;

import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class NonProjectFilesScope extends NamedScope {
    public static final String ID = "Non-Project Files";
    @Deprecated
    public static final String NAME = ID;

    public NonProjectFilesScope() {
        super(ID, LocalizeValue.localizeTODO("Non-Project Files"), new AbstractPackageSet("NonProject") {
            @Override
            public boolean contains(VirtualFile file, @Nonnull Project project, @Nullable NamedScopesHolder holder) {
                // do not include fake-files e.g. fragment-editors, database consoles, etc.
                if (file.getFileSystem() instanceof NonPhysicalFileSystem) {
                    return false;
                }
                if (!file.isInLocalFileSystem()) {
                    return true;
                }
                if (ScratchUtil.isScratch(file)) {
                    return false;
                }
                return !ProjectScopes.getProjectScope(project).contains(file);
            }
        });
    }

    @Override
    public String getDefaultColorName() {
        return "Yellow";
    }

    @Nonnull
    public static NamedScope[] removeFromList(@Nonnull NamedScope[] scopes) {
        int nonProjectIdx = -1;
        for (int i = 0, length = scopes.length; i < length; i++) {
            NamedScope scope = scopes[i];
            if (scope instanceof NonProjectFilesScope) {
                nonProjectIdx = i;
                break;
            }
        }
        if (nonProjectIdx > -1) {
            scopes = ArrayUtil.remove(scopes, nonProjectIdx);
        }
        return scopes;
    }
}
