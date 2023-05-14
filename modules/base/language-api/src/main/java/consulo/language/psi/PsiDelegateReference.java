package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Sergey Evdokimov
 */
public class PsiDelegateReference implements PsiReference {

  private final PsiReference myDelegate;

  public PsiDelegateReference(@Nonnull PsiReference delegate) {
    myDelegate = delegate;
  }

  @RequiredReadAction
  @Override
  public PsiElement getElement() {
    return myDelegate.getElement();
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public TextRange getRangeInElement() {
    return myDelegate.getRangeInElement();
  }

  @RequiredReadAction
  @Nullable
  @Override
  public PsiElement resolve() {
    return myDelegate.resolve();
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public String getCanonicalText() {
    return myDelegate.getCanonicalText();
  }

  @RequiredWriteAction
  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    return myDelegate.handleElementRename(newElementName);
  }

  @RequiredWriteAction
  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    return myDelegate.bindToElement(element);
  }

  @RequiredReadAction
  @Override
  public boolean isReferenceTo(PsiElement element) {
    return myDelegate.isReferenceTo(element);
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public Object[] getVariants() {
    return myDelegate.getVariants();
  }

  @RequiredReadAction
  @Override
  public boolean isSoft() {
    return myDelegate.isSoft();
  }

  public static PsiReference createSoft(PsiReference origin, final boolean soft) {
    return new PsiDelegateReference(origin) {
      @Override
      public boolean isSoft() {
        return soft;
      }
    };
  }
}
