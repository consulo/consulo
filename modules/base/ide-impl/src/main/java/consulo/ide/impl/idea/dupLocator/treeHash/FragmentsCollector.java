package consulo.ide.impl.idea.dupLocator.treeHash;

import consulo.ide.impl.idea.dupLocator.util.PsiFragment;
import javax.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public interface FragmentsCollector {
  void add(int hash, int cost, @Nullable PsiFragment frag);
}
