package com.intellij.mock;

import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.InternalStdFileTypes;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.LocalTimeCounter;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.lang.LanguageVersion;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MockPsiFile extends MockPsiElement implements PsiFile {
  public static final LanguageVersion DUMMY_LANG_VERSION = new LanguageVersion("DUMMY_LANG_VERSION", "DUMMY_LANG_VERSION", Language.ANY);

  private final long myModStamp = LocalTimeCounter.currentTime();
  private VirtualFile myVirtualFile = null;
  public boolean valid = true;
  public String text = "";
  private final FileViewProvider myFileViewProvider;
  private final PsiManager myPsiManager;

  public MockPsiFile(@Nonnull PsiManager psiManager) {
    super(psiManager.getProject());
    myPsiManager = psiManager;
    myFileViewProvider = new SingleRootFileViewProvider(getManager(), new LightVirtualFile("noname", getFileType(), ""));
  }

  public MockPsiFile(@Nonnull VirtualFile virtualFile, @Nonnull PsiManager psiManager) {
    super(psiManager.getProject());
    myPsiManager = psiManager;
    myVirtualFile = virtualFile;
    myFileViewProvider = new SingleRootFileViewProvider(getManager(), virtualFile);
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Override
  public boolean processChildren(final PsiElementProcessor<PsiFileSystemItem> processor) {
    return true;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public String getName() {
    return "mock.file";
  }

  @Override
  @javax.annotation.Nullable
  public ItemPresentation getPresentation() {
    return null;
  }

  @RequiredWriteAction
  @Override
  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public void checkSetName(String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public PsiDirectory getContainingDirectory() {
    return null;
  }

  @Nullable
  public PsiDirectory getParentDirectory() {
    throw new UnsupportedOperationException("Method getParentDirectory is not yet implemented in " + getClass().getName());
  }

  @Override
  public long getModificationStamp() {
    return myModStamp;
  }

  @Override
  @Nonnull
  public PsiFile getOriginalFile() {
    return this;
  }

  @Override
  @Nonnull
  public FileType getFileType() {
    return InternalStdFileTypes.JAVA;
  }

  @Override
  @Nonnull
  public Language getLanguage() {
    return InternalStdFileTypes.JAVA.getLanguage();
  }

  @Nonnull
  @Override
  public LanguageVersion getLanguageVersion() {
    return DUMMY_LANG_VERSION;
  }

  @Override
  @Nonnull
  public PsiFile[] getPsiRoots() {
    return new PsiFile[]{this};
  }

  @Override
  @Nonnull
  public FileViewProvider getViewProvider() {
    return myFileViewProvider;
  }

  @Override
  public PsiManager getManager() {
    return myPsiManager;
  }

  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  public PsiDirectory getParent() {
    return null;
  }

  @Override
  public PsiElement getFirstChild() {
    return null;
  }

  @Override
  public PsiElement getLastChild() {
    return null;
  }

  @Override
  public void acceptChildren(@Nonnull PsiElementVisitor visitor) {
  }

  @Override
  public PsiElement getNextSibling() {
    return null;
  }

  @Override
  public PsiElement getPrevSibling() {
    return null;
  }
  @Override
  public PsiFile getContainingFile() {
    return null;
  }

  @Override
  public TextRange getTextRange() {
    return null;
  }

  @Override
  public int getStartOffsetInParent() {
    return 0;
  }

  @Override
  public int getTextLength() {
    return 0;
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return null;
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return null;
  }

  @Override
  public int getTextOffset() {
    return 0;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  @Nonnull
  public char[] textToCharArray() {
    return ArrayUtil.EMPTY_CHAR_ARRAY;
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
  public boolean textContains(char c) {
    return false;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
  }

  @Override
  public PsiElement copy() {
    return null;
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    return null;
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    return null;
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    return null;
  }

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    return null;
  }

  @Override
  public PsiElement addRangeBefore(@Nonnull PsiElement first, @Nonnull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    return null;
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    return null;
  }

  @Override
  public void delete() throws IncorrectOperationException {
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
  }

  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    return null;
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public PsiReference getReference() {
    return null;
  }

  @Override
  @Nonnull
  public PsiReference[] getReferences() {
    return PsiReference.EMPTY_ARRAY;
  }

  @Override
  public <T> T getCopyableUserData(Key<T> key) {
    return null;
  }

  @Override
  public <T> void putCopyableUserData(Key<T> key, T value) {
  }

  @Override
  @Nonnull
  public Project getProject() {
    final PsiManager manager = getManager();
    if (manager == null) throw new PsiInvalidElementAccessException(this);

    return manager.getProject();
  }

  @Override
  public boolean isPhysical() {
    return true;
  }

  @Override
  public PsiElement getNavigationElement() {
    return this;
  }

  @Override
  public PsiElement getOriginalElement() {
    return this;
  }

  @Override
  @Nonnull
  public GlobalSearchScope getResolveScope() {
    return GlobalSearchScope.EMPTY_SCOPE;
  }

  @Override
  @Nonnull
  public SearchScope getUseScope() {
    return GlobalSearchScope.EMPTY_SCOPE;
  }

  @Override
  public FileASTNode getNode() {
    return null;
  }

  @Override
  public void navigate(boolean requestFocus) {
  }

  @Override
  public boolean canNavigate() {
    return false;
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public PsiElement getContext() {
    return FileContextUtil.getFileContext(this);
  }
}
