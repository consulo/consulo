/*
 * @author max
 */
package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.resolve.PsiFileSystemItemProcessor;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

public abstract class SyntheticFileSystemItem extends PsiElementBase implements PsiFileSystemItem {
  public static final Logger LOG = Logger.getInstance(SyntheticFileSystemItem.class);

  protected final Project myProject;
  protected final PsiManager myManager;

  public SyntheticFileSystemItem(Project project) {
    myProject = project;
    myManager = PsiManager.getInstance(myProject);
  }

  protected static boolean processFileSystemItem(PsiElementProcessor<PsiFileSystemItem> processor, PsiFileSystemItem element) {
    if (processor instanceof PsiFileSystemItemProcessor && !((PsiFileSystemItemProcessor)processor).acceptItem(element.getName(), true)) {
      return true;
    }

    return processor.execute(element);
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Override
  public ASTNode getNode() {
    return null;
  }

  @Override
  public boolean isPhysical() {
    return true;
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public boolean isValid() {
    final VirtualFile virtualFile = getVirtualFile();
    return virtualFile != null && virtualFile.isValid();
  }

  @Override
  public PsiElement replace(@Nonnull final PsiElement newElement) throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be changed");
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be deleted");
  }

  @Override
  public void delete() throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be deleted");
  }

  @Override
  public void accept(@Nonnull final PsiElementVisitor visitor) {
    // TODO
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    final PsiElementProcessor.CollectElements<PsiFileSystemItem> collector = new PsiElementProcessor.CollectElements<PsiFileSystemItem>();
    processChildren(collector);
    return collector.toArray(new PsiFileSystemItem[0]);
  }

  @Override
  public PsiManager getManager() {
    return myManager;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Language getLanguage() {
    return Language.ANY;
  }

  @Override
  public void checkSetName(final String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be renamed");
  }

  @RequiredWriteAction
  @Override
  public PsiElement setName(@NonNls @Nonnull final String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be renamed");
  }

  @Override
  @Nullable
  public PsiFile getContainingFile() {
    return null;
  }

  @RequiredReadAction
  @Override
  @Nullable
  public TextRange getTextRange() {
    return null;
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
  @Nullable
  public String getText() {
    return null;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public char[] textToCharArray() {
    return ArrayUtil.EMPTY_CHAR_ARRAY; // TODO throw new InsupportedOperationException()
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
  public PsiElement copy() {
    LOG.error("method not implemented");
    return null;
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
}
