package consulo.ide.impl.idea.dupLocator;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
public interface ExternalizableDuplocatorState extends DuplocatorState {
  boolean distinguishRole(@Nonnull PsiElementRole role);

  boolean distinguishLiterals();
}
