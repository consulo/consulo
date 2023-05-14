/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiManager;
import consulo.language.util.CharTable;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jul-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ASTElementFactory {
  @Nonnull
  static ASTElementFactory getInstance(Project project) {
    return project.getInstance(ASTElementFactory.class);
  }

  @Nonnull
  ASTNode createSingleLeafElement(@Nonnull IElementType type, CharSequence buffer, int startOffset, int endOffset, @Nullable CharTable table, PsiManager manager);
}
