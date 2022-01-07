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
package com.intellij.psi.search.scope;

import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.scope.packageSet.AbstractPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.Colored;
import com.intellij.util.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
@Colored(color = "ffffe4", darkVariant = "494539")
public class NonProjectFilesScope extends NamedScope {
  public static final String NAME = "Non-Project Files";

  public NonProjectFilesScope() {
    super(NAME, new AbstractPackageSet("NonProject") {
      @Override
      public boolean contains(VirtualFile file, @Nonnull Project project, @Nullable NamedScopesHolder holder) {
        // do not include fake-files e.g. fragment-editors, database consoles, etc.
        if (file.getFileSystem() instanceof NonPhysicalFileSystem) return false;
        if (!file.isInLocalFileSystem()) return true;
        if (ScratchUtil.isScratch(file)) return false;
        return !ProjectScope.getProjectScope(project).contains(file);
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
