// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Topic;
import consulo.document.Document;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Topic(ComponentScope.PROJECT)
public interface PsiDocumentListener {

  /**
   * Called when a document instance is created for a file.
   *
   * @param document the created document instance.
   * @param psiFile  the file for which the document was created.
   * @see PsiDocumentManager#getDocument(PsiFile)
   */
  void documentCreated(@Nonnull Document document, @Nullable PsiFile psiFile, @Nonnull Project project);

  /**
   * Called when a file instance is created for a document.
   *
   * @param file     the created file instance.
   * @param document the document for which the file was created.
   * @see PsiDocumentManager#getDocument(PsiFile)
   */
  default void fileCreated(@Nonnull PsiFile file, @Nonnull Document document) {
  }
}
