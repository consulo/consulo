package consulo.language.psi.resolve;

import consulo.language.psi.PsiElement;
import consulo.util.dataholder.Key;

import org.jspecify.annotations.Nullable;

/**
 * @author peter
 */
public abstract class DelegatingScopeProcessor implements PsiScopeProcessor {
  private final PsiScopeProcessor myDelegate;

  public DelegatingScopeProcessor(PsiScopeProcessor delegate) {
    myDelegate = delegate;
  }

  @Override
  public boolean execute(PsiElement element, ResolveState state) {
    return myDelegate.execute(element, state);
  }

  @Override
  public @Nullable <T> T getHint(Key<T> hintKey) {
    return myDelegate.getHint(hintKey);
  }

  @Override
  public void handleEvent(Event event, Object associated) {
    myDelegate.handleEvent(event, associated);
  }
}
