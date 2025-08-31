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

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;

import jakarta.annotation.Nonnull;

public abstract class LightElement extends PsiElementBase implements LightweightPsiElement {
  protected final PsiManager myManager;
  private final Language myLanguage;
  private volatile PsiElement myNavigationElement = this;

  protected LightElement(PsiManager manager, Language language) {
    myManager = manager;
    myLanguage = language;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Language getLanguage() {
    return myLanguage;
  }

  @Override
  public PsiManager getManager() {
    return myManager;
  }

  @Override
  public PsiElement getParent() {
    return null;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  public PsiFile getContainingFile() {
    return null;
  }

  @RequiredReadAction
  @Override
  public TextRange getTextRange() {
    return TextRange.EMPTY_RANGE;
  }

  @RequiredReadAction
  @Override
  public int getStartOffsetInParent() {
    return -1;
  }

  @RequiredReadAction
  @Override
  public final int getTextLength() {
    String text = getText();
    return text != null ? text.length() : 0;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public char[] textToCharArray() {
    return getText().toCharArray();
  }

  @Override
  public boolean textMatches(@Nonnull CharSequence text) {
    return getText().equals(text.toString());
  }

  @Override
  public boolean textMatches(@Nonnull PsiElement element) {
    return getText().equals(element.getText());
  }

  @RequiredReadAction
  @Override
  public PsiElement findElementAt(int offset) {
    return null;
  }

  @Override
  public int getTextOffset() {
    return -1;
  }

  @Override
  public boolean isValid() {
    PsiElement navElement = getNavigationElement();
    if (navElement != this) {
      return navElement.isValid();
    }

    return true;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isPhysical() {
    return false;
  }

  public abstract String toString();

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public void delete() throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    throw new IncorrectOperationException(getClass().getName());
  }

  @Override
  public ASTNode getNode() {
    return null;
  }

  @RequiredReadAction
  @Override
  public String getText() {
    return null;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
  }

  @Override
  public PsiElement copy() {
    return null;
  }

  @Nonnull
  @Override
  public PsiElement getNavigationElement() {
    return myNavigationElement;
  }

  public void setNavigationElement(@Nonnull PsiElement navigationElement) {
    PsiElement nnElement = navigationElement.getNavigationElement();
    if (nnElement != navigationElement && nnElement != null) {
      navigationElement = nnElement;
    }
    myNavigationElement = navigationElement;
  }

  @RequiredReadAction
  @Override
  public PsiElement getPrevSibling() {
    return null;
  }

  @RequiredReadAction
  @Override
  public PsiElement getNextSibling() {
    return null;
  }

}
