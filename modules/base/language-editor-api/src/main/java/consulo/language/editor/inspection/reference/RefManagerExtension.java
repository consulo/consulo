// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.inspection.reference;

import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.util.dataholder.Key;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

public interface RefManagerExtension<T> {
  
  Key<T> getID();

  
  default Collection<Language> getLanguages() {
    return Collections.singleton(getLanguage());
  }

  @Deprecated
  
  Language getLanguage();

  void iterate(RefVisitor visitor);

  void cleanup();

  void removeReference(RefElement refElement);

  @Nullable
  RefElement createRefElement(PsiElement psiElement);

  /**
   * The method finds problem container (ex: method, class, file) that used to be shown as inspection view tree node.
   * This method will be called if  {@link LocalInspectionTool#getProblemElement(PsiElement)} returns null or PsiFile instance for specific inspection tool.
   *
   * @param psiElement
   * @return container element for given psiElement
   */
  @Nullable
  default PsiNamedElement getElementContainer(PsiElement psiElement) {
    return null;
  }

  @Nullable
  RefEntity getReference(String type, String fqName);

  @Nullable
  String getType(RefEntity entity);

  
  RefEntity getRefinedElement(RefEntity ref);

  void visitElement(PsiElement element);

  @Nullable
  String getGroupName(RefEntity entity);

  boolean belongsToScope(PsiElement psiElement);

  void export(RefEntity refEntity, Element element);

  void onEntityInitialized(RefElement refEntity, PsiElement psiElement);

  default boolean shouldProcessExternalFile(PsiFile file) {
    return false;
  }

  
  default Stream<? extends PsiElement> extractExternalFileImplicitReferences(PsiFile psiFile) {
    return Stream.empty();
  }

  default void markExternalReferencesProcessed(RefElement file) {

  }
}
