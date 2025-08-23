package consulo.language.duplicateAnalysis.treeHash;

import consulo.language.duplicateAnalysis.util.PsiFragment;
import jakarta.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public interface FragmentsCollector {
  void add(int hash, int cost, @Nullable PsiFragment frag);
}
