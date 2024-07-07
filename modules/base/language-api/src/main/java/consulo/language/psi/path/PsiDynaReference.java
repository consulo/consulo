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

package consulo.language.psi.path;

import consulo.document.util.TextRange;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class PsiDynaReference<T extends PsiElement> extends PsiReferenceBase<T> implements FileReferenceOwner, PsiPolyVariantReference, EmptyResolveMessageProvider {
  private final List<PsiReference> myReferences = new ArrayList<>();
  private int myChosenOne = -1;
  private ResolveResult[] myCachedResult;

  public PsiDynaReference(final T psiElement) {
    super(psiElement, true);
  }

  public void addReferences(Collection<PsiReference> references) {
    myReferences.addAll(references);
    for (PsiReference reference : references) {
      if (!reference.isSoft()) mySoft = false;
    }
  }

  public List<PsiReference> getReferences() {
    return myReferences;
  }

  public void addReference(PsiReference reference) {
    myReferences.add(reference);
    if (!reference.isSoft()) mySoft = false;
  }

  @Override
  public TextRange getRangeInElement() {

    PsiReference resolved = null;
    PsiReference reference = myReferences.get(0);

    if (reference.resolve() != null) {
      resolved = reference;
    }

    final TextRange range = reference.getRangeInElement();
    int start = range.getStartOffset();
    int end = range.getEndOffset();
    for (int i = 1; i < myReferences.size(); i++) {
      reference = myReferences.get(i);
      final TextRange textRange = getRange(reference);
      start = Math.min(start, textRange.getStartOffset());
      if (resolved == null) {
        end = Math.max(end, textRange.getEndOffset());
      }
    }
    return new TextRange(start, end);
  }

  private TextRange getRange(PsiReference reference) {
    TextRange rangeInElement = reference.getRangeInElement();
    PsiElement element = reference.getElement();
    while (element != myElement) {
      rangeInElement = rangeInElement.shiftRight(element.getStartOffsetInParent());
      element = element.getParent();
      if (element instanceof PsiFile) break;
    }
    return rangeInElement;
  }

  @Override
  public PsiElement resolve() {
    final ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

  @Override
  @Nonnull
  public String getCanonicalText() {
    final PsiReference reference = chooseReference();
    return reference == null ? myReferences.get(0).getCanonicalText() : reference.getCanonicalText();
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    final PsiReference reference = chooseReference();
    if (reference != null) {
      return reference.handleElementRename(newElementName);
    }
    return myElement;
  }

  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    for (PsiReference reference : myReferences) {
      if (reference instanceof FileReference) {
        return reference.bindToElement(element);
      }
    }
    return myElement;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    for (PsiReference reference : myReferences) {
      if (reference.isReferenceTo(element)) return true;
    }
    return false;
  }


  @Override
  @Nonnull
  public Object[] getVariants() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  @Nonnull
  public ResolveResult[] multiResolve(final boolean incompleteCode) {
    if (myCachedResult == null) {
      myCachedResult = innerResolve(incompleteCode);
    }
    return myCachedResult;
  }

  protected ResolveResult[] innerResolve(final boolean incompleteCode) {
    List<ResolveResult> result = new ArrayList<ResolveResult>();
    for (PsiReference reference : myReferences) {
      if (reference instanceof PsiPolyVariantReference) {
        for (ResolveResult rr : ((PsiPolyVariantReference)reference).multiResolve(incompleteCode)) {
          if (rr.isValidResult()) {
            result.add(rr);
          }
        }
      }
      else {
        final PsiElement resolved = reference.resolve();
        if (resolved != null) {
          result.add(new PsiElementResolveResult(resolved));
        }
      }
    }

    return result.toArray(new ResolveResult[result.size()]);
  }

  @Nullable
  private PsiReference chooseReference() {
    if (myChosenOne != -1) {
      return myReferences.get(myChosenOne);
    }
    boolean flag = false;
    for (int i = 0; i < myReferences.size(); i++) {
      final PsiReference reference = myReferences.get(i);
      if (reference.isSoft() && flag) continue;
      if (!reference.isSoft() && !flag) {
        myChosenOne = i;
        flag = true;
        continue;
      }
      if (reference.resolve() != null) {
        myChosenOne = i;
      }
    }
    return myChosenOne >= 0 ? myReferences.get(myChosenOne) : null;
  }

  @Nonnull
  @Override
  public LocalizeValue buildUnresolvedMessaged(@Nonnull String referenceText) {
    final PsiReference reference = chooseReference();

    if (reference instanceof EmptyResolveMessageProvider emptyResolveMessageProvider) {
      return emptyResolveMessageProvider.buildUnresolvedMessaged(referenceText);
    }
    else {
      return LocalizeValue.localizeTODO(PsiBundle.message("cannot.resolve.symbol", referenceText));
    }
  }

  @Override
  public String toString() {
    return "PsiDynaReference containing " + myReferences.toString();
  }

  @Override
  public PsiFileReference getLastFileReference() {
    for (PsiReference reference : myReferences) {
      if (reference instanceof FileReferenceOwner) {
        return ((FileReferenceOwner)reference).getLastFileReference();
      }
    }
    return null;
  }
}
