/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.impl.ast;

import consulo.language.ast.IElementType;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.DummyHolder;
import consulo.language.impl.psi.DummyHolderFactory;
import consulo.language.impl.psi.PsiErrorElementImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.CharTable;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class Factory  {
  private Factory() {}

  @Nonnull
  public static LeafElement createSingleLeafElement(@Nonnull IElementType type, CharSequence buffer, int startOffset, int endOffset, CharTable table, PsiManager manager, PsiFile originalFile) {
    DummyHolder dummyHolder = DummyHolderFactory.createHolder(manager, table, type.getLanguage());
    dummyHolder.setOriginalFile(originalFile);

    FileElement holderElement = dummyHolder.getTreeElement();

    LeafElement newElement = ASTFactory.leaf(type, holderElement.getCharTable().intern(
            buffer, startOffset, endOffset));
    holderElement.rawAddChildren(newElement);
    CodeEditUtil.setNodeGenerated(newElement, true);
    return newElement;
  }

  @Nonnull
  public static LeafElement createSingleLeafElement(@Nonnull IElementType type, CharSequence buffer, int startOffset, int endOffset, CharTable table, PsiManager manager, boolean generatedFlag) {
    FileElement holderElement = DummyHolderFactory.createHolder(manager, table, type.getLanguage()).getTreeElement();
    LeafElement newElement = ASTFactory.leaf(type, holderElement.getCharTable().intern(
            buffer, startOffset, endOffset));
    holderElement.rawAddChildren(newElement);
    if(generatedFlag) CodeEditUtil.setNodeGenerated(newElement, true);
    return newElement;
  }

  @Nonnull
  public static LeafElement createSingleLeafElement(@Nonnull IElementType type, CharSequence buffer, CharTable table, PsiManager manager) {
    return createSingleLeafElement(type, buffer, 0, buffer.length(), table, manager);
  }

  @Nonnull
  public static LeafElement createSingleLeafElement(@Nonnull IElementType type, CharSequence buffer, int startOffset, int endOffset, @Nullable CharTable table, PsiManager manager) {
    return createSingleLeafElement(type, buffer, startOffset, endOffset, table, manager, true);
  }

  @Nonnull
  public static CompositeElement createErrorElement(@Nonnull LocalizeValue description) {
    return new PsiErrorElementImpl(description);
  }

  @Nonnull
  public static CompositeElement createCompositeElement(@Nonnull IElementType type,
                                                        CharTable charTableByTree,
                                                        PsiManager manager) {
    FileElement treeElement = DummyHolderFactory.createHolder(manager, null, charTableByTree).getTreeElement();
    CompositeElement composite = ASTFactory.composite(type);
    treeElement.rawAddChildren(composite);
    return composite;
  }
}
