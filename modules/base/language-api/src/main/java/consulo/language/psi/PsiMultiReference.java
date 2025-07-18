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

package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.util.IncorrectOperationException;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author ik
 * @since 2003-04-02
 */
public class PsiMultiReference implements PsiPolyVariantReference {
  public static final Comparator<PsiReference> COMPARATOR = new Comparator<PsiReference>() {
    @Override
    public int compare(final PsiReference ref1, final PsiReference ref2) {
      if (ref1.isSoft() && !ref2.isSoft()) return 1;
      if (!ref1.isSoft() && ref2.isSoft()) return -1;

      boolean resolves1 = resolves(ref1);
      boolean resolves2 = resolves(ref2);
      if (resolves1 && !resolves2) return -1;
      if (!resolves1 && resolves2) return 1;

      final TextRange range1 = ref1.getRangeInElement();
      final TextRange range2 = ref2.getRangeInElement();

      if(TextRange.areSegmentsEqual(range1, range2)) return 0;
      if(range1.getStartOffset() >= range2.getStartOffset() && range1.getEndOffset() <= range2.getEndOffset()) return -1;
      if(range2.getStartOffset() >= range1.getStartOffset() && range2.getEndOffset() <= range1.getEndOffset()) return 1;

      return 0;
    }
  };

  private static boolean resolves(final PsiReference ref1) {
    return ref1 instanceof PsiPolyVariantReference && ((PsiPolyVariantReference)ref1).multiResolve(false).length > 0 || ref1.resolve() != null;
  }

  private final PsiReference[] myReferences;
  private final PsiElement myElement;
  private boolean mySorted;

  public PsiMultiReference(@Nonnull PsiReference[] references, PsiElement element){
    assert references.length > 0;
    myReferences = references;
    myElement = element;
  }

  public PsiReference[] getReferences() {
    return myReferences;
  }

  private synchronized PsiReference chooseReference(){
    if (!mySorted) {
      Arrays.sort(myReferences, COMPARATOR);
      mySorted = true;
    }
    return myReferences[0];
  }

  @RequiredReadAction
  @Override
  public PsiElement getElement(){
    return myElement;
  }

  @Nonnull
  @RequiredReadAction
  @Override
  public TextRange getRangeInElement(){
    final PsiReference chosenRef = chooseReference();
    TextRange rangeInElement = chosenRef.getRangeInElement();
    PsiElement element = chosenRef.getElement();
    while(element != myElement) {
      rangeInElement = rangeInElement.shiftRight(element.getStartOffsetInParent());
      element = element.getParent();
      if (element instanceof PsiFile) break;
    }
    return rangeInElement;
  }

  @RequiredReadAction
  @Override
  public PsiElement resolve(){
    final PsiReference reference = chooseReference();
    if (cannotChoose()) {
      final ResolveResult[] results = multiResolve(false);
      return results.length == 1 ? results[0].getElement() : null;
    }
    return reference.resolve();
  }

  private boolean cannotChoose() {
    return myReferences.length > 1 && COMPARATOR.compare(myReferences[0], myReferences[1]) == 0;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public String getCanonicalText(){
    return chooseReference().getCanonicalText();
  }

  @RequiredWriteAction
  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException{
    return chooseReference().handleElementRename(newElementName);
  }

  @RequiredWriteAction
  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException{
    return chooseReference().bindToElement(element);
  }

  @RequiredReadAction
  @Override
  public boolean isReferenceTo(PsiElement element){
    for (PsiReference reference : myReferences) {
      if (reference.isReferenceTo(element)) return true;
    }
    return false;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Object[] getVariants() {
    Set<Object> variants = new HashSet<Object>();
    for(PsiReference ref: myReferences) {
      Object[] refVariants = ref.getVariants();
      ContainerUtil.addAll(variants, refVariants);
    }
    return variants.toArray();
  }

  @RequiredReadAction
  @Override
  public boolean isSoft(){
    for (PsiReference reference : getReferences()) {
      if (!reference.isSoft()) {
        return false;
      }
    }
    return true;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public ResolveResult[] multiResolve(final boolean incompleteCode) {
    final PsiReference[] refs = getReferences();
    Collection<ResolveResult> result = new LinkedHashSet<ResolveResult>(refs.length);
    PsiElementResolveResult selfReference = null;
    for (PsiReference reference : refs) {
      if (reference instanceof PsiPolyVariantReference) {
        ContainerUtil.addAll(result, ((PsiPolyVariantReference)reference).multiResolve(incompleteCode));
      }
      else {
        final PsiElement resolved = reference.resolve();
        if (resolved != null) {
          final PsiElementResolveResult rresult = new PsiElementResolveResult(resolved);
          if (getElement() == resolved) {
            selfReference = rresult;
          } else {
            result.add(rresult);
          }
        }
      }
    }

    if (result.isEmpty() && selfReference != null) {
      result.add(selfReference); // if i the only one starring at the sun
    }

    return result.toArray(new ResolveResult[result.size()]);
  }

  @Override
  public String toString() {
    return "PsiMultiReference{myReferences=" + Arrays.toString(myReferences) + '}';
  }
}
