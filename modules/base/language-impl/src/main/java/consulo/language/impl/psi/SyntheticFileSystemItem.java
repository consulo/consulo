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

public abstract class SyntheticFileSystemItem extends PsiElementBase implements PsiFileSystemItem {
    public static final Logger LOG = Logger.getInstance(SyntheticFileSystemItem.class);

    protected final Project myProject;
    protected final PsiManager myManager;

    public SyntheticFileSystemItem(Project project) {
        myProject = project;
        myManager = PsiManager.getInstance(myProject);
    }

    @RequiredReadAction
    @SuppressWarnings("SimplifiableIfStatement")
    protected static boolean processFileSystemItem(PsiElementProcessor<PsiFileSystemItem> processor, PsiFileSystemItem element) {
        if (processor instanceof PsiFileSystemItemProcessor fsItemProcessor && !fsItemProcessor.acceptItem(element.getName(), true)) {
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
    @RequiredReadAction
    public boolean isValid() {
        VirtualFile virtualFile = getVirtualFile();
        return virtualFile != null && virtualFile.isValid();
    }

    @Override
    @RequiredWriteAction
    public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
        throw new IncorrectOperationException("Frameworks cannot be changed");
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        throw new IncorrectOperationException("Frameworks cannot be deleted");
    }

    @Override
    @RequiredWriteAction
    public void delete() throws IncorrectOperationException {
        throw new IncorrectOperationException("Frameworks cannot be deleted");
    }

    @Override
    public void accept(@Nonnull PsiElementVisitor visitor) {
        // TODO
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElement[] getChildren() {
        PsiElementProcessor.CollectElements<PsiFileSystemItem> collector = new PsiElementProcessor.CollectElements<>();
        processChildren(collector);
        return collector.toArray(new PsiFileSystemItem[0]);
    }

    @Nonnull
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
    public void checkSetName(String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Frameworks cannot be renamed");
    }

    @Override
    @RequiredWriteAction
    public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Frameworks cannot be renamed");
    }

    @Override
    @Nullable
    public PsiFile getContainingFile() {
        return null;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public TextRange getTextRange() {
        return TextRange.EMPTY_RANGE;
    }

    @Override
    @RequiredReadAction
    public int getStartOffsetInParent() {
        return -1;
    }

    @Override
    @RequiredReadAction
    public int getTextLength() {
        return -1;
    }

    @Override
    @RequiredReadAction
    public PsiElement findElementAt(int offset) {
        return null;
    }

    @Override
    public int getTextOffset() {
        return -1;
    }

    @Nullable
    @Override
    @RequiredReadAction
    public String getText() {
        return null;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public char[] textToCharArray() {
        return ArrayUtil.EMPTY_CHAR_ARRAY; // TODO throw new UnsupportedOperationException()
    }

    @Override
    @RequiredReadAction
    public boolean textMatches(@Nonnull CharSequence text) {
        return false;
    }

    @Override
    @RequiredReadAction
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
    @RequiredWriteAction
    public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    @RequiredWriteAction
    public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }
}
