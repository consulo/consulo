package com.intellij.psi.impl.source.tree;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.tree.IElementType;
import javax.annotation.Nonnull;

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
  public void accept(@Nonnull PsiElementVisitor visitor){
    visitor.visitComment(this);
  }

  @Override
  public String toString(){
    return "PsiComment(" + getElementType().toString() + ")";
  }

  @Override
  @Nonnull
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }
}
