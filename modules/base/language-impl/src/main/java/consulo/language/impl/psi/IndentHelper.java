/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.impl.psi;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@Deprecated
@DeprecationInfo("Prefer CodeStyle api")
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class IndentHelper {
  public static IndentHelper getInstance() {
    return Application.get().getInstance(IndentHelper.class);
  }

  /**
   * @deprecated Use {@link #getIndent(PsiFile, ASTNode)}
   */
  @Deprecated
  public final int getIndent(Project project, FileType fileType, ASTNode element) {
    return getIndent(getFile(element), element);
  }

  /**
   * @deprecated Use {@link #getIndent(PsiFile, ASTNode, boolean)}
   */
  @Deprecated
  public final int getIndent(Project project, FileType fileType, ASTNode element, boolean includeNonSpace) {
    return getIndent(getFile(element), element, includeNonSpace);
  }

  private static PsiFile getFile(ASTNode element) {
    return element.getPsi().getContainingFile();
  }

  public abstract int getIndent(@Nonnull PsiFile file, @Nonnull ASTNode element);

  public abstract int getIndent(@Nonnull PsiFile file, @Nonnull ASTNode element, boolean includeNonSpace);

  @Deprecated
  public abstract String fillIndent(Project project, FileType fileType, int indent);

  @Deprecated
  public abstract int getIndent(Project project, FileType fileType, String text, boolean includeNonSpace);
}
