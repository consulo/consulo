/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.refactoring.changeSignature;

import consulo.application.ApplicationManager;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.ParameterInfo;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.refactoring.rename.RenameProcessor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
* @author anna
* @since 2010-10-29
*/
public abstract class RenameChangeInfo implements ChangeInfo {
  private final PsiFile myFile;
  private final int myOffset;
  private final String myOldName;

  public RenameChangeInfo(final PsiNameIdentifierOwner namedElement, final ChangeInfo oldInfo) {
    myOldName = oldInfo instanceof RenameChangeInfo ? ((RenameChangeInfo)oldInfo).getOldName() : namedElement.getName();
    myFile = namedElement.getContainingFile();
    myOffset = namedElement.getTextOffset();
  }

  @Nonnull
  @Override
  public ParameterInfo[] getNewParameters() {
    return new ParameterInfo[0];
  }

  @Override
  public boolean isParameterSetOrOrderChanged() {
    return false;
  }

  @Override
  public boolean isParameterTypesChanged() {
    return false;
  }

  @Override
  public boolean isParameterNamesChanged() {
    return false;
  }

  @Override
  public boolean isGenerateDelegate() {
    return false;
  }

  @Override
  public boolean isNameChanged() {
    return true;
  }

  @Override
  public PsiElement getMethod() {
    return getNamedElement();
  }

  @Override
  public boolean isReturnTypeChanged() {
    return false;
  }

  @Override
  public String getNewName() {
    final PsiNameIdentifierOwner nameOwner = getNamedElement();
    return nameOwner != null ? nameOwner.getName() : null;
  }

  public String getOldName() {
    return myOldName;
  }

  @Nullable
  public PsiNameIdentifierOwner getNamedElement() {
    return PsiTreeUtil.getParentOfType(myFile.findElementAt(myOffset), PsiNameIdentifierOwner.class);
  }

  public void perform() {
    final PsiNameIdentifierOwner element = getNamedElement();
    if (element != null) {
      final String name = element.getName();
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          element.setName(myOldName);
        }
      });
      new RenameProcessor(element.getProject(), element, name, false, false).run();
    }
  }

  @Nullable
  public PsiElement getNameIdentifier() {
    final PsiNameIdentifierOwner namedElement = getNamedElement();
    return namedElement != null ? namedElement.getNameIdentifier() : null;
  }
}
