/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.folding;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.project.DumbService;

/**
 * @author yole
 * @author Konstantin Bulenkov
 */
public class LanguageFolding {
  @RequiredReadAction
  public static FoldingDescriptor[] buildFoldingDescriptors(FoldingBuilder builder, PsiElement root, Document document, boolean quick) {
    if (!DumbService.isDumbAware(builder) && DumbService.getInstance(root.getProject()).isDumb()) {
      return FoldingDescriptor.EMPTY;
    }

    if (builder instanceof FoldingBuilderEx) {
      return ((FoldingBuilderEx)builder).buildFoldRegions(root, document, quick);
    }
    final ASTNode astNode = root.getNode();
    if (astNode == null || builder == null) {
      return FoldingDescriptor.EMPTY;
    }

    return builder.buildFoldRegions(astNode, document);
  }
}
