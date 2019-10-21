/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.FreeThreadedFileViewProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.Set;

class MultipleRootsInjectedFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider implements FreeThreadedFileViewProvider, InjectedFileViewProvider {
  private final Object myLock = new Object();
  private final DocumentWindowImpl myDocumentWindow;
  private final Language myLanguage;
  private boolean myPatchingLeaves;
  protected final AbstractFileViewProvider myOriginalProvider;

  MultipleRootsInjectedFileViewProvider(@Nonnull PsiManager psiManager,
                                        @Nonnull VirtualFileWindow virtualFile,
                                        @Nonnull DocumentWindowImpl documentWindow,
                                        @Nonnull Language language,
                                        @Nonnull AbstractFileViewProvider original) {
    super(psiManager, (VirtualFile)virtualFile, true);
    myDocumentWindow = documentWindow;
    myLanguage = language;
    myOriginalProvider = original;
  }

  @Override
  public Object getLock() {
    return myLock;
  }

  @Override
  public boolean getPatchingLeaves() {
    return myPatchingLeaves;
  }

  @Override
  public FileViewProvider clone() {
    return cloneImpl();
  }

  @Override
  public void rootChanged(@Nonnull PsiFile psiFile) {
    super.rootChanged(psiFile);
    rootChangedImpl(psiFile);
  }

  @Override
  public boolean isEventSystemEnabled() {
    return isEventSystemEnabledImpl();
  }

  @Override
  public boolean isPhysical() {
    return isPhysicalImpl();
  }

  @Nonnull
  @Override
  public Language getBaseLanguage() {
    return myLanguage;
  }

  @Nonnull
  @Override
  public Set<Language> getLanguages() {
    FileViewProvider original = myOriginalProvider;
    Set<Language> languages = original.getLanguages();
    Language base = original.getBaseLanguage();
    return ContainerUtil.map2Set(languages, (language) -> language == base ? myLanguage : language);
  }

  @Nonnull
  @Override
  protected MultiplePsiFilesPerDocumentFileViewProvider cloneInner(@Nonnull VirtualFile fileCopy) {
    return (MultiplePsiFilesPerDocumentFileViewProvider)getManager().getFileManager().createFileViewProvider(fileCopy, false);
  }

  @Override
  @Nonnull
  public DocumentWindowImpl getDocument() {
    return myDocumentWindow;
  }

  @NonNls
  @Override
  public String toString() {
    return "Multi root injected file '" + getVirtualFile().getName() + "' " + (isValid() ? "" : " invalid") + (isPhysical() ? "" : " nonphysical");
  }

  @Override
  public final void forceCachedPsi(@Nonnull PsiFile psiFile) {
    myRoots.put(psiFile.getLanguage(), (PsiFileImpl)psiFile);
    getManager().getFileManager().setViewProvider(getVirtualFile(), this);
  }

  public void doNotInterruptMeWhileImPatchingLeaves(@Nonnull Runnable runnable) {
    myPatchingLeaves = true;
    try {
      runnable.run();
    }
    finally {
      myPatchingLeaves = false;
    }
  }

  static final class Template extends MultipleRootsInjectedFileViewProvider implements TemplateLanguageFileViewProvider {
    Template(@Nonnull PsiManagerEx psiManager, @Nonnull VirtualFileWindow virtualFile, @Nonnull DocumentWindowImpl documentWindow, @Nonnull Language language, AbstractFileViewProvider original) {
      super(psiManager, virtualFile, documentWindow, language, original);
      assert myOriginalProvider instanceof TemplateLanguageFileViewProvider;
    }

    @Nonnull
    @Override
    public Language getTemplateDataLanguage() {
      return ((TemplateLanguageFileViewProvider)myOriginalProvider).getTemplateDataLanguage();
    }

    @Override
    public IElementType getContentElementType(@Nonnull Language language) {
      return ((TemplateLanguageFileViewProvider)myOriginalProvider).getContentElementType(language);
    }
  }
}
