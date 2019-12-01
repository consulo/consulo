package com.intellij.dupLocator.treeHash;

import com.intellij.dupLocator.util.PsiFragment;
import javax.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public interface FragmentsCollector {
  void add(int hash, int cost, @Nullable PsiFragment frag);
}
