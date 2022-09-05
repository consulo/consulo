package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.util.IncorrectOperationException;

import javax.annotation.Nonnull;

/**
 * @author traff
 */
public class PsiReferenceWrapper implements PsiReference {
  private final PsiReference myOriginalPsiReference;

  public PsiReferenceWrapper(PsiReference originalPsiReference) {
    myOriginalPsiReference = originalPsiReference;
  }

  @RequiredReadAction
  @Override
  public PsiElement getElement() {
    return myOriginalPsiReference.getElement();
  }

  @RequiredReadAction
  @Override
  public TextRange getRangeInElement() {
    return myOriginalPsiReference.getRangeInElement();
  }

  @RequiredReadAction
  @Override
  public PsiElement resolve() {
    return myOriginalPsiReference.resolve();
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public String getCanonicalText() {
    return myOriginalPsiReference.getCanonicalText();
  }

  @RequiredWriteAction
  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    return myOriginalPsiReference.handleElementRename(newElementName);
  }

  @RequiredWriteAction
  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    return myOriginalPsiReference.bindToElement(element);
  }

  @RequiredReadAction
  @Override
  public boolean isReferenceTo(PsiElement element) {
    return myOriginalPsiReference.isReferenceTo(element);
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public Object[] getVariants() {
    return myOriginalPsiReference.getVariants();
  }

  @RequiredReadAction
  @Override
  public boolean isSoft() {
    return myOriginalPsiReference.isSoft();
  }

  public <T extends PsiReference> boolean isInstance(Class<T> clazz) {
    if (myOriginalPsiReference instanceof PsiReferenceWrapper) {
      return ((PsiReferenceWrapper)myOriginalPsiReference).isInstance(clazz);
    }
    return clazz.isInstance(myOriginalPsiReference);
  }

  public <T extends PsiReference> T cast(Class<T> clazz) {
    if (myOriginalPsiReference instanceof PsiReferenceWrapper) {
      return ((PsiReferenceWrapper)myOriginalPsiReference).cast(clazz);
    }
    return clazz.cast(myOriginalPsiReference);
  }
}
