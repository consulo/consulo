// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.psi;

import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.impl.internal.psi.SharedPsiElementImplUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DummyHolderViewProvider extends AbstractFileViewProvider {
  private DummyHolder myHolder;
  private final long myModificationStamp;

  public DummyHolderViewProvider(@Nonnull PsiManager manager) {
    super(manager, new LightVirtualFile("DummyHolder", UnknownFileType.INSTANCE, ""), false);
    myModificationStamp = LocalTimeCounter.currentTime();
  }

  @Override
  @Nonnull
  public CharSequence getContents() {
    return myHolder != null ? myHolder.getNode().getText() : "";
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

  @Nullable
  @Override
  protected PsiFile getPsiInner(Language target) {
    return getCachedPsi(target);
  }

  @Override
  public PsiFile getCachedPsi(@Nonnull Language target) {
    ((PsiManagerEx)getManager()).getFileManager().setViewProvider(getVirtualFile(), this);
    return target == getBaseLanguage() ? myHolder : null;
  }

  @Nonnull
  @Override
  public List<PsiFile> getCachedPsiFiles() {
    return Collections.singletonList(myHolder);
  }

  @Nonnull
  @Override
  public List<FileElement> getKnownTreeRoots() {
    return Collections.singletonList(myHolder.getTreeElement());
  }

  @Override
  @Nonnull
  public List<PsiFile> getAllFiles() {
    return getCachedPsiFiles();
  }

  @Override
  public long getModificationStamp() {
    return myModificationStamp;
  }

  public void setDummyHolder(@Nonnull DummyHolder dummyHolder) {
    myHolder = dummyHolder;
    ((LightVirtualFile)getVirtualFile()).setFileType(dummyHolder.getFileType());
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return SharedPsiElementImplUtil.findReferenceAt(getPsi(getBaseLanguage()), offset);
  }

  @Override
  public PsiElement findElementAt(int offset, @Nonnull Class<? extends Language> lang) {
    if (!lang.isAssignableFrom(getBaseLanguage().getClass())) return null;
    return findElementAt(offset);
  }

  @Nonnull
  @Override
  public FileViewProvider createCopy(@Nonnull VirtualFile copy) {
    throw new RuntimeException("Clone is not supported for DummyHolderProviders. Use DummyHolder clone directly.");
  }

  @Override
  public PsiElement findElementAt(int offset) {
    LeafElement element = myHolder.calcTreeElement().findLeafElementAt(offset);
    return element != null ? element.getPsi() : null;
  }
}
