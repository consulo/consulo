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

package consulo.ide.impl.psi.impl.source;

import consulo.language.psi.PsiFileEx;
import consulo.language.impl.psi.ResolveScopeManager;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.psi.PsiNavigationSupport;
import consulo.language.ast.FileASTNode;
import consulo.language.Language;
import consulo.application.ApplicationManager;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.CheckUtil;
import consulo.language.impl.psi.PsiElementBase;
import consulo.language.impl.internal.psi.SharedPsiElementImplUtil;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.impl.internal.psi.PsiFileImplUtil;
import consulo.language.psi.FileContextUtil;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.content.scope.SearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.psi.PsiElementWithSubtreeChangeNotifier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class LightPsiFileImpl extends PsiElementBase implements PsiFileEx, PsiElementWithSubtreeChangeNotifier {

  private static final Logger LOG = Logger.getInstance(LightPsiFileImpl.class);
  private PsiFile myOriginalFile = null;
  private boolean myExplicitlySetAsValid = false;
  private boolean myInvalidated = false;
  private final FileViewProvider myViewProvider;
  private final PsiManagerImpl myManager;
  private final Language myLanguage;

  public LightPsiFileImpl(@Nonnull FileViewProvider provider, @Nonnull Language language) {
    myViewProvider = provider;
    myManager = (PsiManagerImpl)provider.getManager();
    myLanguage = language;
  }

  @Override
  public VirtualFile getVirtualFile() {
    return getViewProvider().isEventSystemEnabled() ? getViewProvider().getVirtualFile() : null;
  }

  @Override
  public boolean processChildren(final PsiElementProcessor<PsiFileSystemItem> processor) {
    return true;
  }

  @Override
  public boolean isValid() {
    if (myInvalidated) return false;
    if (!getViewProvider().isPhysical() || myExplicitlySetAsValid) return true; // "dummy" file
    return getViewProvider().getVirtualFile().isValid();
  }

  public void setIsValidExplicitly(boolean b) {
    LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode());
    myExplicitlySetAsValid = b;
  }

  @Override
  public String getText() {
    return getViewProvider().getContents().toString();
  }

  @Override
  public long getModificationStamp() {
    return getViewProvider().getModificationStamp();
  }

  @Override
  public void subtreeChanged() {
    clearCaches();
    getViewProvider().rootChanged(this);
  }

  public abstract void clearCaches();

  @Override
  @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
  protected LightPsiFileImpl clone() {
    final FileViewProvider provider = getViewProvider().clone();
    final LightPsiFileImpl clone = (LightPsiFileImpl)provider.getPsi(getLanguage());

    copyCopyableDataTo(clone);

    if (getViewProvider().isEventSystemEnabled()) {
      clone.myOriginalFile = this;
    }
    else if (myOriginalFile != null) {
      clone.myOriginalFile = myOriginalFile;
    }
    return clone;
  }

  @Override
  @Nonnull
  public String getName() {
    return getViewProvider().getVirtualFile().getName();
  }

  @Override
  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
    checkSetName(name);
    subtreeChanged();
    return PsiFileImplUtil.setName(this, name);
  }

  @Override
  public void checkSetName(String name) throws IncorrectOperationException {
    if (!getViewProvider().isEventSystemEnabled()) return;
    PsiFileImplUtil.checkSetName(this, name);
  }

  @Override
  public PsiDirectory getParent() {
    return getContainingDirectory();
  }

  @Override
  public PsiDirectory getContainingDirectory() {
    final VirtualFile parentFile = getViewProvider().getVirtualFile().getParent();
    if (parentFile == null) return null;
    return getManager().findDirectory(parentFile);
  }

  @Nullable
  public PsiDirectory getParentDirectory() {
    return getContainingDirectory();
  }

  @Override
  public PsiFile getContainingFile() {
    return this;
  }

  @Override
  public void delete() throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    if (!getViewProvider().isEventSystemEnabled()) {
      throw new IncorrectOperationException();
    }
    CheckUtil.checkWritable(this);
  }

  @Override
  @Nonnull
  public PsiFile getOriginalFile() {
    return myOriginalFile == null ? this : myOriginalFile;
  }

  public void setOriginalFile(final PsiFile originalFile) {
    myOriginalFile = originalFile.getOriginalFile();
  }

  @Override
  @Nonnull
  public PsiFile[] getPsiRoots() {
    return new PsiFile[]{this};
  }

  @Override
  public boolean isPhysical() {
    return getViewProvider().isEventSystemEnabled();
  }

  @Override
  @Nonnull
  public Language getLanguage() {
    return myLanguage;
  }

  @Override
  @Nonnull
  public FileViewProvider getViewProvider() {
    return myViewProvider;
  }

  @Override
  public PsiManager getManager() {
    return myManager;
  }

  @Override
  @Nonnull
  public Project getProject() {
    return getManager().getProject();
  }

  @Override
  public void acceptChildren(@Nonnull PsiElementVisitor visitor) {
    PsiElement child = getFirstChild();
    while (child != null) {
      final PsiElement nextSibling = child.getNextSibling();
      child.accept(visitor);
      child = nextSibling;
    }
  }

  @Override
  public final synchronized PsiElement copy() {
    return clone();
  }

  @Override
  public final void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    CheckUtil.checkWritable(this);
  }

  @Override
  @Nonnull
  public synchronized PsiReference[] getReferences() {
    return SharedPsiElementImplUtil.getReferences(this);
  }

  @Override
  @Nonnull
  public SearchScope getUseScope() {
    return ResolveScopeManager.getElementUseScope(this);
  }

  @Override
  public void navigate(boolean requestFocus) {
    PsiNavigationSupport.getInstance().getDescriptor(this).navigate(requestFocus);
  }

  @Override
  public synchronized PsiElement findElementAt(int offset) {
    return getViewProvider().findElementAt(offset);
  }

  @Override
  public synchronized PsiReference findReferenceAt(int offset) {
    return getViewProvider().findReferenceAt(offset);
  }

  @Override
  @Nonnull
  public char[] textToCharArray() {
    return CharArrayUtil.fromSequence(getViewProvider().getContents());
  }

  @Override
  public boolean isContentsLoaded() {
    return true;
  }

  @Override
  public void onContentReload() {
  }

  @Override
  public boolean isWritable() {
    return getViewProvider().getVirtualFile().isWritable();
  }

  @Override
  @Nonnull
  public abstract PsiElement[] getChildren();

  @Override
  public PsiElement getFirstChild() {
    final PsiElement[] children = getChildren();
    return children.length == 0 ? null : children[0];
  }

  @Override
  public PsiElement getLastChild() {
    final PsiElement[] children = getChildren();
    return children.length == 0 ? null : children[children.length - 1];
  }

  @Override
  public TextRange getTextRange() {
    return new TextRange(0, getTextLength());
  }

  @Override
  public int getStartOffsetInParent() {
    return 0;
  }

  @Override
  public int getTextLength() {
    return getViewProvider().getContents().length();
  }

  @Override
  public int getTextOffset() {
    return 0;
  }

  @Override
  public boolean textMatches(@Nonnull PsiElement element) {
    return textMatches(element.getText());
  }

  @Override
  public boolean textMatches(@Nonnull CharSequence text) {
    return text.equals(getViewProvider().getContents());
  }

  @Override
  public boolean textContains(char c) {
    return getText().indexOf(c) >= 0;
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public final PsiElement addRangeBefore(@Nonnull PsiElement first, @Nonnull PsiElement last, PsiElement anchor)
          throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public final PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
          throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public FileASTNode getNode() {
    return null;
  }

  public abstract LightPsiFileImpl copyLight(final FileViewProvider viewProvider);

  @Override
  public PsiElement getContext() {
    return FileContextUtil.getFileContext(this);
  }

  @Override
  public void markInvalidated() {
    myInvalidated = true;
  }
}
