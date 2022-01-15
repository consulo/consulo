// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.lang;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefVisitor;
import com.intellij.lang.Language;
import consulo.util.dataholder.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

public interface RefManagerExtension<T> {
  @Nonnull
  Key<T> getID();

  @Nonnull
  default Collection<Language> getLanguages() {
    return Collections.singleton(getLanguage());
  }

  @Deprecated
  @Nonnull
  Language getLanguage();

  void iterate(@Nonnull RefVisitor visitor);

  void cleanup();

  void removeReference(@Nonnull RefElement refElement);

  @Nullable
  RefElement createRefElement(@Nonnull PsiElement psiElement);

  /**
   * The method finds problem container (ex: method, class, file) that used to be shown as inspection view tree node.
   * This method will be called if  {@link LocalInspectionTool#getProblemElement(PsiElement)} returns null or PsiFile instance for specific inspection tool.
   *
   * @param psiElement
   * @return container element for given psiElement
   */
  @Nullable
  default PsiNamedElement getElementContainer(@Nonnull PsiElement psiElement) {
    return null;
  }

  @Nullable
  RefEntity getReference(String type, String fqName);

  @Nullable
  String getType(@Nonnull RefEntity entity);

  @Nonnull
  RefEntity getRefinedElement(@Nonnull RefEntity ref);

  void visitElement(@Nonnull PsiElement element);

  @Nullable
  String getGroupName(@Nonnull RefEntity entity);

  boolean belongsToScope(@Nonnull PsiElement psiElement);

  void export(@Nonnull RefEntity refEntity, @Nonnull Element element);

  void onEntityInitialized(@Nonnull RefElement refEntity, @Nonnull PsiElement psiElement);

  default boolean shouldProcessExternalFile(@Nonnull PsiFile file) {
    return false;
  }

  @Nonnull
  default Stream<? extends PsiElement> extractExternalFileImplicitReferences(@Nonnull PsiFile psiFile) {
    return Stream.empty();
  }

  default void markExternalReferencesProcessed(@Nonnull RefElement file) {

  }
}
