/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.language.ast.ASTNode;
import consulo.codeEditor.FoldingGroup;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public class NamedFoldingDescriptor extends FoldingDescriptor {
  private final String myPlaceholderText;

  public NamedFoldingDescriptor(PsiElement e, int start, int end, @Nullable FoldingGroup group, String placeholderText) {
    this(e.getNode(), new TextRange(start, end), group, placeholderText);
  }

  public NamedFoldingDescriptor(ASTNode node, int start, int end, @Nullable FoldingGroup group, String placeholderText) {
    this(node, new TextRange(start, end), group, placeholderText);
  }

  public NamedFoldingDescriptor(ASTNode node,
                                TextRange range,
                                @Nullable FoldingGroup group,
                                String placeholderText) {
    super(node, range, group);
    myPlaceholderText = placeholderText;
  }

  @Override
  
  public String getPlaceholderText() {
    return myPlaceholderText;
  }
}
