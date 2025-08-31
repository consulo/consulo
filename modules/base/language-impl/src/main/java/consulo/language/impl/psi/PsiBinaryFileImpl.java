// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.util.Queryable;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.FileASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.internal.file.FileManagerImpl;
import consulo.language.psi.FileContextUtil;
import consulo.language.psi.PsiFileEx;
import consulo.language.impl.internal.psi.PsiFileImplUtil;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.util.IncorrectOperationException;
import consulo.module.content.util.ModuleContentUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.lazy.ClearableLazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.module.Module;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

public class PsiBinaryFileImpl extends PsiElementBase implements PsiBinaryFile, PsiFileEx, Cloneable, Queryable, PsiElementWithSubtreeChangeNotifier {
  private final PsiManagerImpl myManager;
  private String myName; // for myFile == null only
  private byte[] myContents; // for myFile == null only
  private final AbstractFileViewProvider myViewProvider;
  private volatile boolean myPossiblyInvalidated;
  private final ClearableLazyValue<Module> myModuleRef;

  public PsiBinaryFileImpl(@Nonnull PsiManager manager, @Nonnull FileViewProvider viewProvider) {
    myViewProvider = (AbstractFileViewProvider)viewProvider;
    myManager = (PsiManagerImpl)manager;
    myModuleRef = ClearableLazyValue.nullable(() -> ModuleContentUtil.findModuleForFile(viewProvider.getVirtualFile(), getProject()));
  }

  @RequiredReadAction
  @Override
  @Nullable
  public Module getModule() {
    return myModuleRef.get();
  }

  @Override
  public void clearCaches() {
    myModuleRef.clear();
  }

  @Override
  @Nonnull
  public VirtualFile getVirtualFile() {
    return myViewProvider.getVirtualFile();
  }

  @Override
  public boolean processChildren(@Nonnull PsiElementProcessor<PsiFileSystemItem> processor) {
    return true;
  }

  byte[] getStoredContents() {
    return myContents;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public String getName() {
    return !isCopy() ? getVirtualFile().getName() : myName;
  }

  @RequiredWriteAction
  @Override
  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
    checkSetName(name);

    if (isCopy()) {
      myName = name;
      return this; // not absolutely correct - might change type
    }

    return PsiFileImplUtil.setName(this, name);
  }

  @Override
  public void checkSetName(String name) throws IncorrectOperationException {
    if (isCopy()) return;
    PsiFileImplUtil.checkSetName(this, name);
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public PsiDirectory getContainingDirectory() {
    VirtualFile parentFile = getVirtualFile().getParent();
    if (parentFile == null) return null;
    return getManager().findDirectory(parentFile);
  }

  @Nullable
  public PsiDirectory getParentDirectory() {
    return getContainingDirectory();
  }

  @Override
  public long getModificationStamp() {
    return getVirtualFile().getModificationStamp();
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Language getLanguage() {
    return Language.ANY;
  }

  @Nonnull
  @Override
  public PsiManager getManager() {
    return myManager;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  public PsiDirectory getParent() {
    return getContainingDirectory();
  }

  @Override
  public PsiFile getContainingFile() {
    return this;
  }

  @Nonnull
  @RequiredReadAction
  @Override
  public TextRange getTextRange() {
    return TextRange.EMPTY_RANGE;
  }

  @RequiredReadAction
  @Override
  public int getStartOffsetInParent() {
    return -1;
  }

  @RequiredReadAction
  @Override
  public int getTextLength() {
    return -1;
  }

  @RequiredReadAction
  @Override
  public PsiElement findElementAt(int offset) {
    return null;
  }

  @Override
  public int getTextOffset() {
    return -1;
  }

  @RequiredReadAction
  @Override
  public String getText() {
    return ""; // TODO[max] throw new UnsupportedOperationException()
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public char[] textToCharArray() {
    return ArrayUtil.EMPTY_CHAR_ARRAY; // TODO[max] throw new UnsupportedOperationException()
  }

  @Override
  public boolean textMatches(@Nonnull CharSequence text) {
    return false;
  }

  @Override
  public boolean textMatches(@Nonnull PsiElement element) {
    return false;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitBinaryFile(this);
  }

  @Override
  public PsiElement copy() {
    PsiBinaryFileImpl clone = (PsiBinaryFileImpl)clone();
    clone.myName = getName();
    try {
      clone.myContents = !isCopy() ? getVirtualFile().contentsToByteArray() : myContents;
    }
    catch (IOException ignored) {
    }
    return clone;
  }

  private boolean isCopy() {
    return myName != null;
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void delete() throws IncorrectOperationException {
    checkDelete();
    PsiFileImplUtil.doDelete(this);
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    if (isCopy()) {
      throw new IncorrectOperationException();
    }
    CheckUtil.checkWritable(this);
  }

  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    return null;
  }

  @Override
  public boolean isValid() {
    if (isCopy()) return true; // "dummy" file
    if (!getVirtualFile().isValid() || myManager.getProject().isDisposed()) return false;

    if (!myPossiblyInvalidated) return true;

    // synchronized by read-write action
    if (((FileManagerImpl)myManager.getFileManager()).evaluateValidity(this)) {
      myPossiblyInvalidated = false;
      PsiInvalidElementAccessException.setInvalidationTrace(this, null);
      return true;
    }
    return false;
  }

  @Override
  public boolean isWritable() {
    return isCopy() || getVirtualFile().isWritable();
  }

  @Override
  public boolean isPhysical() {
    return !isCopy();
  }

  @Override
  @Nonnull
  public PsiFile getOriginalFile() {
    return this;
  }

  @Override
  @NonNls
  public String toString() {
    return "PsiBinaryFile:" + getName();
  }

  @Override
  @Nonnull
  public FileType getFileType() {
    return myViewProvider.getFileType();
  }

  @Override
  @Nonnull
  public PsiFile[] getPsiRoots() {
    return new PsiFile[]{this};
  }

  @Override
  @Nonnull
  public FileViewProvider getViewProvider() {
    return myViewProvider;
  }

  @Override
  public FileASTNode getNode() {
    return null; // TODO[max] throw new UnsupportedOperationException()
  }

  @Override
  public void subtreeChanged() {
  }

  @Override
  public PsiElement getContext() {
    return FileContextUtil.getFileContext(this);
  }

  @Override
  public void putInfo(@Nonnull Map<String, String> info) {
    info.put("fileName", getName());
    info.put("fileType", getFileType().getName());
  }

  @Override
  public boolean isContentsLoaded() {
    return false;
  }

  @Override
  public void onContentReload() {
  }

  @Override
  public void markInvalidated() {
    myPossiblyInvalidated = true;
    DebugUtil.onInvalidated(this);
  }
}
