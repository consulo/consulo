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
package com.intellij.mock;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.util.containers.FactoryMap;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class MockFileManager implements FileManager {
  private final PsiManagerEx myManager;
  private final Map<VirtualFile,FileViewProvider> myViewProviders;

  public MockFileManager(final PsiManagerEx manager) {
    myManager = manager;
    myViewProviders = FactoryMap.create(key -> new SingleRootFileViewProvider(manager, key));
  }

  @Override
  @Nonnull
  public FileViewProvider createFileViewProvider(@Nonnull final VirtualFile file, final boolean physical) {
    return new SingleRootFileViewProvider(myManager, file, physical);
  }

  @Override
  public void dispose() {
    throw new UnsupportedOperationException("Method dispose is not yet implemented in " + getClass().getName());
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiFile findFile(@Nonnull VirtualFile vFile) {
    return getCachedPsiFile(vFile);
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiDirectory findDirectory(@Nonnull VirtualFile vFile) {
    throw new UnsupportedOperationException("Method findDirectory is not yet implemented in " + getClass().getName());
  }

  @RequiredWriteAction
  @Override
  public void reloadFromDisk(@Nonnull PsiFile file) //Q: move to PsiFile(Impl)?
  {
    throw new UnsupportedOperationException("Method reloadFromDisk is not yet implemented in " + getClass().getName());
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiFile getCachedPsiFile(@Nonnull VirtualFile vFile) {
    final FileViewProvider provider = findCachedViewProvider(vFile);
    return provider.getPsi(provider.getBaseLanguage());
  }

  @Override
  public void cleanupForNextTest() {
    myViewProviders.clear();
  }

  @RequiredReadAction
  @Override
  public FileViewProvider findViewProvider(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException("Method findViewProvider is not yet implemented in " + getClass().getName());
  }

  @RequiredReadAction
  @Override
  public FileViewProvider findCachedViewProvider(@Nonnull VirtualFile file) {
    return myViewProviders.get(file);
  }

  @RequiredReadAction
  @Override
  public void setViewProvider(@Nonnull VirtualFile virtualFile, FileViewProvider fileViewProvider) {
    myViewProviders.put(virtualFile, fileViewProvider);
  }

  @Override
  @Nonnull
  public List<PsiFile> getAllCachedFiles() {
    throw new UnsupportedOperationException("Method getAllCachedFiles is not yet implemented in " + getClass().getName());
  }
}
