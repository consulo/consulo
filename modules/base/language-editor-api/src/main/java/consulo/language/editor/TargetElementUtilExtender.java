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
package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionList;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 20.04.2015
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TargetElementUtilExtender {
  ExtensionList<TargetElementUtilExtender, Application> EP = ExtensionList.of(TargetElementUtilExtender.class);

  String REFERENCED_ELEMENT_ACCEPTED = "reference element accepted";
  String ELEMENT_NAME_ACCEPTED = "element name accepted";
  String LOOKUP_ITEM_ACCEPTED = "lookup item accepted";

  default void collectAllAccepted(@Nonnull Set<String> set) {
  }

  default void collectDefinitionSearchFlags(@Nonnull Set<String> set) {
  }

  default void collectReferenceSearchFlags(@Nonnull Set<String> set) {
  }

  default boolean isIdentifierPart(@Nonnull PsiFile file, @Nonnull CharSequence text, int offset) {
    return false;
  }

  default boolean isAcceptableReferencedElement(final PsiElement element, @Nonnull final PsiElement referenceOrReferencedElement) {
    return true;
  }

  @Nullable
  default PsiElement adjustElement(final Editor editor, final Set<String> flags, final PsiElement element, final PsiElement contextElement) {
    return null;
  }

  @Nullable
  default PsiElement adjustReference(@Nonnull PsiReference ref) {
    return null;
  }

  @Nullable
  default PsiElement getReferenceOrReferencedElement(@Nonnull PsiReference reference, @Nonnull Set<String> flags) {
    return null;
  }

  @Nullable
  default PsiElement modifyReferenceOrReferencedElement(@Nullable PsiElement refElement, @Nonnull PsiFile file, @Nonnull Editor editor, @Nonnull Set<String> flags, int offset) {
    return null;
  }

  default boolean includeSelfInGotoImplementation(@Nonnull PsiElement element) {
    return true;
  }

  default boolean acceptImplementationForReference(PsiReference reference, PsiElement element) {
    return true;
  }

  @Nullable
  default PsiElement getGotoDeclarationTarget(final PsiElement element, final PsiElement navElement) {
    return null;
  }

  @Nullable
  default Collection<PsiElement> getTargetCandidates(@Nonnull PsiReference reference) {
    return null;
  }

  @Nullable
  default PsiElement getNamedElement(@Nonnull final PsiElement element) {
    return null;
  }

  @Nullable
  default PsiElement modifyTargetElement(@Nonnull PsiElement element, @Nonnull Set<String> flags) {
    return null;
  }
}
