/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.mock;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author peter
 */
public class MockPsiDocumentManager extends PsiDocumentManager {
  @RequiredReadAction
  @Override
  @javax.annotation.Nullable
  public PsiFile getPsiFile(@Nonnull Document document) {
    throw new UnsupportedOperationException("Method getPsiFile is not yet implemented in " + getClass().getName());
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiFile getCachedPsiFile(@Nonnull Document document) {
    throw new UnsupportedOperationException("Method getCachedPsiFile is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public Document getDocument(@Nonnull PsiFile file) {
    return null;
  }

  @Override
  @Nullable
  public Document getCachedDocument(@Nonnull PsiFile file) {
    VirtualFile vFile = file.getViewProvider().getVirtualFile();
    return FileDocumentManager.getInstance().getCachedDocument(vFile);
  }

  @RequiredUIAccess
  @Override
  public void commitAllDocuments() {
  }

  @Override
  public boolean commitAllDocumentsUnderProgress() {
    return false;
  }

  @Override
  public void performForCommittedDocument(@Nonnull final Document document, @Nonnull final Runnable action) {
    action.run();
  }

  @Override
  public void commitDocument(@Nonnull Document document) {
  }

  @Nonnull
  @Override
  public CharSequence getLastCommittedText(@Nonnull Document document) {
    return document.getImmutableCharSequence();
  }

  @Override
  public long getLastCommittedStamp(@Nonnull Document document) {
    return document.getModificationStamp();
  }

  @Nullable
  @Override
  public Document getLastCommittedDocument(@Nonnull PsiFile file) {
    return null;
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  public Document[] getUncommittedDocuments() {
    throw new UnsupportedOperationException("Method getUncommittedDocuments is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean isUncommited(@Nonnull Document document) {
    throw new UnsupportedOperationException("Method isUncommited is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean isCommitted(@Nonnull Document document) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasUncommitedDocuments() {
    throw new UnsupportedOperationException("Method hasUncommitedDocuments is not yet implemented in " + getClass().getName());
  }

  @Override
  public void commitAndRunReadAction(@Nonnull Runnable runnable) {
    throw new UnsupportedOperationException("Method commitAndRunReadAction is not yet implemented in " + getClass().getName());
  }

  @Override
  public <T> T commitAndRunReadAction(@Nonnull Computable<T> computation) {
    throw new UnsupportedOperationException("Method commitAndRunReadAction is not yet implemented in " + getClass().getName());
  }

  @Override
  public void reparseFiles(@Nonnull Collection<? extends VirtualFile> files, boolean includeOpenFiles) {
    
  }

  @Override
  public void addListener(@Nonnull Listener listener) {
    throw new UnsupportedOperationException("Method addListener is not yet implemented in " + getClass().getName());
  }

  @Override
  public void removeListener(@Nonnull Listener listener) {
    throw new UnsupportedOperationException("Method removeListener is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean isDocumentBlockedByPsi(@Nonnull Document doc) {
    throw new UnsupportedOperationException("Method isDocumentBlockedByPsi is not yet implemented in " + getClass().getName());
  }

  @Override
  public void doPostponedOperationsAndUnblockDocument(@Nonnull Document doc) {
    throw new UnsupportedOperationException(
            "Method doPostponedOperationsAndUnblockDocument is not yet implemented in " + getClass().getName());
  }

  @RequiredUIAccess
  @Override
  public boolean performWhenAllCommitted(@Nonnull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void performLaterWhenAllCommitted(@Nonnull Runnable runnable) {

  }

  @Override
  public void performLaterWhenAllCommitted(@Nonnull Runnable runnable, ModalityState modalityState) {
    throw new UnsupportedOperationException();
  }
}
