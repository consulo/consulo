/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.impl.psi.impl.source.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-09-26
 */
@Extension(ComponentScope.APPLICATION)
public interface IndentHelperExtension {
  ExtensionPointName<IndentHelperExtension> EP_NAME = ExtensionPointName.create(IndentHelperExtension.class);

  int TOO_BIG_WALK_THRESHOLD = 450;

  boolean isAvailable(@Nonnull PsiFile file);

  @RequiredReadAction
  int getIndentInner(@Nonnull PsiFile file, @Nonnull final ASTNode element, boolean includeNonSpace, int recursionLevel);
}
