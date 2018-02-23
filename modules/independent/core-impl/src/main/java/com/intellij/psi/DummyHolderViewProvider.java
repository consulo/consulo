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
package com.intellij.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.SharedPsiElementImplUtil;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.LocalTimeCounter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DummyHolderViewProvider extends UserDataHolderBase implements FileViewProvider{
  private DummyHolder myHolder;
  private final PsiManager myManager;
  private final long myModificationStamp;
  private final LightVirtualFile myLightVirtualFile = new LightVirtualFile("DummyHolder");

  public DummyHolderViewProvider(@Nonnull PsiManager manager) {
    myManager = manager;
    myModificationStamp = LocalTimeCounter.currentTime();
  }

  @Override
  @Nonnull
  public PsiManager getManager() {
    return myManager;
  }

  @Override
  @Nullable
  public Document getDocument() {
    return FileDocumentManager.getInstance().getDocument(getVirtualFile());
  }

  @Override
  @Nonnull
  public CharSequence getContents() {
    return myHolder != null ? myHolder.getNode().getText() : "";
  }

  @Override
  @Nonnull
  public VirtualFile getVirtualFile() {
    return myLightVirtualFile;
  }

  @Override
  @Nonnull
  public Language getBaseLanguage() {
    return myHolder.getLanguage();
  }

  @Override
  @Nonnull
  public Set<Language> getLanguages() {
    return Collections.singleton(getBaseLanguage());
  }

  @Override
  public PsiFile getPsi(@Nonnull Language target) {
    ((PsiManagerEx)myManager).getFileManager().setViewProvider(getVirtualFile(), this);
    return target == getBaseLanguage() ? myHolder : null;
  }

  @Override
  @Nonnull
  public List<PsiFile> getAllFiles() {
    return Collections.singletonList(getPsi(getBaseLanguage()));
  }

  @Override
  public void beforeContentsSynchronized() {}

  @Override
  public void contentsSynchronized() {}

  @Override
  public boolean isEventSystemEnabled() {
    return false;
  }

  @Override
  public boolean isPhysical() {
    return false;
  }

  @Override
  public long getModificationStamp() {
    return myModificationStamp;
  }

  @Override
  public boolean supportsIncrementalReparse(@Nonnull final Language rootLanguage) {
    return true;
  }

  @Override
  public void rootChanged(@Nonnull PsiFile psiFile) {
  }

  public void setDummyHolder(@Nonnull DummyHolder dummyHolder) {
    myHolder = dummyHolder;
    myLightVirtualFile.setFileType(dummyHolder.getFileType());
  }

  @Override
  public FileViewProvider clone(){
    throw new RuntimeException("Clone is not supported for DummyHolderProviders. Use DummyHolder clone directly.");
  }

  @Override
  public PsiReference findReferenceAt(final int offset) {
    return SharedPsiElementImplUtil.findReferenceAt(getPsi(getBaseLanguage()), offset);
  }

  @Override
  @Nullable
  public PsiElement findElementAt(final int offset, @Nonnull final Language language) {
    return language == getBaseLanguage() ? findElementAt(offset) : null;
  }


  @Override
  public PsiElement findElementAt(int offset, @Nonnull Class<? extends Language> lang) {
    if (!lang.isAssignableFrom(getBaseLanguage().getClass())) return null;
    return findElementAt(offset);
  }

  @Override
  public PsiReference findReferenceAt(final int offsetInElement, @Nonnull final Language language) {
    return language == getBaseLanguage() ? findReferenceAt(offsetInElement) : null;
  }

  @Nonnull
  @Override
  public FileViewProvider createCopy(@Nonnull final VirtualFile copy) {
    throw new RuntimeException("Clone is not supported for DummyHolderProviders. Use DummyHolder clone directly.");
  }

  @Nonnull
  @Override
  public PsiFile getStubBindingRoot() {
    return getPsi(getBaseLanguage());
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return myLightVirtualFile.getFileType();
  }

  @Override
  public PsiElement findElementAt(final int offset) {
    final LeafElement element = ((PsiFileImpl)getPsi(getBaseLanguage())).calcTreeElement().findLeafElementAt(offset);
    return element != null ? element.getPsi() : null;
  }
}
