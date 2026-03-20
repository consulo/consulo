// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi.pointer;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

class FileElementInfo extends SmartPointerElementInfo {
  
  private final VirtualFile myVirtualFile;
  
  private final Project myProject;
  
  private final String myLanguageId;
  
  private final String myFileClassName;

  FileElementInfo(PsiFile file) {
    myVirtualFile = file.getViewProvider().getVirtualFile();
    myProject = file.getProject();
    myLanguageId = LanguageUtil.getRootLanguage(file).getID();
    myFileClassName = file.getClass().getName();
  }

  @Override
  PsiElement restoreElement(SmartPointerManagerImpl manager) {
    Language language = Language.findLanguageByID(myLanguageId);
    if (language == null) return null;
    PsiFile file = SelfElementInfo.restoreFileFromVirtual(myVirtualFile, myProject, language);
    return file != null && file.getClass().getName().equals(myFileClassName) ? file : null;
  }

  @Override
  PsiFile restoreFile(SmartPointerManagerImpl manager) {
    PsiElement element = restoreElement(manager);
    return element == null ? null : element.getContainingFile(); // can be directory
  }

  @Override
  int elementHashCode() {
    return myVirtualFile.hashCode();
  }

  @Override
  boolean pointsToTheSameElementAs(SmartPointerElementInfo other, SmartPointerManagerImpl manager) {
    return other instanceof FileElementInfo && Comparing.equal(myVirtualFile, ((FileElementInfo)other).myVirtualFile);
  }

  
  @Override
  VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Override
  Segment getRange(SmartPointerManagerImpl manager) {
    if (!myVirtualFile.isValid()) return null;

    Document document = FileDocumentManager.getInstance().getDocument(myVirtualFile);
    return document == null ? null : TextRange.from(0, document.getTextLength());
  }

  @Override
  @Nullable Segment getPsiRange(SmartPointerManagerImpl manager) {
    Document currentDoc = FileDocumentManager.getInstance().getCachedDocument(myVirtualFile);
    Document committedDoc = currentDoc == null ? null : ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject)).getLastCommittedDocument(currentDoc);
    return committedDoc == null ? getRange(manager) : new TextRange(0, committedDoc.getTextLength());
  }

  @Override
  public String toString() {
    return "file{" + myVirtualFile + ", " + myLanguageId + "}";
  }
}
