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

package consulo.ide.impl.idea.ide.structureView.impl;

import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.language.Language;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.template.TemplateLanguageFileViewProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
public class StructureViewElementWrapper<V extends PsiElement> implements StructureViewTreeElement {
  private final StructureViewTreeElement myTreeElement;
  private final PsiFile myMainFile;

  public StructureViewElementWrapper(@Nonnull StructureViewTreeElement treeElement, @Nonnull PsiFile mainFile) {
    myTreeElement = treeElement;
    myMainFile = mainFile;
  }

  public StructureViewTreeElement getWrappedElement() {
    return myTreeElement;
  }

  @Override
  public V getValue() {
    return (V)myTreeElement.getValue();
  }

  @Override
  public StructureViewTreeElement[] getChildren() {
    TreeElement[] baseChildren = myTreeElement.getChildren();
    List<StructureViewTreeElement> result = new ArrayList<StructureViewTreeElement>();
    for (TreeElement element : baseChildren) {
      StructureViewTreeElement wrapper = new StructureViewElementWrapper((StructureViewTreeElement)element, myMainFile);

      result.add(wrapper);
    }
    return result.toArray(new StructureViewTreeElement[result.size()]);
  }

  @Override
  public ItemPresentation getPresentation() {
    return myTreeElement.getPresentation();
  }

  @Override
  public void navigate(boolean requestFocus) {
    Navigatable navigatable = getNavigatableInTemplateLanguageFile();
    if (navigatable != null) {
      navigatable.navigate(requestFocus);
    }
  }

  @Nullable
  private Navigatable getNavigatableInTemplateLanguageFile() {
    PsiElement element = (PsiElement)myTreeElement.getValue();
    if (element == null) return null;

    int offset = element.getTextRange().getStartOffset();
    Language dataLanguage = ((TemplateLanguageFileViewProvider)myMainFile.getViewProvider()).getTemplateDataLanguage();
    PsiFile dataFile = myMainFile.getViewProvider().getPsi(dataLanguage);
    if (dataFile == null) return null;

    PsiElement tlElement = dataFile.findElementAt(offset);
    while(true) {
      if (tlElement == null || tlElement.getTextRange().getStartOffset() != offset) break;
      if (tlElement instanceof Navigatable) {
        return (Navigatable)tlElement;
      }
      tlElement = tlElement.getParent();
    }
    return null;
  }

  @Override
  public boolean canNavigate() {
    return getNavigatableInTemplateLanguageFile() != null;
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }
}
