package com.intellij.psi.scope;

import consulo.util.dataholder.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public abstract class DelegatingScopeProcessor implements PsiScopeProcessor {
  private final PsiScopeProcessor myDelegate;

  public DelegatingScopeProcessor(PsiScopeProcessor delegate) {
    myDelegate = delegate;
  }

  @Override
  public boolean execute(@Nonnull PsiElement element, ResolveState state) {
    return myDelegate.execute(element, state);
  }

  @Override
  @Nullable
  public <T> T getHint(@Nonnull Key<T> hintKey) {
    return myDelegate.getHint(hintKey);
  }

  @Override
  public void handleEvent(Event event, Object associated) {
    myDelegate.handleEvent(event, associated);
  }
}
