/*
 * Copyright 2013-2016 consulo.io
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
package consulo.codeInsight;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.extensions.CompositeExtensionPointName;

import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 20.04.2015
 */
public interface TargetElementUtilEx {
  CompositeExtensionPointName<TargetElementUtilEx> EP_NAME =
          CompositeExtensionPointName.applicationPoint("com.intellij.targetElementUtilEx", TargetElementUtilEx.class);

  class Adapter implements TargetElementUtilEx {
    @Override
    public void collectAllAccepted(@Nonnull Set<String> set) {

    }

    @Override
    public void collectDefinitionSearchFlags(@Nonnull Set<String> set) {

    }

    @Override
    public void collectReferenceSearchFlags(@Nonnull Set<String> set) {

    }

    @Override
    public boolean isIdentifierPart(@Nonnull PsiFile file, @Nonnull CharSequence text, int offset) {
      return false;
    }

    @Override
    @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
    public boolean isAcceptableReferencedElement(PsiElement element, @Nonnull PsiElement referenceOrReferencedElement) {
      return true;
    }

    @Nullable
    @Override
    public PsiElement adjustElement(Editor editor, Set<String> flags, PsiElement element, PsiElement contextElement) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement adjustReference(@Nonnull PsiReference ref) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement getReferenceOrReferencedElement(@Nonnull PsiReference reference, @Nonnull Set<String> flags) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement modifyReferenceOrReferencedElement(@Nullable PsiElement refElement,
                                                         @Nonnull PsiFile file,
                                                         @Nonnull Editor editor,
                                                         @Nonnull Set<String> flags,
                                                         int offset) {
      return null;
    }

    @Override
    @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
    public boolean includeSelfInGotoImplementation(@Nonnull PsiElement element) {
      return true;
    }

    @Override
    @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
    public boolean acceptImplementationForReference(PsiReference reference, PsiElement element) {
      return true;
    }

    @Nullable
    @Override
    public PsiElement getGotoDeclarationTarget(PsiElement element, PsiElement navElement) {
      return null;
    }

    @Nullable
    @Override
    public Collection<PsiElement> getTargetCandidates(@Nonnull PsiReference reference) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement getNamedElement(@Nonnull PsiElement element) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement modifyTargetElement(@Nonnull PsiElement element, @Nonnull Set<String> flags) {
      return null;
    }
  }

  @NonNls String REFERENCED_ELEMENT_ACCEPTED = "reference element accepted";
  @NonNls String ELEMENT_NAME_ACCEPTED = "element name accepted";
  @NonNls String LOOKUP_ITEM_ACCEPTED = "lookup item accepted";

  void collectAllAccepted(@Nonnull Set<String> set);

  void collectDefinitionSearchFlags(@Nonnull Set<String> set);

  void collectReferenceSearchFlags(@Nonnull Set<String> set);

  boolean isIdentifierPart(@Nonnull PsiFile file, @Nonnull CharSequence text, int offset);

  @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
  boolean isAcceptableReferencedElement(final PsiElement element, @Nonnull final PsiElement referenceOrReferencedElement);

  @Nullable
  PsiElement adjustElement(final Editor editor, final Set<String> flags, final PsiElement element, final PsiElement contextElement);

  @Nullable
  PsiElement adjustReference(@Nonnull PsiReference ref);

  @Nullable
  PsiElement getReferenceOrReferencedElement(@Nonnull PsiReference reference, @Nonnull Set<String> flags);

  @Nullable
  PsiElement modifyReferenceOrReferencedElement(@Nullable PsiElement refElement,
                                                @Nonnull PsiFile file,
                                                @Nonnull Editor editor,
                                                @Nonnull Set<String> flags,
                                                int offset);

  @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
  boolean includeSelfInGotoImplementation(@Nonnull PsiElement element);

  @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
  boolean acceptImplementationForReference(PsiReference reference, PsiElement element);

  @Nullable
  PsiElement getGotoDeclarationTarget(final PsiElement element, final PsiElement navElement);

  @Nullable
  Collection<PsiElement> getTargetCandidates(@Nonnull PsiReference reference);

  @Nullable
  PsiElement getNamedElement(@Nonnull final PsiElement element);

  @Nullable
  PsiElement modifyTargetElement(@Nonnull PsiElement element, @Nonnull Set<String> flags);
}
