package consulo.language.impl.psi;

import consulo.language.ast.IElementType;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceProvidersRegistry;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class PsiCoreCommentImpl extends LeafPsiElement implements PsiComment {
  public PsiCoreCommentImpl(IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public IElementType getTokenType() {
    return getElementType();
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitComment(this);
  }

  @Override
  public String toString() {
    return "PsiComment(" + getElementType().toString() + ")";
  }

  @Override
  @Nonnull
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }
}
