/*
 * Copyright 2013-2015 must-be.org
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    public void collectAllAccepted(@NotNull Set<String> set) {

    }

    @Override
    public void collectDefinitionSearchFlags(@NotNull Set<String> set) {

    }

    @Override
    public void collectReferenceSearchFlags(@NotNull Set<String> set) {

    }

    @Override
    public boolean isIdentifierPart(@NotNull PsiFile file, @NotNull CharSequence text, int offset) {
      return false;
    }

    @Override
    @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
    public boolean isAcceptableReferencedElement(PsiElement element, @NotNull PsiElement referenceOrReferencedElement) {
      return true;
    }

    @Nullable
    @Override
    public PsiElement adjustElement(Editor editor, Set<String> flags, PsiElement element, PsiElement contextElement) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement adjustReference(@NotNull PsiReference ref) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement getReferenceOrReferencedElement(@NotNull PsiReference reference, @NotNull Set<String> flags) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement modifyReferenceOrReferencedElement(@Nullable PsiElement refElement,
                                                         @NotNull PsiFile file,
                                                         @NotNull Editor editor,
                                                         @NotNull Set<String> flags,
                                                         int offset) {
      return null;
    }

    @Override
    @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
    public boolean includeSelfInGotoImplementation(@NotNull PsiElement element) {
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
    public Collection<PsiElement> getTargetCandidates(@NotNull PsiReference reference) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement getNamedElement(@NotNull PsiElement element) {
      return null;
    }

    @Nullable
    @Override
    public PsiElement modifyTargetElement(@NotNull PsiElement element, @NotNull Set<String> flags) {
      return null;
    }
  }

  @NonNls String REFERENCED_ELEMENT_ACCEPTED = "reference element accepted";
  @NonNls String ELEMENT_NAME_ACCEPTED = "element name accepted";
  @NonNls String LOOKUP_ITEM_ACCEPTED = "lookup item accepted";

  void collectAllAccepted(@NotNull Set<String> set);

  void collectDefinitionSearchFlags(@NotNull Set<String> set);

  void collectReferenceSearchFlags(@NotNull Set<String> set);

  boolean isIdentifierPart(@NotNull PsiFile file, @NotNull CharSequence text, int offset);

  @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
  boolean isAcceptableReferencedElement(final PsiElement element, @NotNull final PsiElement referenceOrReferencedElement);

  @Nullable
  PsiElement adjustElement(final Editor editor, final Set<String> flags, final PsiElement element, final PsiElement contextElement);

  @Nullable
  PsiElement adjustReference(@NotNull PsiReference ref);

  @Nullable
  PsiElement getReferenceOrReferencedElement(@NotNull PsiReference reference, @NotNull Set<String> flags);

  @Nullable
  PsiElement modifyReferenceOrReferencedElement(@Nullable PsiElement refElement,
                                                @NotNull PsiFile file,
                                                @NotNull Editor editor,
                                                @NotNull Set<String> flags,
                                                int offset);

  @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
  boolean includeSelfInGotoImplementation(@NotNull PsiElement element);

  @CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
  boolean acceptImplementationForReference(PsiReference reference, PsiElement element);

  @Nullable
  PsiElement getGotoDeclarationTarget(final PsiElement element, final PsiElement navElement);

  @Nullable
  Collection<PsiElement> getTargetCandidates(@NotNull PsiReference reference);

  @Nullable
  PsiElement getNamedElement(@NotNull final PsiElement element);

  @Nullable
  PsiElement modifyTargetElement(@NotNull PsiElement element, @NotNull Set<String> flags);
}
